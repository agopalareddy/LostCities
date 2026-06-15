import java.awt.*;
import java.util.*;

/**
 * MinimaxAi — finds the globally best (outgoing card + incoming draw) pair
 * for each turn via exhaustive enumeration with a heuristic leaf evaluation.
 *
 * Unlike GreedyAi (Ai), which picks the outgoing card first and then
 * independently picks the incoming draw, MinimaxAi treats the full turn as
 * a single decision: it enumerates every legal combination of
 *   (card × place-or-discard) × (draw from deck or from any discard pile)
 * and picks the combination with the highest evalPosition() score.
 *
 * This is provably no worse than GreedyAi because the greedy choice is always
 * a member of the search space. In practice it is better when the best
 * outgoing card depends on the expected incoming draw (e.g. discarding a
 * card of a color whose discard pile becomes the best draw option).
 *
 * There is no explicit min-node (opponent response) in this class; that is
 * added by AlphaBetaAi.
 */
public class MinimaxAi extends Ai {

    MinimaxAi() {}

    @Override
    public boolean isIt(String s) {
        return "minimax".equalsIgnoreCase(s);
    }

    @Override
    public void play(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt) {
        executeBestTurn(opponentPlaced, discards, undealt);
        System.out.print("\nMinimax AI hand is now ");
        display();
    }

    /**
     * Enumerate every legal (outgoing move, incoming draw) pair, evaluate each
     * using evalPosition(), and execute the pair with the highest value.
     *
     * Undo mechanics (all safe because CardsCollection uses reference equality
     * for remove and maintains sorted order on re-insert):
     *  - place sim:   hand.removeCard / placed_down[i].addCard ... undo: removeCard / hand.addCard
     *  - discard sim: hand.removeCard / discards.addCard ...       undo: discards.removeCard / hand.addCard
     *  - incoming:    hand.addCard(candidate) for eval, then hand.removeCard — deck and
     *                 discard sources are NOT modified during search; only the hand changes.
     */
    private void executeBestTurn(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt) {

        double bestVal = Double.NEGATIVE_INFINITY;
        Card bestOut = hand.getCardAt(0);   // fallback
        boolean bestPlace = false;
        Card bestIn = null;                 // null ⇒ draw from deck
        boolean bestInFromDeck = true;

        int n = hand.size();
        for (int i = 0; i < n; i++) {
            Card out = hand.getCardAt(i);
            int cidx = getColorIndex(out.getCardColor());

            // ── Try PLACE ─────────────────────────────────────────────────────
            if (isPlaceLegal(out)) {
                // Simulate place.
                hand.removeCard(out);
                placed_down.get(cidx).addCard(out);

                // Enumerate incoming options (discard piles unchanged; no exclude).
                for (Card in : getDrawOptions(discards, undealt, null)) {
                    Card actual = (in == null) ? undealt.getTopCard() : in;
                    hand.addCard(actual);
                    double val = evalPosition(opponentPlaced, discards, undealt);
                    hand.removeCard(actual);

                    if (val > bestVal) {
                        bestVal = val; bestOut = out; bestPlace = true;
                        bestIn = in; bestInFromDeck = (in == null);
                    }
                }

                // Undo place.
                placed_down.get(cidx).removeCard(out);
                hand.addCard(out);
            }

            // ── Try DISCARD ────────────────────────────────────────────────────
            {
                // Simulate discard.
                hand.removeCard(out);
                discards.addCard(out);

                // Enumerate incoming options (out is now top of its colour; excluded).
                for (Card in : getDrawOptions(discards, undealt, out)) {
                    Card actual = (in == null) ? undealt.getTopCard() : in;
                    hand.addCard(actual);
                    double val = evalPosition(opponentPlaced, discards, undealt);
                    hand.removeCard(actual);

                    if (val > bestVal) {
                        bestVal = val; bestOut = out; bestPlace = false;
                        bestIn = in; bestInFromDeck = (in == null);
                    }
                }

                // Undo discard.
                discards.removeCard(out);
                hand.addCard(out);
            }
        }

        // ── Execute best outgoing ──────────────────────────────────────────────
        if (bestPlace) {
            System.out.print("Minimax AI placed "); bestOut.display();
            removeCard(bestOut);
            placeCard(bestOut);
        } else {
            System.out.print("Minimax AI discarded "); bestOut.display();
            removeCard(bestOut);
            discards.addCard(bestOut);
        }

        // ── Execute best incoming ──────────────────────────────────────────────
        if (bestInFromDeck || bestIn == null) {
            Card c = undealt.getTopCard();
            undealt.removeCard(c);
            System.out.print("Minimax AI drew from deck. ");
            addCard(c);
        } else {
            discards.removeCard(bestIn);
            System.out.print("Minimax AI drew from discard. ");
            addCard(bestIn);
        }
    }

    // ── Helpers (also used by AlphaBetaAi) ────────────────────────────────────

    /**
     * Return list of drawable card options:
     *   null  → draw from deck (identity of top card not inspected until draw time)
     *   Card  → specific top-of-discard-pile card
     *
     * Cards are included only if they are genuinely accessible:
     *   - Deck option included when the undealt pile is non-empty.
     *   - Discard pile top included when the pile is non-empty AND the card is
     *     not the one we just discarded (which sits on top and cannot be re-taken).
     */
    protected java.util.List<Card> getDrawOptions(DiscardPiles discards,
            CardsCollection undealt, Card exclude) {
        java.util.List<Card> opts = new ArrayList<>();
        if (!undealt.isEmpty()) opts.add(null); // null = deck
        for (Color col : colors) {
            Card top = discards.getTopCard(col);
            if (top.getCardColor() == Color.black) continue; // empty pile sentinel
            if (top == exclude) continue;                    // just-discarded card
            opts.add(top);
        }
        return opts;
    }

    /**
     * True if placing card c on our expedition is a legal move.
     * A card can be placed when the expedition of its colour is empty, or when
     * the card's number is ≥ the current top card's number (stacking in order).
     */
    protected boolean isPlaceLegal(Card card) {
        int idx = getColorIndex(card.getCardColor());
        CardsCollection pile = placed_down.get(idx);
        return pile.isEmpty() || card.getCardNumber() >= pile.getTopCard().getCardNumber();
    }
}
