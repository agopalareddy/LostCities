import java.awt.*;
import java.util.*;

/*
Holds
    array of colors possible
    array of numbers possible (0 for handshake card)

    a pile of cards
    whether the pile is a discard pile or not
    whether the pile is the undealt cards pile or not
*/

public class CardsCollection {
    Random rand;

    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    private ArrayList<Card> pile = new ArrayList<>(); // just holds a list of cards
    private boolean is_discard_pile; // holds whether the pile is a discard pile or not
    private boolean is_undealtCards; // holds whether the pile is undealt cards pile or not

    /* CONSTRUCTORS */

    public CardsCollection(char ch) {
        this(ch, "random");
    }

    public CardsCollection(char ch, String seedOption) {
        if ("fixed".equals(seedOption) || "0".equals(seedOption)) {
            rand = new Random(0);
        } else {
            rand = new Random();
        }

        if (ch == 'U' || ch == 'u') {// if undealt cards pile
            is_undealtCards = true;
            is_discard_pile = false;
            makeUndealtCardsPile();
        } else if (ch == 'D' || ch == 'd') {// if discard pile
            is_undealtCards = false;
            is_discard_pile = true;
        } else {
            is_undealtCards = false;
            is_discard_pile = false;
        }
    }

    /* Makes non-special pile of cards */
    public CardsCollection() {
        rand = new Random();
        is_undealtCards = false;
        is_discard_pile = false;
    }

    /* GETTER FUNCTIONS */

    /* Return size of cards */
    public int size() {
        return pile.size();
    }

    /* Returns true if a card exists in the cards */
    public boolean contains(Card c) {
        for (int i = 0; i < pile.size(); i++) {
            if (pile.get(i).getCardColor() == c.getCardColor() && pile.get(i).getCardNumber() == c.getCardNumber())
                return true;
        }
        return false;
    }

    /* Returns true if empty */
    public boolean isEmpty() {
        return pile.isEmpty();
    }

    /* Return topmost card */
    public Card getTopCard() {
        return pile.get(pile.size() - 1);
    }

    /* Return specific card from index */
    public Card getCardAt(int index) {
        return pile.get(index);
    }

    /* Return the smallest card in a color */
    public Card getSmallestCard(Color col) {
        return getCardsbyColor(col).getCardAt(0);
    }

    /* Return cards of a specific color as CardsCollection */
    public CardsCollection getCardsbyColor(Color col) {
        CardsCollection cards = new CardsCollection();
        for (Card c : pile) {
            if (c.getCardColor() == col)
                cards.addCard(c);
        }
        return cards;
    }

    /* Return cards of a specific color as ArrayList */
    public ArrayList<Card> getCardsbyColorAsArrayList(Color col) {
        ArrayList<Card> cards = new ArrayList<>();
        for (Card c : pile) {
            if (c.getCardColor() == col)
                cards.add(c);
        }
        return cards;
    }

    /*
     * Return the score of the pile based on the cards in the pile
     * - count multipliers (multiplier++)
     * - sum numbered cards (sum+=c.getCardNumber())
     * - deduct 20 (sum-=20)
     * - multiply sum and multipliers (sum*=multiplier)
     * - add 20 bonus if more than 8 cards are placed down (sum+=20)
     * - add sum to total
     */

    public double getScore(String s, double bonus) {
        sort();
        double multiplier = 1;
        double sum = 0;
        // count multipliers and sum of numbered cards
        for (Card c : pile) {
            if (c.getCardNumber() == 0.000) {
                multiplier++;
            } else {
                sum += c.getCardNumber();
            }
        }
        // cost
        if (!pile.isEmpty()) {
            sum -= 20;
        }
        double multiplied_sum = sum * multiplier;
        double final_sum = multiplied_sum;
        if (pile.size() > 7) {
            final_sum += bonus;
        }

        // if asked to display score too (like at the end of the game), display it
        if (s != "") {
            System.out.println("Sum (before -20)\t= " + (sum + 20));
            System.out.println("Sum (after -20)\t\t= " + sum);
            System.out.println("\tMultiplier\t= " + multiplier);
            System.out.println("\tSum Now\t\t\t= " + multiplied_sum);
            if (final_sum > multiplied_sum) {// bonus points
                System.out.println("\tBonus Points\t= " + bonus);
                System.out.println("\tSum after bonus points\t\t= " + final_sum);
            }
        }
        return final_sum;
    }

