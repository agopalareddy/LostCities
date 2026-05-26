import java.io.*;
import java.awt.*;
import java.util.*;

public class Human extends Player {
    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    File file;
    Scanner in2;
    Scanner in3;

    /* CONSTRUCTORS */

    /* Default CONSTRUCTOR */
    public Human() {
    }

    /*
     * Takes in a string indicating the file name from in which moves have been
     * written already
     */
    public Human(String file_name) {
        file = new File(file_name);
        try {
            in2 = new Scanner(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        in3 = new Scanner(System.in);
    }

    /* GETTER FUNCTIONS */

    /* Returns true if string is "human" or something similar */
    @Override
    public boolean isIt(String s) {
        return s == "human" || s == "Human" || s == "HUMAN";
    }

    /* AUXILIARY FUNCTIONS */

    /* Conducts the turn if called on a human object */
    @Override
    public void play(ArrayList<CardsCollection> opponent_placed_down, DiscardPiles discards, CardsCollection undealt) {
        Card outgoing_card = outgoingPlay(discards);
        incomingPlay(discards, undealt, outgoing_card);
    }

    /* PROTECTED FUNCTIONS */

    protected Card outgoingPlay(DiscardPiles discards) {
        // outgoing part

        Card outgoing_card;

        /* Ask whether player wants to discard or place card */
        char[] choices1 = { 'd', 'p' };
        String discard_or_place = ask("\nDiscard or Place", choices1);

        /* Ask which card player wants to place */
        char[] choices2 = { '0', '1', '2', '3', '4', '5', '6', '7' };
        String outgoing_card_index_str = ask("Pick a card to play", choices2);
        int outgoing_card_index = Integer.parseInt(outgoing_card_index_str);
        outgoing_card = getCardAt(outgoing_card_index);

        if (discard_or_place.equalsIgnoreCase("d")) {
            removeCard(outgoing_card);
            discards.addCard(outgoing_card);
            System.out.print("You chose to discard ");
        } else {
            // if the player chooses to place a card that is smaller than the last placed
            // card in that card's color
            if (getTopPlacedCard(outgoing_card.getCardColor()).getCardColor() != Color.black
                    && outgoing_card.getCardNumber() < getTopPlacedCard(outgoing_card.getCardColor()).getCardNumber()) {
                System.out.println(
                        "The card you chose to place is less than the last placed card in that color. Pick another card to place.");
                outgoing_card_index_str = ask("Pick a card to play", choices2);
                outgoing_card_index = Integer.parseInt(outgoing_card_index_str);
                outgoing_card = getCardAt(outgoing_card_index);
            }
            removeCard(outgoing_card);
            placeCard(outgoing_card);
        }
        System.out.print("You chose to place ");
        outgoing_card.display();

        System.out.print("\nYour hand is now ");
        display();
        return outgoing_card;
    }

    protected void incomingPlay(DiscardPiles discards, CardsCollection undealt, Card outgoing_card) {
        Card incoming_card;
        if (discards.isEmpty()) {
            /*
             * if player can take a card from undealt pile only because there are no cards
             * in discard pile
             */
            System.out.println("\nDiscard piles are empty. You can take a card from draw pile only.");
            incoming_card = undealt.getTopCard();
            undealt.removeCard(incoming_card);
        } else if (discards.totalSize() == 1 && discards.getOnlyCard() == outgoing_card) {
            /*
             * There's only one card in all discard piles together, and that card is the one
             * the player just placed
             */
            System.out.println(
                    "\nYou cannot pick the card you just discarded. So, you need to take a card from draw pile only.");
            incoming_card = undealt.getTopCard();
            undealt.removeCard(incoming_card);
        } else {
            /* Ask whether player wants to take card from discard pile or undealt pile */
            char[] choices1 = { 'u', 'd' };
            String discard_or_undealt = ask("\nPick from Draw Pile or Discards", choices1);

            if (discard_or_undealt.equalsIgnoreCase("d")) {
                System.out.print("You chose discard pile.\n");
                String picked_color;
                discards.displayPiles();
                char[] choices2 = { 'y', 'b', 'w', 'g', 'r' };
                picked_color = ask("Pick a color", choices2);
                // ask until the pile which is not empty is chosen
                while (discards.isEmpty(getColor(picked_color))) {
                    System.out.println(
                            "The " + getColorName(getColor(picked_color)) + " Discard Pile chosen is empty");
                    discards.displayPiles();
                    picked_color = ask("Pick a color", choices2);
                }
                incoming_card = discards.getCard(picked_color);

                // if the player picked the same card as the one he just discarded
                while (incoming_card == outgoing_card) {
                    System.out.println("You just discarded that card. Pick another card.");
                    discards.displayPiles();
                    picked_color = ask("Pick a color", choices2);
                    // ask until the pile which is not empty is chosen
                    while (discards.isEmpty(getColor(picked_color))) {
                        System.out.println(
                                "The " + getColorName(getColor(picked_color)) + " Discard Pile chosen is empty");
                        discards.displayPiles();
                        picked_color = ask("Pick a color", choices2);
                    }
                }

                discards.removeCard(incoming_card);
            } else {
                incoming_card = undealt.getTopCard();
                undealt.removeCard(incoming_card);
                System.out.print("You chose draw pile\n");
            }
        }
        System.out.print("You're getting ");
        incoming_card.display();
        addCard(incoming_card);// add the card to player's hand

        System.out.print("\nYour hand is now ");
        display();
    }

    /*
     * Ask player for an input corresponding to the options shown. Keep asking until
     * player enters something in2 the given options (character array).
     * Return the string form of that choice
     */
    protected String ask(String s, char[] choices) {
        System.out.print(s);
        displayChoices(choices);
        String input = "";
        while (true) {
            input = getNextString();
            char input_char = Character.toLowerCase(input.charAt(0));
            boolean match = false;
            for (char ch : choices) {
                if (ch == input_char) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                System.out.print("Wrong input! " + s + " again");
                displayChoices(choices);
            } else {
                break;
            }
        }
        return input;
    }

    /* Output to console the choices player has to choose from */
    protected void displayChoices(char[] choices) {
        System.out.print(" [");
        for (int i = 0; i < choices.length; i++) {
            if (i == choices.length - 1)
                System.out.print(choices[i] + "]: ");
            else
                System.out.print(choices[i] + ", ");
        }
    }

    /*
     * If the file has another line, reads it and returns it
     * If not, takes input from player and returns it
     */
    protected String getNextString() {
        String s;
        if (in2.hasNextLine()) {
            s = in2.nextLine();
            System.out.println(s);
            return s;
        } else {
            return in3.nextLine();
        }
    }
}