import java.awt.*;

/*
Holds:
    array of colors possible
    array of numbers possible (0 for handshake card)

    card number
    card color
*/

public class Card {
    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    public double card_number;
    public Color card_color;

    /* CONSTRUCTORS */

    /* Set defalut values */
    public Card() {
        card_number = Integer.MIN_VALUE;
        card_color = Color.black;
    }

    /* Set internal variables to parameters */
    public Card(int num, Color col) {
        for (int i = 0; i < colors.length; i++) {
            if (col == colors[i])
                break;
            else if (col != colors[i] && i == colors.length - 1)
                throw new java.lang.Error("Invalid Color input");
        }
        card_number = num;
        card_color = col;
    }

    /* GETTER FUNCTIONS */

    /* Return card number */
    public double getCardNumber() {
        return card_number;
    }

    /* Return card color */
    public Color getCardColor() {
        return card_color;
    }

    /* Return card color in string type */
    public String getCardColorName() {
        return getColorName(card_color);
    }

    /* SETTER FUNCTIONS */

    /* Sets the card's number to the value of the input number */
    public void setCardNumber(double num) {
        card_number = num;
    }

    /* DISPLAY FUNCTIONS */

    /* Output card to console */
    public void display() {
        System.out.print(getColorName(card_color).charAt(0));
        System.out.print((float) Math.round(card_number * 1000) / 1000);
    }

    /* PROTECTED FUNCTIONS */

    /* Return string form of color passed as parameter */
    protected String getColorName(Color col) {
        return (col == colors[0]) ? "Yellow"
                : (col == colors[1]) ? "Blue"
                        : (col == colors[2]) ? "White"
                                : (col == colors[3]) ? "Green"
                                        : (col == colors[4]) ? "Red" : "";
    }
}