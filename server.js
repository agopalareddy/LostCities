const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const { spawn, exec } = require('child_process');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  path: '/lost-cities/socket.io',
});

const PORT = process.env.PORT || 8082;

console.log('Compiling Lost Cities Java files...');
exec('javac *.java', { cwd: path.join(__dirname, 'LostCities') }, (error, stdout, stderr) => {
  if (error) {
    console.error(`Compilation error: ${error.message}`);
    return;
  }
  if (stderr) {
    console.error(`Compilation stderr: ${stderr}`);
  }
  console.log('Lost Cities Java compiled successfully.');
});

app.use(express.static(__dirname));

app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

// Helper to parse card collections in lines like: Yellow:\t[Y0.0, Y2.0]
function parseCardsLine(line) {
  const match = line.match(/\[(.*?)\]/);
  if (!match) return [];
  const content = match[1].trim();
  if (!content) return [];
  return content
    .split(',')
    .map((s) => {
      s = s.trim();
      const m = s.match(/^([YBWGR])([\d\.]+)$/);
      if (m) {
        return { color: m[1], val: parseFloat(m[2]) };
      }
      return null;
    })
    .filter(Boolean);
}

// Parse full game state from stdout buffer
function parseGameState(stdout) {
  const lines = stdout.split('\n');

  let currentTurn = 1;
  let playerHand = [];
  let player1Hand = [];
  let player2Hand = [];
  let p1Placed = { Yellow: [], Blue: [], White: [], Green: [], Red: [] };
  let p2Placed = { Yellow: [], Blue: [], White: [], Green: [], Red: [] };
  let discards = { Yellow: [], Blue: [], White: [], Green: [], Red: [] };
  let drawPileSize = null; // null until parsed — avoids clobbering real count with a stale default

  let section = null; // 'p1_placed', 'p2_placed', 'discards', 'hand'
  let winnerText = null;
  let isGameOver = false;
  let p1Score = 0;
  let p2Score = 0;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim();

    if (line.includes("It's player 1's turn:")) {
      currentTurn = 1;
    } else if (line.includes("It's player 2's turn:")) {
      currentTurn = 2;
    }

    if (
      line.includes("Player 1's Placed Down cards:") ||
      line.includes("Player 1's Placed Down cards")
    ) {
      section = 'p1_placed';
      continue;
    } else if (
      line.includes("Player 2's Placed Down cards:") ||
      line.includes("Player 2's Placed Down cards")
    ) {
      section = 'p2_placed';
      continue;
    } else if (line.includes('Discard Piles:')) {
      section = 'discards';
      continue;
    } else if (line.includes("Player 1's Hand:") || line.includes("Player 1's Hand")) {
      const nextLine = lines[i + 1] ? lines[i + 1].trim() : '';
      if (nextLine.startsWith('[')) {
        player1Hand = parseCardsLine(nextLine);
      }
      section = null;
      continue;
    } else if (line.includes("Player 2's Hand:") || line.includes("Player 2's Hand")) {
      const nextLine = lines[i + 1] ? lines[i + 1].trim() : '';
      if (nextLine.startsWith('[')) {
        player2Hand = parseCardsLine(nextLine);
      }
      section = null;
      continue;
    } else if (line.startsWith('Hand:')) {
      const cards = parseCardsLine(line);
      if (currentTurn === 1) {
        player1Hand = cards;
      } else {
        player2Hand = cards;
      }
      section = null;
      continue;
    }

    // Capture draw pile size
    if (line.includes('left in the draw pile')) {
      const match = line.match(/(\d+)\s+card/);
      if (match) {
        drawPileSize = parseInt(match[1]);
      }
    }

    // Capture Game Over state & scores
    if (line.includes('Player 1 scored')) {
      const match = line.match(/scored\s+(-?\d+)/);
      if (match) p1Score = parseInt(match[1]);
      isGameOver = true;
    }
    if (line.includes('Player 2 scored')) {
      const match = line.match(/scored\s+(-?\d+)/);
      if (match) p2Score = parseInt(match[1]);
    }
    if (line.includes('won!')) {
      winnerText = line;
    }

    // Parse colored lists
    if (section === 'p1_placed' || section === 'p2_placed' || section === 'discards') {
      const colorMatch = line.match(/^(Yellow|Blue|White|Green|Red):\s*(.*)/i);
      if (colorMatch) {
        const color = colorMatch[1];
        const cards = parseCardsLine(line);
        if (section === 'p1_placed') p1Placed[color] = cards;
        if (section === 'p2_placed') p2Placed[color] = cards;
        if (section === 'discards') discards[color] = cards;
      }
    }
  }

  // Detect which question/prompt is active
  let prompt = null;
  let options = [];

  if (stdout.includes('Discard or Place')) {
    prompt = 'DISCARD_OR_PLACE';
    options = ['d', 'p'];
  } else if (stdout.includes('Pick a card to play')) {
    prompt = 'PICK_CARD';
    // We can pick cards based on active hand indexes (0-7)
    options = playerHand.map((_, idx) => idx.toString());
  } else if (stdout.includes('Pick from Draw Pile or Discards')) {
    prompt = 'PICK_DRAW_OR_DISCARD';
    options = ['u', 'd'];
  } else if (stdout.includes('Pick a color')) {
    prompt = 'PICK_DISCARD_COLOR';
    options = ['y', 'b', 'w', 'g', 'r'];
  } else if (stdout.includes('Play again?')) {
    prompt = 'PLAY_AGAIN';
    options = ['y', 'n'];
  }

  return {
    currentTurn,
    player1Hand,
    player2Hand,
    playerHand: currentTurn === 1 ? player1Hand : player2Hand,
    p1Placed,
    p2Placed,
    discards,
    drawPileSize,
    prompt,
    options,
    isGameOver,
    winnerText,
    p1Score,
    p2Score,
  };
}

