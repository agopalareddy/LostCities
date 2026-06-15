import java.io.OutputStream;
import java.io.PrintStream;

public class AiSearchRegressionTest {
    public static void main(String[] args) {
        runToCompletion("greedy", "minimax");
        runToCompletion("greedy", "alphabeta");
        runToCompletion("minimax", "alphabeta");
        System.out.println("AiSearchRegressionTest passed");
    }

    private static void runToCompletion(String p1, String p2) {
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        GameManager gm = new GameManager(p1, p2, "fixed");
        gm.dealCards();

        int turns = 0;
        while (!gm.undealt.isEmpty() && turns < 120) {
            gm.playPlayer(gm.p1);
            turns++;
            if (!gm.undealt.isEmpty()) {
                gm.playPlayer(gm.p2);
                turns++;
            }
        }
        System.setOut(originalOut);

        if (!gm.undealt.isEmpty()) {
            throw new AssertionError(p1 + " vs " + p2 + " did not finish in " + turns + " turns");
        }
    }
}
