import java.awt.*;
import java.util.*;

public class Ai extends Player {
    Random rand = new Random(0);

    static Color[] colors = { Color.yellow, Color.blue, Color.white, Color.green, Color.red };
    static int[] numbers = { 0, 0, 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

    /* CONSTRUCTORS */

    /* Default CONSTRUCTOR */
    Ai() {
    }

    /* GETTER FUNCTIONS */

    /* Returns true if string is "ai" or something similar */
    @Override
    public boolean isIt(String s) {
        return s == "ai" || s == "AI" || s == "Ai";
    }

    /** Return random number between min and max */
    private int getRandomNumber(int min, int max) {
        return rand.nextInt(max - min) + min;
    }

    /* AUXILIARY FUNCTIONS */

    /* Conducts the turn if called on an ai object */
    @Override
    public void play(ArrayList<CardsCollection> opponent_placed_down, DiscardPiles discards, CardsCollection undealt) {
        int random_number = 0;
        Card outgoing_card;
        /** Decide to discard or play */
        // random_number = getRandomNumber(0, 2);
        // if (random_number == 0) {
        // // discard
        // /** Decide which card */
        // random_number = getRandomNumber(0, 8);
        // outgoing_card = getCardAt(random_number);
        // removeCard(outgoing_card);
        // discards.addCard(outgoing_card);
        // System.out.print("AI chose to discard ");

        // } else {
        // // play
        // /** Decide which card */
        // random_number = getRandomNumber(0, 8);
        // outgoing_card = getCardAt(random_number);
        // removeCard(outgoing_card);
        // placeCard(outgoing_card);
        // System.out.print("AI chose to place ");
        // }
        // outgoing_card.display();

        outgoing_card = outgoingPlay(opponent_placed_down, discards, undealt);

        System.out.print("\nAI's hand is now ");
        display();

        Card incoming_card;
        if (discards.isEmpty()) {
            /**
             * if AI can take a card from undealt pile only because there are no cards
             * in discard pile
             */
            System.out.println("\nAI took card from draw pile only because there are no cards in discard piles");
            incoming_card = undealt.getTopCard();
            undealt.removeCard(incoming_card);
        } else {
            /** Decide to take from undealt or one of the discard piles */
            random_number = getRandomNumber(0, 2);
            if (random_number == 0) {

                /** Decide which pile if discard piles */
                random_number = getRandomNumber(0, 5);
                incoming_card = discards.getTopCard(colors[random_number]);
                while (incoming_card.getCardColor() == Color.black) {
                    random_number = getRandomNumber(0, 5);
                    incoming_card = discards.getTopCard(colors[random_number]);
                }
                if (incoming_card == outgoing_card) {
                    incoming_card = undealt.getTopCard();
                    undealt.removeCard(incoming_card);
                    System.out.print("AI chose draw pile\n");
                } else {
                    discards.removeCard(incoming_card);
                    System.out.print("AI chose discard pile.\n");
                }
            } else {
                incoming_card = undealt.getTopCard();
                undealt.removeCard(incoming_card);
                System.out.print("AI chose draw pile\n");
            }

        }

        System.out.print("AI's getting ");
        incoming_card.display();
        addCard(incoming_card);// add the card to player's hand

        System.out.print("\nAI's hand is now ");
        display();
    }

    /*
     * General Approach:
     * - Calculate a potential score based on the cards known and unknown
     * - If it's greater than the opponent's potential score, place the best card
     * - If it's not, then discard the worst
     */
    public Card outgoingPlay(ArrayList<CardsCollection> opponent_placed_down, DiscardPiles discards,
            CardsCollection undealt) {

        ArrayList<CardsCollection> potential_placed_cards = makePotentialPlacedCards(hand, placed_down,
                opponent_placed_down, undealt);
        // System.out.println("potential placed cards: ");
        // for (int i = 0; i < potential_placed_cards.size(); i++) {
        //     potential_placed_cards.get(i).display();
        // }

        ArrayList<CardsCollection> opponent_potential_placed_cards = makePotentialPlacedCards(hand,
                opponent_placed_down, placed_down, undealt);
        // System.out.println("opponent potential placed cards: ");
        // for (int i = 0; i < opponent_potential_placed_cards.size(); i++) {
        //     opponent_potential_placed_cards.get(i).display();
        // }

        ArrayList<ArrayList<Double>> expected_scores = getExpectedScores(potential_placed_cards, undealt);

        // System.out.println("AI Expected Scores: ");
        // System.out.println(expected_scores.toString());

        // opponent expected score
        double total = 0;
        double perc = (((double) undealt.size() + 16) / 2) / ((double) undealt.size());
        for (int i = 0; i < potential_placed_cards.size(); i++) {
            double score = potential_placed_cards.get(getColorIndex((colors[i]))).getScore(20 * perc);
            total += score;
        }
        System.out.println("Opponent's total expected score = " + total);

        int placing_max_index = 0;
        int discarding_max_index = 0;
        for (int i = 0; i < expected_scores.get(1).size(); i++) {
            if (expected_scores.get(1).get(i) > expected_scores.get(1).get(placing_max_index))
                placing_max_index = i;
            if (expected_scores.get(0).get(i) > expected_scores.get(0).get(discarding_max_index))
                discarding_max_index = i;
        }

        Card outgoing_card;
        // if expected score of discarding is better than placing, then discard,
        // else place
        if (expected_scores.get(0).get(discarding_max_index) > expected_scores.get(1).get(placing_max_index)) {
            // discard the card when discarding gives a higher score than placing it
            outgoing_card = hand.getCardAt(discarding_max_index);
            System.out.print("AI discarded ");
            outgoing_card.display();
            removeCard(outgoing_card);
            discards.addCard(outgoing_card);
        } else {
            // place the card when placing it gives a higher score than discarding it
            outgoing_card = hand.getCardAt(placing_max_index);
            System.out.print("AI placed ");
            outgoing_card.display();
            removeCard(outgoing_card);
            placeCard(outgoing_card);
        }
        return outgoing_card;
    }

    /* PROTECTED FUNCTIONS */

    /*
     * - First makes a list of all cards, seperated by colors and ordered by value
     * (handshake first, and then ascending)
     * - Then, goes through each card, making the following changes
     * ---- if card is in AI's hand, or opponent has placed down, or the card is
     * less than the AI's last placed down in that color, then remove the card
     * ---- if card is not placed by AI, change the card's value to be a fraction
     * of itself
     * ---- if card is none of the above, then keep the card as it is
     */
    protected ArrayList<CardsCollection> makePotentialPlacedCards(CardsCollection player_hand,
            ArrayList<CardsCollection> player_placed_down, ArrayList<CardsCollection> opponent_placed_down,
            CardsCollection undealt) {
        ArrayList<CardsCollection> potential_placed_cards = new ArrayList<>();
        for (int i = 0; i < colors.length; i++) {
            potential_placed_cards.add(new CardsCollection());
            potential_placed_cards.get(i).makeColorPile(colors[i]);

            for (int j = 0; j < potential_placed_cards.get(i).size(); j++) {
                Card c = potential_placed_cards.get(i).getCardAt(j);
                potential_placed_cards.get(i).removeCard(c);
                if (player_placed_down == placed_down) {
                    if (player_hand.contains(c) || (c.getCardNumber() == 0)
                            || opponent_placed_down.get(i).contains(c)
                            || c.getCardNumber() < getTopPlacedCard(colors[i]).getCardNumber()) {
                        j--;
                    } else if (!player_placed_down.get(i).contains(c)) {
                        // anything other than player placed down or hand
                        double perc = (((double) undealt.size() + 8) / 2) / ((double) undealt.size());
                        c.setCardNumber(c.getCardNumber() * perc);
                        potential_placed_cards.get(i).addCard(c);
                    } else {
                        // If player placed
                        potential_placed_cards.get(i).addCard(c);
                    }
                } else {
                    if (c.getCardNumber() == 0 || opponent_placed_down.get(i).contains(c) || player_hand.contains(c)
                            || (!player_placed_down.get(i).isEmpty()
                                    && c.getCardNumber() < player_placed_down.get(i).getTopCard().getCardNumber())) {
                        j--;
                    } else if (!player_placed_down.get(i).contains(c)) {
                        double perc = (((double) undealt.size() + 16) / 2) / ((double) undealt.size());
                        c.setCardNumber(c.getCardNumber() * perc);
                        potential_placed_cards.get(i).addCard(c);
                    } else {
                        potential_placed_cards.get(i).addCard(c);
                    }
                }
            }
            for (int j = 0; j < player_placed_down.get(i).size(); j++) {
                if (player_placed_down.get(i).getCardAt(j).getCardNumber() != 0) {
                    break;
                } else {
                    potential_placed_cards.get(i).addCard(new Card(0, colors[i]));
                }
            }
        }
        return potential_placed_cards;
    }

    /*
     * 
     */
    protected ArrayList<ArrayList<Double>> getExpectedScores(ArrayList<CardsCollection> potential_placed_cards,
            CardsCollection undealt) {
        ArrayList<ArrayList<Double>> expected_scores = new ArrayList<>();
        expected_scores.add(new ArrayList<>()); // 1 = expected scores for discarding
        expected_scores.add(new ArrayList<>()); // 2 = expected scores for placing

        // for each card in the hand, calculate an expected score for placing it and
        // discarding it and add it to the appropriate ArrayList
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.getCardAt(i);
            Card c2 = new Card((int) c.getCardNumber(), c.getCardColor());

            // discarding it
            double total = 0;
            double perc = (((double) undealt.size() + 8) / 2) / ((double) undealt.size());
            c2.setCardNumber(c2.getCardNumber() * perc);
            potential_placed_cards.get(getColorIndex(c2.getCardColor())).addCard(c2);
            for (Color col : colors) {
                double score = potential_placed_cards.get(getColorIndex((col))).getScore(20 * perc);
                total += score;
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).removeCard(c2);
            // display expected score for discarding card
            expected_scores.get(0).add(total);

            // placing it
            CardsCollection placeable_cards_in_hand = hand.getCardsbyColor(c.getCardColor());
            for (int j = 0; j < placeable_cards_in_hand.size(); j++) {
                if (placeable_cards_in_hand.getCardAt(j).getCardNumber() < c.getCardNumber()) {
                    placeable_cards_in_hand.removeCard(placeable_cards_in_hand.getCardAt(j));
                }
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).addCards(placeable_cards_in_hand);
            total = 0;
            for (Color col : colors) {
                double score = potential_placed_cards.get(getColorIndex((col))).getScore(20 * perc);
                total += score;
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).removeCards(placeable_cards_in_hand);
            // display expected score for placing cards
            expected_scores.get(1).add(total);
        }
        return expected_scores;
    }
}