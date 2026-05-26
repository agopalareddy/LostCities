import java.util.*;

public class LostCities {
    public static void main(String[] args) {
        // Force standard output to auto-flush so that piped Node.js streams receive chunks in real-time
        System.setOut(new java.io.PrintStream(System.out, true));

        while (true) {
            // GameManager gm = new GameManager(askAiHuman(1), askAiHuman(2));
            GameManager gm = new GameManager("human", "ai");
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

    protected static String askAiHuman(int player_number) {
        try (Scanner in = new Scanner(System.in)) {
            System.out.print("What is player " + player_number + " [AI/Human]? ");
            return in.nextLine().toLowerCase();
        }
    }
}