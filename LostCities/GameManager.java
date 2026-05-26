import java.io.*;
import java.awt.*;
import java.util.*;

/*
Holds:
    array of colors possible
    array of numbers possible (0 for handshake card)

    Player 1
    Player 2
    Undealt Cards Pile
    Discard Piles
*/

public class GameManager {
    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    Player p1;
    Player p2;
    CardsCollection undealt;
    DiscardPiles discards;

    File file;
    Scanner in;
    Scanner in2;

    /* CONSTRUCTORS */

    /* Initialize the internal variables */
    public GameManager(String s1, String s2) {
        if (s1 == "human") {
            // p1 = new Human("testCasesp1.txt");
            p1 = new Human("emptyFileForHuman.txt");
        } else {
            p1 = new Ai();
        }

        if (s2 == "human") {
            p2 = new Human("testCasesp2.txt");
            // p2 = new Human("emptyFileForHuman.txt");
        } else {
            p2 = new Ai();
        }
        undealt = new CardsCollection('U');
        discards = new DiscardPiles();
    }

    /* AUXILIARY FUNCTIONS */

    /** Deal cards to both players from undealt cards pile */
    public void dealCards() {
        for (int i = 0; i < 8 * 2; i++) {
            Card c = undealt.getTopCard();
            undealt.removeCard(c);
            if (i % 2 == 0)
                p1.addCard(c);
            else
                p2.addCard(c);
        }
        System.out.println("Player 1's Hand: ");
        p1.display();
        System.out.println("Player 2's Hand: ");
        p2.display();
    }

    /**
     * Play the game!!
     * Until undealt pile is empty, play players
     * Once undealt pile is empty, calculate score
     */
    public void playGame() {
        while (!undealt.isEmpty()) {
            // Player 1's Turn
            playPlayer(p1);
            if (!undealt.isEmpty())
                playPlayer(p2);
        }

        // Calculate scores
        System.out.println("\n***********************************");
        System.out.println("\nPlayer 1 Score Calculation: ");
        System.out.println("Player 1's Placed Down cards:");
        p1.displayPlacedDownCards();
        double p1_score = p1.getScore();
        System.out.println("\nPlayer 2 Score Calculation: ");
        System.out.println("Player 2's Placed Down cards:");
        p2.displayPlacedDownCards();
        double p2_score = p2.getScore();

        // Output Scores
        System.out.println("\n***********************************");
        System.out.println("\n\nPlayer 1 scored " + (int) p1_score);
        System.out.println("Player 2 scored " + (int) p2_score);
        System.out.println(((p1_score > p2_score) ? "Player 1 won!" : "Player 2 won!"));
    }

    /* PROTECTED FUNCTIONS */

    /**
     * A Player's turn
     * - Display pre-turn statistics
     * - Play outgoing card part of turn
     * - Play incoming card part of turn
     * - Display post-turn statistics
     */
    protected void playPlayer(Player p) {
        /**
         * Display pre-turn statistics
         * - indicate Player's turn
         * - their hand
         */
        System.out.println("\n**********************************\n");
        System.out.println("It's player " + ((p == p1) ? 1 : 2) + "'s turn:");
        System.out.println("*_*_*_*_*_*_*_*_*_*_");
        System.out.println("Pre-turn Statistics: ");
        displayStatistics(p);

        /*
         * If p is initialized as AI, Ai.play() is called
         * If p is initialized as Human, Human.play() is called
         * Player.play() is only an abstract definition
         */
        p.play(((p == p1) ? p2 : p1).getPlacedCards(), discards, undealt); // execute player's turn

        /*
         * Display post-turn statistics
         * - number of undealt cards left
         * - discard piles
         */

        System.out.println("\n_*_*_*_*_*_*_*_*_*_*");
        System.out.println("Post-turn Statistics: ");
        displayStatistics(p);
    }

    // Output to console the current statistics of the games
    protected void displayStatistics(Player p) {
        System.out.println("Player " + ((p == p1) ? 2 : 1) + "'s Placed Down cards:");
        ((p == p1) ? p2 : p1).displayPlacedDownCards();
        discards.displayPiles();
        System.out.println("Player " + ((p == p1) ? 1 : 2) + "'s Placed Down cards:");
        ((p == p1) ? p1 : p2).displayPlacedDownCards();
        System.out.print("There " + (undealt.size() == 1 ? "is " : "are ")
                + undealt.size() + " card" + (undealt.size() == 1 ? "" : "s") + " left in the draw pile");
        System.out.print("\nHand: ");
        p.display();
        System.out.println("\n*_*_*_*_*_*_*_*_*_*_");
    }
}