io.on('connection', (socket) => {
  console.log(`Lost Cities Client connected: ${socket.id}`);
  let javaProcess = null;
  let outputBuffer = '';
  let currentState = {
    currentTurn: 1,
    playerHand: [],
    player1Hand: [],
    player2Hand: [],
    p1Placed: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
    p2Placed: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
    discards: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
    drawPileSize: 44,
    prompt: null,
    options: [],
    isGameOver: false,
    winnerText: null,
    p1Score: 0,
    p2Score: 0,
    gameMode: 'ai',
  };

  const startGame = (mode) => {
    if (javaProcess) {
      javaProcess.kill();
    }
    currentState = {
      currentTurn: 1,
      playerHand: [],
      player1Hand: [],
      player2Hand: [],
      p1Placed: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
      p2Placed: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
      discards: { Yellow: [], Blue: [], White: [], Green: [], Red: [] },
      drawPileSize: 44,
      prompt: null,
      options: [],
      isGameOver: false,
      winnerText: null,
      p1Score: 0,
      p2Score: 0,
      gameMode: mode || 'ai',
    };

    const p2Type = mode === 'human' ? 'human' : 'ai';
    console.log(`Spawning Java process: LostCities human ${p2Type}`);
    javaProcess = spawn('java', ['LostCities', 'human', p2Type], {
      cwd: path.join(__dirname, 'LostCities'),
    });

    javaProcess.stdout.on('data', (data) => {
      const chunk = data.toString();
      outputBuffer += chunk;
      console.log(`[Java LostCities Stdout]: ${chunk}`);

      const state = parseGameState(outputBuffer);

      // Update persistent game collections if we parsed newer non-empty data
      if (state.player1Hand && state.player1Hand.length > 0) {
        currentState.player1Hand = state.player1Hand;
      }
      if (state.player2Hand && state.player2Hand.length > 0) {
        currentState.player2Hand = state.player2Hand;
      }
      currentState.playerHand =
        currentState.currentTurn === 1 ? currentState.player1Hand : currentState.player2Hand;

      if (state.p1Placed && Object.values(state.p1Placed).some((arr) => arr.length > 0)) {
        currentState.p1Placed = state.p1Placed;
      }
      if (state.p2Placed && Object.values(state.p2Placed).some((arr) => arr.length > 0)) {
        currentState.p2Placed = state.p2Placed;
      }
      if (state.discards && Object.values(state.discards).some((arr) => arr.length > 0)) {
        currentState.discards = state.discards;
      }

      if (state.drawPileSize !== null) currentState.drawPileSize = state.drawPileSize;
      currentState.currentTurn = state.currentTurn;
      currentState.prompt = state.prompt || currentState.prompt;
      currentState.options = state.options;
      if (state.isGameOver) {
        currentState.isGameOver = true;
        currentState.winnerText = state.winnerText || currentState.winnerText;
        currentState.p1Score = state.p1Score;
        currentState.p2Score = state.p2Score;
      }

      socket.emit('game-update', {
        ...currentState,
        raw: outputBuffer,
      });

      // Reset buffer on active question prompts
      if (state.prompt || state.isGameOver) {
        outputBuffer = '';
      }
    });

    javaProcess.stderr.on('data', (data) => {
      console.error(`[Java LostCities Stderr]: ${data}`);
    });

    javaProcess.on('close', (code) => {
      console.log(`Java LostCities closed with code ${code}`);
      socket.emit('game-terminated', { code });
    });
  };

  socket.on('start-game', (mode) => {
    startGame(mode);
  });

  socket.on('input', (message) => {
    if (javaProcess && javaProcess.stdin && javaProcess.stdin.writable) {
      const cleanMessage = String(message).trim();
      console.log(`[LostCities Input]: ${cleanMessage}`);
      currentState.prompt = null; // Clear prompt immediately on user input consumption!
      javaProcess.stdin.write(cleanMessage + '\n');
    }
  });

  socket.on('disconnect', () => {
    console.log(` Lost Cities client disconnected: ${socket.id}`);
    if (javaProcess) {
      javaProcess.kill();
    }
  });
});

server.listen(PORT, () => {
  console.log(`Lost Cities Web Wrapper running on http://localhost:${PORT}`);
});