    /*
     * When no input is provided to getScore function, call it without displaying
     * the score
     */
    public double getScore() {
        return getScore("", 20);
    }

    /* When when a string only is input but no bonus point changes */
    public double getScore(String s) {
        if (s != "")
            return getScore(s, 20);
        return getScore();
    }

    /* When when only a bonus point change is requested */
    public double getScore(double n) {
        return getScore("", n);
    }

    /* DISPLAY FUNCTIONS */

    /* Display cards to console */
    public void display() {
        if (pile.isEmpty()) {
            System.out.println("[]");
            return;
        }
        System.out.print("[");
        if (is_discard_pile)
            getTopCard().display();
        else
            for (Card c : pile) {
                c.display();
                if (c != getTopCard())
                    System.out.print(", ");
            }
        System.out.println("]");
    }

    /* AUXILIARY FUNCTIONS */

    /* Add card passed as parameter to cards based on what kind of pile it is */
    public void addCard(Card c) {
        if (is_discard_pile || is_undealtCards) {
            pile.add(c);
        } else {
            pile.add(c);
            sort();
        }
    }

    public void addCardAt(int n, Card c) {
        if (is_discard_pile || is_undealtCards) {
            pile.add(c);
        } else {
            pile.add(n, c);
            sort();
        }
    }

    /* Add a collection of cards */
    public void addCards(CardsCollection cards) {
        if (is_discard_pile || is_undealtCards) {
            for (int i = 0; i < cards.size(); i++) {
                pile.add(cards.getCardAt(i));
            }
        } else {
            for (int i = 0; i < cards.size(); i++) {
                pile.add(cards.getCardAt(i));
            }
            sort();
        }
    }

    /* Remove a specific card */
    public void removeCard(Card c) {
        pile.remove(c);
    }

    /*
     * OVERLOAD FUNCTION
     * Takes in a color and number. Creates a card using parameters and removes that
     * card from cards
     */
    public void removeCard(int num, Color col) {
        pile.remove(new Card(num, col));
    }

    /* Add a collection of cards */
    public void removeCards(CardsCollection cards) {
        if (is_discard_pile || is_undealtCards) {
            for (int i = 0; i < cards.size(); i++) {
                pile.remove(cards.getCardAt(i));
            }
        } else {
            for (int i = 0; i < cards.size(); i++) {
                pile.remove(cards.getCardAt(i));
            }
            sort();
        }
    }

    /* Make a pile of Undealt Cards and shuffle it */

    public void makeUndealtCardsPile() {
        for (Color col : colors) {
            // add number and handshake cards
            for (int num : numbers) {
                addCard(new Card(num, col));
            }
        }

        for (int i = 0; i < 6; i++) {
            if (!is_discard_pile) {
                Collections.shuffle(pile, rand);
            }
        }
    }

    /* If not discard pile, sort cards according to numbers in each color */
    public void sort() {
        ArrayList<Card> sorted_cards = new ArrayList<>();
        for (int x = 0; x < colors.length && !is_discard_pile; x++) {
            ArrayList<Card> c = getCardsbyColorAsArrayList(colors[x]);
            c.sort(Comparator.comparingDouble(Card::getCardNumber));
            sorted_cards.addAll(c);
        }
        pile = sorted_cards;
    }

    /* Creates a pile of cards from 0 to 9 of the same color */
    public void makeColorPile(Color col) {
        for (int num : numbers) {
            addCard(new Card(num, col));
        }
    }
}