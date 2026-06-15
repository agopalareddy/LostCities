import java.awt.*;
import java.util.*;

/**
 * AlphaBetaAi — extends MinimaxAi with a 2-ply adversarial search.
 *
 * MinimaxAi only searches our own best (outgoing, incoming) pair. AlphaBetaAi
 * adds a MIN node: after our complete turn, it estimates the opponent's best
 * incoming draw and penalises our evaluation accordingly. The outer loop
 * (over our outgoing choices) is pruned with the standard alpha-beta rule.
 *
 * Search tree structure (depth = 2 plies):
 *   Ply 1 — MAX: our outgoing card × action (16 options).
 *   Ply 1 — MAX: our incoming draw (up to 6 options, within the same ply).
 *   Ply 2 — MIN: opponent's best incoming draw (up to 6 options).
 *              Opponent draws the card that maximises their expected gain,
 *              which we estimate without knowing their hand via
 *              estimateOppCardGain().
 *   Leaf  — evalPosition() adjusted by opponent's gain.
 *
 * Alpha-beta pruning is applied on the outgoing-card dimension (ply 1).
 * Pruning on the opponent's draw loop (ply 2) is via a local beta cut.
 */
public class AlphaBetaAi extends MinimaxAi {

    AlphaBetaAi() {}

    @Override
    public boolean isIt(String s) {
        return "alphabeta".equalsIgnoreCase(s)
                || "alpha-beta".equalsIgnoreCase(s)
                || "ab".equalsIgnoreCase(s);
    }

    @Override
    public void play(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt) {
        executeBestTurnAB(opponentPlaced, discards, undealt);
        System.out.print("\nAlpha-Beta AI hand is now ");
        display();
    }

    private void executeBestTurnAB(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt) {

        double alpha = Double.NEGATIVE_INFINITY;
        double beta  = Double.POSITIVE_INFINITY;

        double bestVal = Double.NEGATIVE_INFINITY;
        Card bestOut = hand.getCardAt(0);
        boolean bestPlace = false;
        Card bestIn = null;          // null ⇒ deck
        boolean bestInFromDeck = true;

        int n = hand.size();
        for (int i = 0; i < n; i++) {
            Card out = hand.getCardAt(i);
            int cidx = getColorIndex(out.getCardColor());

            // ── Try PLACE ───────────────────────────────────────────────────
            if (isPlaceLegal(out)) {
                hand.removeCard(out);
                placed_down.get(cidx).addCard(out);

                for (Card in : getDrawOptions(discards, undealt, null)) {
                    Card actual = (in == null) ? undealt.getTopCard() : in;
                    hand.addCard(actual);

                    // 2-ply: after our (place, draw), apply opponent's best draw.
                    double val = evalWithOpponentResponse(opponentPlaced, discards, undealt, alpha, beta);

                    hand.removeCard(actual);

                    if (val > bestVal) {
                        bestVal = val; bestOut = out; bestPlace = true;
                        bestIn = in; bestInFromDeck = (in == null);
                        alpha = Math.max(alpha, val);
                    }
                    if (alpha >= beta) break; // prune incoming loop
                }

                placed_down.get(cidx).removeCard(out);
                hand.addCard(out);
            }

            if (alpha >= beta) break; // prune outgoing loop (alpha-beta on ply 1)

            // ── Try DISCARD ─────────────────────────────────────────────────
            {
                hand.removeCard(out);
                discards.addCard(out);

                for (Card in : getDrawOptions(discards, undealt, out)) {
                    Card actual = (in == null) ? undealt.getTopCard() : in;
                    hand.addCard(actual);

                    double val = evalWithOpponentResponse(opponentPlaced, discards, undealt, alpha, beta);

                    hand.removeCard(actual);

                    if (val > bestVal) {
                        bestVal = val; bestOut = out; bestPlace = false;
                        bestIn = in; bestInFromDeck = (in == null);
                        alpha = Math.max(alpha, val);
                    }
                    if (alpha >= beta) break;
                }

                discards.removeCard(out);
                hand.addCard(out);
            }

            if (alpha >= beta) break;
        }

        // ── Execute best outgoing ────────────────────────────────────────────
        if (bestPlace) {
            System.out.print("Alpha-Beta AI placed "); bestOut.display();
            removeCard(bestOut);
            placeCard(bestOut);
            lastDiscardedCard = null;
        } else {
            System.out.print("Alpha-Beta AI discarded "); bestOut.display();
            removeCard(bestOut);
            discards.addCard(bestOut);
            lastDiscardedCard = bestOut;
        }

        // ── Execute best incoming ────────────────────────────────────────────
        if (bestInFromDeck || bestIn == null) {
            Card c = undealt.getTopCard();
            undealt.removeCard(c);
            System.out.print("Alpha-Beta AI drew from deck. ");
            addCard(c);
        } else {
            discards.removeCard(bestIn);
            System.out.print("Alpha-Beta AI drew from discard. ");
            addCard(bestIn);
        }
    }

