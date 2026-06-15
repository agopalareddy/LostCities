const express = require('express');
const http = require('http');
const socketIo = require('socket.io');
const { spawn } = require('child_process');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  path: '/lost-cities/socket.io',
});

const PORT = process.env.PORT || 8082;

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

  // ── AI-vs-AI step buffering ──────────────────────────────────────────────────
  // In AI-vs-AI mode, Java emits [TURN_DONE] after each turn. We buffer those
  // chunks and release them one-at-a-time when the client requests "next-step".
  let aiVsAiMode = false;
  let stepQueue = []; // Array of raw stdout snapshots (one per [TURN_DONE])
  let pendingChunk = ''; // Accumulates raw stdout between [TURN_DONE] markers
  let stepPaused = true; // Start paused / buffering by default in AI-vs-AI mode
  let hasReleasedFirstStep = false; // Tracks if the initial deal/first turn has been sent
  // ─────────────────────────────────────────────────────────────────────────────

  /** Merge new parse results into currentState, only updating non-empty/changed fields. */
  function mergeState(state) {
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
  }

  /** Emit a game-update for the given raw stdout chunk. */
  function emitStep(rawChunk) {
    outputBuffer += rawChunk;
    const state = parseGameState(outputBuffer);
    mergeState(state);

    socket.emit('game-update', {
      ...currentState,
      raw: outputBuffer,
    });

    if (state.prompt || state.isGameOver) {
      outputBuffer = '';
    }
  }

  /** Release one queued step (if any) immediately. We always keep stepPaused = true in AI-vs-AI mode to prevent auto-draining. */
  function releaseNextStep() {
    if (stepQueue.length > 0) {
      const chunk = stepQueue.shift();
      emitStep(chunk);
    }
    stepPaused = true;
    socket.emit('step-queue-size', stepQueue.length);
  }

  const startGame = (options) => {
    const { mode, shuffle, p1Type, p2Type } = options;

    if (javaProcess) {
      javaProcess.kill();
    }

    // Reset step buffers
    stepQueue = [];
    pendingChunk = '';
    stepPaused = true;
    hasReleasedFirstStep = false;

    const seedOption = shuffle === 'fixed' ? 'fixed' : 'random';
    aiVsAiMode = mode === 'ai-vs-ai';

    // Determine Java player type args
    let javaP1 = 'human';
    let javaP2 = 'minimax';

    if (mode === 'ai') {
      javaP1 = 'human';
      javaP2 = p2Type || 'minimax';
    } else if (mode === 'human') {
      javaP1 = 'human';
      javaP2 = 'human';
    } else if (mode === 'ai-vs-ai') {
      javaP1 = p1Type || 'minimax';
      javaP2 = p2Type || 'alphabeta';
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
      shuffleMode: seedOption,
      p1Type: javaP1,
      p2Type: javaP2,
    };
    outputBuffer = '';

    console.log(`Spawning Java process: LostCities ${javaP1} ${javaP2} ${seedOption}`);
    javaProcess = spawn('java', ['LostCities', javaP1, javaP2, seedOption], {
      cwd: path.join(__dirname, 'LostCities'),
    });

    javaProcess.stdout.on('data', (data) => {
      const chunk = data.toString();
      console.log(`[Java LostCities Stdout]: ${chunk}`);

      if (!aiVsAiMode) {
        // Human-involved mode: stream directly as before
        outputBuffer += chunk;
        const state = parseGameState(outputBuffer);
        mergeState(state);

        socket.emit('game-update', {
          ...currentState,
          raw: outputBuffer,
        });

        if (state.prompt || state.isGameOver) {
          outputBuffer = '';
        }
      } else {
        // AI-vs-AI mode: buffer turns between [TURN_DONE] markers
        pendingChunk += chunk;

        // Split on [TURN_DONE] markers
        let parts = pendingChunk.split('[TURN_DONE]');
        // The last part may be incomplete; keep it in pendingChunk
        pendingChunk = parts.pop();

        for (const part of parts) {
          if (part.trim()) {
            stepQueue.push(part);
          }
        }

        // If the remaining buffer ends a game (has 'won!'), push it too
        if (pendingChunk.includes('won!') || pendingChunk.includes('Play again?')) {
          stepQueue.push(pendingChunk);
          pendingChunk = '';
        }

        // If we haven't released the first step yet, do it automatically so the board renders
        if (!hasReleasedFirstStep && stepQueue.length > 0) {
          emitStep(stepQueue.shift());
          hasReleasedFirstStep = true;
        }

        socket.emit('step-queue-size', stepQueue.length);

        // If not paused, release immediately (first turn or auto-play)
        if (!stepPaused) {
          // Drain queued steps immediately (auto mode releases as they arrive)
          while (stepQueue.length > 0) {
            emitStep(stepQueue.shift());
          }
        } else {
          // Paused: waiting for client to request next-step
          socket.emit('step-queue-size', stepQueue.length);
        }
      }
    });

    javaProcess.stderr.on('data', (data) => {
      console.error(`[Java LostCities Stderr]: ${data}`);
    });

    javaProcess.on('close', (code) => {
      console.log(`Java LostCities closed with code ${code}`);
      // Flush any remaining pending chunk in AI-vs-AI mode
      if (aiVsAiMode && pendingChunk.trim()) {
        stepQueue.push(pendingChunk);
        pendingChunk = '';
        socket.emit('step-queue-size', stepQueue.length);
      }
      socket.emit('game-terminated', { code });
    });
  };

  // Legacy start-game support (old signature: mode, shuffle)
  socket.on('start-game', (modeOrOptions, shuffle) => {
    if (typeof modeOrOptions === 'object') {
      startGame(modeOrOptions);
    } else {
      // Legacy: mode string + shuffle string
      startGame({ mode: modeOrOptions, shuffle });
    }
  });

  socket.on('input', (message) => {
    if (javaProcess && javaProcess.stdin && javaProcess.stdin.writable) {
      const cleanMessage = String(message).trim();
      console.log(`[LostCities Input]: ${cleanMessage}`);
      currentState.prompt = null; // Clear prompt immediately on user input consumption!
      javaProcess.stdin.write(cleanMessage + '\n');
    }
  });

  // ── AI-vs-AI step control ───────────────────────────────────────────────────
  socket.on('next-step', () => {
    stepPaused = true; // Ensure we stay in manual mode
    releaseNextStep();
  });

  socket.on('set-auto-play', (enabled) => {
    // No-op: client manages pacing and pulls steps via 'next-step'
  });
  // ─────────────────────────────────────────────────────────────────────────────

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
