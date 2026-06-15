import java.awt.*;
import java.util.*;

/**
 * Ai (Greedy) — makes locally-optimal decisions at each step.
 *
 * OUTGOING card: uses an expected-score heuristic (makePotentialPlacedCards +
 * getExpectedScores) to pick the best (card, place-or-discard) action. This
 * is unchanged from the original implementation and is deliberately simple.
 *
 * INCOMING card: evaluates ALL available draw options (deck top + each
 * non-empty discard pile top) via evalPosition() and picks the card that
 * maximises our expected-score advantage. The original code chose randomly
 * (50 % deck, 50 % random discard pile), which is clearly sub-optimal.
 *
 * Decisions are made SEQUENTIALLY — outgoing first, then incoming — rather
 * than jointly. This can miss globally better (outgoing, incoming) pairs;
 * MinimaxAi corrects this.
 */
public class Ai extends Player {

    /* CONSTRUCTORS */
    Ai() {}

    /* GETTER FUNCTIONS */

    @Override
    public boolean isIt(String s) {
        return "ai".equalsIgnoreCase(s) || "greedy".equalsIgnoreCase(s);
    }

    /* AUXILIARY FUNCTIONS */

    /** Conducts one full turn: outgoing card then incoming draw. */
    @Override
    public void play(ArrayList<CardsCollection> opponent_placed_down, DiscardPiles discards, CardsCollection undealt) {
        // Phase 1 — outgoing card (heuristic, unchanged).
        Card outgoing_card = outgoingPlay(opponent_placed_down, discards, undealt);

        System.out.print("\nAI's hand is now ");
        display();

        // Phase 2 — incoming card: evaluate every draw option, pick the best.
        Card incoming_card = bestIncomingCard(opponent_placed_down, discards, undealt, outgoing_card);

        System.out.print("AI's getting ");
        incoming_card.display();
        addCard(incoming_card);

        System.out.print("\nAI's hand is now ");
        display();
    }