    /**
     * 2-ply leaf evaluation: take our base evalPosition() and subtract the
     * best gain the opponent can achieve by choosing their incoming draw.
     *
     * The opponent is modelled as a minimiser over the currently available
     * draw options (deck top + discard tops). For each option, we estimate
     * how much drawing that card benefits the opponent (estimateOppCardGain),
     * which reduces our relative advantage. A local beta cut exits early.
     *
     * Note: we do NOT know the opponent's hand, so we cannot model their
     * outgoing card choice. Their outgoing move is implicitly captured in the
     * opponentPlaced state, which grows over time and is already reflected in
     * evalPosition(). We model only their incoming draw here, which is the
     * only information we have direct control over (their access to our discard).
     */
    private double evalWithOpponentResponse(ArrayList<CardsCollection> opponentPlaced,
            DiscardPiles discards, CardsCollection undealt,
            double alpha, double beta) {

        double baseEval = evalPosition(opponentPlaced, discards, undealt);

        // MIN node: opponent picks the draw that maximises their gain (= minimises our baseEval).
        double worstForUs = Double.POSITIVE_INFINITY;
        double localBeta = beta; // opponent's beta

        // Opponent can draw from each non-empty discard pile top.
        for (Color col : colors) {
            Card top = discards.getTopCard(col);
            if (top.getCardColor() == Color.black) continue; // empty pile

            double gain = estimateOppCardGain(top, opponentPlaced);
            double adjusted = baseEval - gain;

            if (adjusted < worstForUs) {
                worstForUs = adjusted;
                localBeta = Math.min(localBeta, worstForUs);
                if (localBeta <= alpha) return worstForUs; // alpha cut
            }
        }

        // Opponent can also draw from the deck. The deck top is visible in the
        // server output but is an uncertain card for us here; we use the average
        // card value as a conservative estimate (discounted for uncertainty).
        if (!undealt.isEmpty()) {
            // Average card value in Lost Cities (handshakes ≈ 3 equivalent, numbers 2-10 avg ≈ 6).
            double avgGain = 2.5;
            double adjusted = baseEval - avgGain;
            if (adjusted < worstForUs) worstForUs = adjusted;
        }

        return (worstForUs == Double.POSITIVE_INFINITY) ? baseEval : worstForUs;
    }

    /**
     * Estimate how much drawing card c benefits the opponent (in score units).
     *
     * Rules:
     *  - Handshake (value 0): high value if opponent has started that expedition
     *    (adds a multiplier), moderate if they haven't (might start one).
     *  - Numbered card that fits in opponent's expedition (>= top placed):
     *    contributes its face value directly.
     *  - Numbered card that doesn't fit: small nuisance value (goes to discard or
     *    starts a costly new expedition).
     */
    private double estimateOppCardGain(Card c, ArrayList<CardsCollection> opponentPlaced) {
        int idx = getColorIndex(c.getCardColor());
        CardsCollection oppPile = opponentPlaced.get(idx);

        if (c.getCardNumber() == 0) {
            // Handshake: multiplier card — very valuable if expedition exists.
            return oppPile.isEmpty() ? 2.5 : 6.0;
        }

        if (!oppPile.isEmpty()) {
            double top = oppPile.getTopCard().getCardNumber();
            if (c.getCardNumber() >= top) {
                // Fits in opponent's expedition → direct value contribution.
                return c.getCardNumber() * 0.9;
            } else {
                // Too low to be placed; limited use.
                return c.getCardNumber() * 0.15;
            }
        } else {
            // Opponent hasn't started this expedition. Starting costs −20;
            // only worthwhile for high cards.
            return c.getCardNumber() > 6 ? c.getCardNumber() * 0.4 : -4.0;
        }
    }
}
