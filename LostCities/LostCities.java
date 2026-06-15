import java.util.*;

public class LostCities {
    public static void main(String[] args) {
        // Force standard output to auto-flush so that piped Node.js streams receive chunks in real-time
        System.setOut(new java.io.PrintStream(System.out, true));

        String p1Type = "human";
        String p2Type = "ai";
        String seedOption = "random";
        if (args.length >= 2) {
            p1Type = args[0].toLowerCase();
            p2Type = args[1].toLowerCase();
        }
        if (args.length >= 3) {
            seedOption = args[2].toLowerCase();
        }

        while (true) {
            GameManager gm = new GameManager(p1Type, p2Type, seedOption);
            gm.dealCards();
            gm.playGame();

            // ask player if they want to play again
            try (Scanner in = new Scanner(System.in)) {
                System.out.println("Play again? (y/n)");
                String answer = in.nextLine();
                if (answer.equalsIgnoreCase("n")) {
                    break;
                }
            }
        }
    }
}