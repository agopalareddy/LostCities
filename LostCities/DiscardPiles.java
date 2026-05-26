import java.awt.*;
import java.util.*;
/*
Holds:
    array of colors possible
    array of numbers possible (0 for handshake card)
    
    List of discard piles
*/

public class DiscardPiles {
    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    ArrayList<CardsCollection> discard_piles = new ArrayList<>(); // just holds a list of discard piles for each color

    /* CONSTRUCTORS */

    /* Make a discard pile for each color */
    public DiscardPiles() {
        discard_piles = new ArrayList<>();
        for (int i = 0; i < colors.length; i++) {
            discard_piles.add(new CardsCollection('d'));
        }
    }

    /* GETTER FUNCTIONS */

    /* Returns the total number of cards in all discard piles together */
    public int totalSize() {
        int sum = 0;
        for (Color col : colors) {
            sum += size(col);
        }
        return sum;
    }

    /* Returns the number of cards in the specific color's discard pile */
    public int size(Color col) {
        return discard_piles.get(getColorIndex(col)).size();
    }

    /* Return true if all discard piles are empty */
    public boolean isEmpty() {
        for (CardsCollection cards : discard_piles) {
            if (!cards.isEmpty())
                return false;
        }
        return true;
    }

    /*
     * OVERLOAD FUNCTION
     * Return true if the input color's discard pile is empty
     */
    public boolean isEmpty(Color col) {
        return getPile(col).isEmpty();
    }

    /* Return the corresponding color's discard pile */
    public CardsCollection getPile(Color col) {
        return discard_piles.get(getColorIndex(col));
    }

    /*
     * OVERLOAD FUNCTION
     * Return the corresponding color's discard pile (color passed as String)
     */
    public CardsCollection getPile(String picked_color) {
        for (Color col : colors) {
            if (isColorsMatching(picked_color, col))
                return discard_piles.get(getColorIndex(col));
        }
        return new CardsCollection();
    }

    /*
     * Return topmost card from the input color's discard pile if at least one card
     * is present in the discard pile. Else return new card
     */
    public Card getTopCard(Color col) {
        if (!discard_piles.get(getColorIndex(col)).isEmpty())
            return discard_piles.get(getColorIndex(col)).getTopCard();
        return new Card();
    }

    /*
     * OVERLOAD FUNCTION
     * Returns the topmost card from the given color's discard pile
     * (color passed as String)
     */
    public Card getCard(String picked_color) {
        for (Color col : colors) {
            if (!discard_piles.get(getColorIndex(col)).isEmpty()
                    && isColorsMatching(picked_color, col))
                return getTopCard(col);
        }
        return new Card();
    }

    /* Returns a card if it's the only card, else a new card */
    public Card getOnlyCard() {
        if (totalSize() == 1) {
            for (Color col : colors) {
                if (size(col) == 1) {
                    return getTopCard(col);
                }
            }
        }
        return new Card();
    }

    /* DISPLAY FUNCTIONS */

    /* Output discard piles to console */
    public void displayPiles() {
        System.out.println("Discard Piles: ");
        for (Color col : colors) {
            System.out.print(getColorName(col) + ":\t");
            discard_piles.get(getColorIndex(col)).display();
        }
    }

    /* AUXILIARY FUNCTIONS */

    /* Add card into the appropriate discard pile */
    public void addCard(Card c) {
        discard_piles.get(getColorIndex(c.getCardColor())).addCard(c);
    }

    /* Remove topmost card from given color's discard pile */
    public void removeCard(Color col) {
        discard_piles.get(getColorIndex(col)).removeCard(getTopCard(col));
    }

    /*
     * OVERLOAD FUNCTION
     * Takes in a card
     * Verifies it's the topmost card
     * if it is, removes it
     */
    public void removeCard(Card c) {
        if (isTopCard(c))
            removeCard(c.getCardColor());
    }

    /* PROTECTED FUNCTIONS */

    /* Return true if given card is topmost card */
    protected boolean isTopCard(Card c) {
        return c == getTopCard(c.getCardColor());
    }

    /*
     * Return true if the given string form of the color matches the given color
     */
    protected boolean isColorsMatching(String picked_color, Color col) {
        return Character.toLowerCase(picked_color.charAt(0)) == Character.toLowerCase(getColorName(col).charAt(0));
    }

    /* Return index of discard pile according to given color */
    protected int getColorIndex(Color col) {
        for (int i = 0; i < colors.length; i++) {
            if (col == colors[i])
                return i;
        }
        return -1;
    }

    /* Return string form of color passed as parameter */

    protected String getColorName(Color col) {
        return (col == colors[0]) ? "Yellow"
                : (col == colors[1]) ? "Blue"
                        : (col == colors[2]) ? "White"
                                : (col == colors[3]) ? "Green"
                                        : (col == colors[4]) ? "Red" : "";
    }
}