    /**
     * Greedy outgoing: pick the (card, place/discard) with the highest expected
     * score impact. Returns the card played so that bestIncomingCard can exclude
     * a just-discarded card from the draw pool.
     */
    public Card outgoingPlay(ArrayList<CardsCollection> opponent_placed_down,
            DiscardPiles discards, CardsCollection undealt) {

        ArrayList<CardsCollection> potential_placed_cards =
                makePotentialPlacedCards(hand, placed_down, opponent_placed_down, undealt);

        ArrayList<ArrayList<Double>> expected_scores = getExpectedScores(potential_placed_cards, undealt);

        // Print our own total expected score (informational).
        double total = 0;
        double perc = undealt.size() > 0
                ? (((double) undealt.size() + 16) / 2) / undealt.size()
                : 1.0;
        for (int i = 0; i < potential_placed_cards.size(); i++) {
            total += potential_placed_cards.get(getColorIndex(colors[i])).getScore(20 * perc);
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
        if (expected_scores.get(0).get(discarding_max_index) > expected_scores.get(1).get(placing_max_index)) {
            outgoing_card = hand.getCardAt(discarding_max_index);
            System.out.print("AI discarded ");
            outgoing_card.display();
            removeCard(outgoing_card);
            discards.addCard(outgoing_card);
            lastDiscardedCard = outgoing_card;
        } else {
            outgoing_card = hand.getCardAt(placing_max_index);
            System.out.print("AI placed ");
            outgoing_card.display();
            removeCard(outgoing_card);
            placeCard(outgoing_card);
            lastDiscardedCard = null;
        }
        return outgoing_card;
    }

    /**
     * Greedy incoming: evaluate each available draw option (deck top and every
     * non-empty discard pile top, excluding the card just discarded) and pick
     * the one that maximises evalPosition(). Replaces the original random draw.
     */
    protected Card bestIncomingCard(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt, Card outCard) {

        if (discards.isEmpty() || undealt.size() <= 1) {
            System.out.println("\nAI took from draw pile.");
            Card c = undealt.getTopCard();
            undealt.removeCard(c);
            return c;
        }

        double bestVal = Double.NEGATIVE_INFINITY;
        Card bestCard = null;
        boolean bestFromDeck = true;

        // Option: draw from deck.
        if (!undealt.isEmpty()) {
            Card deckTop = undealt.getTopCard();
            hand.addCard(deckTop);
            double val = evalPosition(opponentPlaced, discards, undealt);
            hand.removeCard(deckTop);
            if (val > bestVal) {
                bestVal = val;
                bestCard = deckTop;
                bestFromDeck = true;
            }
        }

        // Option: draw from each non-empty discard pile top.
        for (Color col : colors) {
            Card top = discards.getTopCard(col);
            if (top.getCardColor() == Color.black) continue; // empty pile sentinel
            if (top == outCard) continue;                    // can't re-take just-discarded
            if (lastDiscardedCard != null && top.getCardColor() == lastDiscardedCard.getCardColor()
                    && top.getCardNumber() == lastDiscardedCard.getCardNumber()) {
                continue; // prevent loops
            }

            hand.addCard(top);
            double val = evalPosition(opponentPlaced, discards, undealt);
            hand.removeCard(top);

            if (val > bestVal) {
                bestVal = val;
                bestCard = top;
                bestFromDeck = false;
            }
        }

        // Execute the chosen draw.
        if (bestFromDeck || bestCard == null) {
            Card c = undealt.getTopCard();
            undealt.removeCard(c);
            System.out.print("AI chose draw pile. ");
            return c;
        } else {
            discards.removeCard(bestCard);
            System.out.print("AI chose discard pile. ");
            return bestCard;
        }
    }

    /* PROTECTED FUNCTIONS */

    /**
     * Evaluate the current position as: our expected score − opponent's estimated
     * score. Used as the leaf-node evaluation function for MinimaxAi and
     * AlphaBetaAi.
     *
     * Our expected score is computed via makePotentialPlacedCards (accounts for
     * cards already placed, cards in hand, and the probability of drawing future
     * cards). Opponent's score is estimated from their placed cards only (their
     * hand is unknown), weighted by the same draw-probability factor.
     */
    protected double evalPosition(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt) {

        double ourScore = evaluateKnownPosition(placed_down, hand);
        double oppScore = 0;
        for (CardsCollection pile : opponentPlaced) {
            oppScore += pile.getScore();
        }

        return ourScore - oppScore;
    }

    protected double evaluateKnownPosition(ArrayList<CardsCollection> placed, CardsCollection availableHand) {
        double total = 0;
        for (Color col : colors) {
            int idx = getColorIndex(col);
            CardsCollection options = availableHand.getCardsbyColor(col);
            total += bestKnownColorScore(placed.get(idx), options);
        }
        return total;
    }

    private double bestKnownColorScore(CardsCollection placed, CardsCollection options) {
        int count = options.size();
        double best = placed.isEmpty() ? 0 : placed.getScore();

        for (int mask = 1; mask < (1 << count); mask++) {
            CardsCollection candidate = copyCards(placed);
            boolean legal = true;

            for (int i = 0; i < count; i++) {
                if ((mask & (1 << i)) == 0) continue;

                Card card = options.getCardAt(i);
                if (!candidate.isEmpty() && card.getCardNumber() < candidate.getTopCard().getCardNumber()) {
                    legal = false;
                    break;
                }
                candidate.addCard(new Card((int) card.getCardNumber(), card.getCardColor()));
            }

            if (legal) {
                best = Math.max(best, candidate.getScore());
            }
        }
        return best;
    }

    private CardsCollection copyCards(CardsCollection source) {
        CardsCollection copy = new CardsCollection();
        for (int i = 0; i < source.size(); i++) {
            Card card = source.getCardAt(i);
            copy.addCard(new Card((int) card.getCardNumber(), card.getCardColor()));
        }
        return copy;
    }

    /*
     * - First makes a list of all cards, separated by colors and ordered by value
     *   (handshake first, then ascending).
     * - Then, for each card:
     *   - If it is in the AI's hand, or opponent has placed it, or it is below the
     *     AI's last placed card in that color → remove it from potential.
     *   - If it has not been placed by the AI → scale its value by draw probability.
     *   - Otherwise (already placed by AI) → keep at face value.
     */
    protected ArrayList<CardsCollection> makePotentialPlacedCards(CardsCollection player_hand,
            ArrayList<CardsCollection> player_placed_down,
            ArrayList<CardsCollection> opponent_placed_down,
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
                        double perc = futureCardScale(undealt, 8);
                        c.setCardNumber(c.getCardNumber() * perc);
                        potential_placed_cards.get(i).addCard(c);
                    } else {
                        potential_placed_cards.get(i).addCard(c);
                    }
                } else {
                    if (c.getCardNumber() == 0 || opponent_placed_down.get(i).contains(c)
                            || player_hand.contains(c)
                            || (!player_placed_down.get(i).isEmpty()
                                    && c.getCardNumber() < player_placed_down.get(i).getTopCard().getCardNumber())) {
                        j--;
                    } else if (!player_placed_down.get(i).contains(c)) {
                        double perc = futureCardScale(undealt, 16);
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

    protected ArrayList<ArrayList<Double>> getExpectedScores(
            ArrayList<CardsCollection> potential_placed_cards, CardsCollection undealt) {

        ArrayList<ArrayList<Double>> expected_scores = new ArrayList<>();
        expected_scores.add(new ArrayList<>()); // index 0 — expected score if discarded
        expected_scores.add(new ArrayList<>()); // index 1 — expected score if placed

        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.getCardAt(i);
            Card c2 = new Card((int) c.getCardNumber(), c.getCardColor());

            // Score for discarding card c.
            double total = 0;
            double perc = futureCardScale(undealt, 8);
            c2.setCardNumber(c2.getCardNumber() * perc);
            potential_placed_cards.get(getColorIndex(c2.getCardColor())).addCard(c2);
            for (Color col : colors) {
                total += potential_placed_cards.get(getColorIndex(col)).getScore(20 * perc);
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).removeCard(c2);
            expected_scores.get(0).add(total);

            // Score for placing card c (includes all same-color cards in hand with
            // value >= c, since placing c allows placing those later).
            CardsCollection placeable_cards_in_hand = hand.getCardsbyColor(c.getCardColor());
            for (int j = 0; j < placeable_cards_in_hand.size(); j++) {
                if (placeable_cards_in_hand.getCardAt(j).getCardNumber() < c.getCardNumber()) {
                    placeable_cards_in_hand.removeCard(placeable_cards_in_hand.getCardAt(j));
                    j--;
                }
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).addCards(placeable_cards_in_hand);
            total = 0;
            for (Color col : colors) {
                total += potential_placed_cards.get(getColorIndex(col)).getScore(20 * perc);
            }
            potential_placed_cards.get(getColorIndex(c.getCardColor())).removeCards(placeable_cards_in_hand);
            expected_scores.get(1).add(total);
        }
        return expected_scores;
    }

    protected double futureCardScale(CardsCollection undealt, double knownCards) {
        if (undealt.isEmpty()) return 1.0;
        return (((double) undealt.size() + knownCards) / 2) / undealt.size();
    }
}
