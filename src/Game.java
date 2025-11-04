import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private Map<String, Player> players;
    private List<String> playerOrder;
    private Deck deck;
    private List<Card> board;
    private int pot;
    private Street street;
    private int bigBlind;
    private int smallBlind;

    private int currentBet;
    private Map<String, Integer> playerBets;
    private List<String> activePlayerIds;
    private int currentActorPos;

    public Game(List<Player> initialPlayers, int bigBlind) {
        this.players = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        for (Player p : initialPlayers) {
            this.players.put(p.id, p);
            this.playerOrder.add(p.id);
        }

        this.bigBlind = bigBlind;
        this.smallBlind = bigBlind / 2;
        this.deck = new Deck();
        this.board = new ArrayList<>();
    }

    public void startHand() {
        this.deck = new Deck();
        this.board.clear();
        this.pot = 0;
        this.street = Street.PREFLOP;

        this.activePlayerIds = new ArrayList<>(this.playerOrder);
        this.playerBets = new HashMap<>();
        for (String id : this.playerOrder) {
            this.playerBets.put(id, 0);
        }

        // --- 1. Post Blinds (Simplified) ---
        // (In a real game, you'd find players at SB/BB positions)
        String sbPlayerId = this.playerOrder.get(0);
        String bbPlayerId = this.playerOrder.get(1);

        this.pot += _postBet(sbPlayerId, this.smallBlind);
        this.pot += _postBet(bbPlayerId, this.bigBlind);
        this.currentBet = this.bigBlind;

        // --- 2. Deal Hands ---
        for (String id : this.playerOrder) {
            this.players.get(id).hand = this.deck.deal(2);
        }

        // --- 3. Set First Actor ---
        // (First to act is "Under the Gun", simplified to player after BB)
        this.currentActorPos = 2 % this.playerOrder.size();

        System.out.println("--- Hand Starting ---");
        System.out.println("Pot: " + this.pot);
        // You would serialize and send this state to players
    }

    public void handleAction(String playerId, String action, int amount) {
        // (Add checks: is it this player's turn? Is the action valid?)

        switch (action) {
            case "fold":
                this.activePlayerIds.remove(playerId);
                System.out.println("Player " + playerId + " folds.");
                break;
            case "call":
                int callAmount = this.currentBet - this.playerBets.get(playerId);
                this.pot += _postBet(playerId, callAmount);
                System.out.println("Player " + playerId + " calls " + callAmount);
                break;
            case "raise":
                // (Add checks: is raise valid size? min 2x current bet, etc.)
                this.pot += _postBet(playerId, amount);
                this.currentBet = this.playerBets.get(playerId); // New current bet
                System.out.println("Player " + playerId + " raises to " + amount);
                break;
        }

        // --- Advance to next player ---
        // (This is a simplified advancement)
        this.currentActorPos = (this.currentActorPos + 1) % this.playerOrder.size();

        // --- Check if betting round is over ---
        if (_isBettingRoundOver()) {
            _progressStreet();
        }
    }

    private int _postBet(String playerId, int amount) {
        Player player = this.players.get(playerId);
        // (Add check for all-in)
        player.stack -= amount;
        this.playerBets.put(playerId, this.playerBets.get(playerId) + amount);
        return amount;
    }

    private boolean _isBettingRoundOver() {
        // Placeholder: Checks if all active players have matched
        // the current bet or folded.
        // (Real logic is complex. This is a simple stub.)
        return false; // We will call _progressStreet manually for example.
    }

    public void _progressStreet() {
        // Move to the next street
        this.street = Street.values()[this.street.ordinal() + 1];

        this.currentBet = 0; // Reset for next betting round
        for (String id : this.playerBets.keySet()) {
            this.playerBets.put(id, 0);
        }
        // (Reset current_actor_pos to first active player after button)

        switch (this.street) {
            case FLOP:
                this.deck.deal(); // Burn card
                this.board = this.deck.deal(3);
                System.out.println("--- FLOP ---: " + this.board);
                break;
            case TURN:
                this.deck.deal(); // Burn card
                this.board.add(this.deck.deal());
                System.out.println("--- TURN ---: " + this.board);
                break;
            case RIVER:
                this.deck.deal(); // Burn card
                this.board.add(this.deck.deal());
                System.out.println("--- RIVER ---: " + this.board);
                break;
            case SHOWDOWN:
                _doShowdown();
                break;
        }
    }

    private void _doShowdown() {
        System.out.println("--- SHOWDOWN ---");
        HandStrength bestHand = null;
        String winnerId = null;

        for (String playerId : this.activePlayerIds) {
            Player player = this.players.get(playerId);
            HandStrength handStrength = _evaluateHand(player.hand, this.board);

            System.out.println("Player " + playerId + " has: " + handStrength);

            if (bestHand == null || handStrength.compareTo(bestHand) > 0) {
                bestHand = handStrength;
                winnerId = playerId;
            }
        }

        System.out.println("Winner is " + winnerId + " with " + bestHand);
        _awardPot(winnerId);
    }

    private void _awardPot(String winnerId) {
        // (In a real game, this handles side-pots. This is simplified.)
        Player winner = this.players.get(winnerId);
        winner.stack += this.pot;
        System.out.println("Awarding " + this.pot + " to " + winnerId + ". New stack: " + winner.stack);
        this.pot = 0;
    }

    private HandStrength _evaluateHand(List<Card> playerHand, List<Card> board) {
        // Combine player hand and board cards
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(playerHand);
        allCards.addAll(board);

        // Generate all 5-card combinations from 7 cards
        List<List<Card>> combinations = _getCombinations(allCards, 5);

        HandStrength best = null;
        for (List<Card> combo : combinations) {
            HandStrength strength = _evaluate5Cards(combo);
            if (best == null || strength.compareTo(best) > 0) {
                best = strength;
            }
        }

        return best;
    }

    private List<List<Card>> _getCombinations(List<Card> cards, int k) {
        List<List<Card>> result = new ArrayList<>();
        _getCombinationsHelper(cards, k, 0, new ArrayList<>(), result);
        return result;
    }

    private void _getCombinationsHelper(List<Card> cards, int k, int start,
                                        List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            _getCombinationsHelper(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    private HandStrength _evaluate5Cards(List<Card> cards) {
        // Sort cards by rank (descending)
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> _getRankValue(b.rank()) - _getRankValue(a.rank()));

        boolean flush = _isFlush(sorted);
        boolean straight = _isStraight(sorted);
        Map<Integer, Integer> rankCounts = _getRankCounts(sorted);

        // Check for straight flush
        if (flush && straight) {
            int highCard = _getRankValue(sorted.get(0).rank());
            // Special case: A-2-3-4-5 (wheel)
            if (highCard == 14 && _getRankValue(sorted.get(1).rank()) == 5) {
                return new HandStrength(8, Arrays.asList(5)); // Straight flush to 5
            }
            return new HandStrength(8, Arrays.asList(highCard)); // Straight flush
        }

        // Check for four of a kind
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 4) {
                int quadRank = entry.getKey();
                int kicker = rankCounts.keySet().stream()
                        .filter(r -> r != quadRank)
                        .findFirst().orElse(0);
                return new HandStrength(7, Arrays.asList(quadRank, kicker));
            }
        }

        // Check for full house
        Integer trips = null;
        Integer pair = null;
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) {
                if (trips == null || entry.getKey() > trips) {
                    // If we already had trips, demote it to a pair
                    if (trips != null) {
                        pair = trips;
                    }
                    trips = entry.getKey();
                } else {
                    // Second set of trips becomes the pair
                    pair = entry.getKey();
                }
            } else if (entry.getValue() == 2) {
                if (pair == null || entry.getKey() > pair) {
                    pair = entry.getKey();
                }
            }
        }
        if (trips != null && pair != null) {
            return new HandStrength(6, Arrays.asList(trips, pair));
        }

        // Check for flush
        if (flush) {
            List<Integer> kickers = sorted.stream()
                    .map(c -> _getRankValue(c.rank()))
                    .collect(Collectors.toList());
            return new HandStrength(5, kickers);
        }

        // Check for straight
        if (straight) {
            int highCard = _getRankValue(sorted.get(0).rank());
            // Special case: A-2-3-4-5 (wheel)
            if (highCard == 14 && _getRankValue(sorted.get(1).rank()) == 5) {
                return new HandStrength(4, Arrays.asList(5));
            }
            return new HandStrength(4, Arrays.asList(highCard));
        }

        // Check for three of a kind
        if (trips != null) {
            final Integer tripsRank = trips;
            List<Integer> kickers = rankCounts.keySet().stream()
                    .filter(r -> r != tripsRank)
                    .sorted(Comparator.reverseOrder())
                    .limit(2)
                    .collect(Collectors.toList());
            List<Integer> result = new ArrayList<>();
            result.add(tripsRank);
            result.addAll(kickers);
            return new HandStrength(3, result);
        }

        // Check for two pair
        List<Integer> pairs = rankCounts.entrySet().stream()
                .filter(e -> e.getValue() == 2)
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (pairs.size() >= 2) {
            int highPair = pairs.get(0);
            int lowPair = pairs.get(1);
            int kicker = rankCounts.keySet().stream()
                    .filter(r -> r != highPair && r != lowPair)
                    .max(Integer::compareTo)
                    .orElse(0);
            return new HandStrength(2, Arrays.asList(highPair, lowPair, kicker));
        }

        // Check for one pair
        if (pairs.size() == 1) {
            int pairRank = pairs.get(0);
            List<Integer> kickers = rankCounts.keySet().stream()
                    .filter(r -> r != pairRank)
                    .sorted(Comparator.reverseOrder())
                    .limit(3)
                    .collect(Collectors.toList());
            List<Integer> result = new ArrayList<>();
            result.add(pairRank);
            result.addAll(kickers);
            return new HandStrength(1, result);
        }

        // High card
        List<Integer> kickers = sorted.stream()
                .map(c -> _getRankValue(c.rank()))
                .collect(Collectors.toList());
        return new HandStrength(0, kickers);
    }

    private int _getRankValue(String rank) {
        switch (rank) {
            case "A": return 14;
            case "K": return 13;
            case "Q": return 12;
            case "J": return 11;
            default: return Integer.parseInt(rank);
        }
    }

    private boolean _isFlush(List<Card> cards) {
        String suit = cards.get(0).suit();
        return cards.stream().allMatch(c -> c.suit().equals(suit));
    }

    private boolean _isStraight(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(c -> _getRankValue(c.rank()))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        // Check for regular straight
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i) - ranks.get(i + 1) != 1) {
                // Check for wheel (A-2-3-4-5)
                if (ranks.get(0) == 14 && ranks.get(1) == 5 &&
                        ranks.get(2) == 4 && ranks.get(3) == 3 && ranks.get(4) == 2) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private Map<Integer, Integer> _getRankCounts(List<Card> cards) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card card : cards) {
            int rankValue = _getRankValue(card.rank());
            counts.put(rankValue, counts.getOrDefault(rankValue, 0) + 1);
        }
        return counts;
    }
}

record HandStrength(int rank, List<Integer> kickers) implements Comparable<HandStrength> {
    // This allows us to compare hands easily
    @Override
    public int compareTo(HandStrength other) {
        if (this.rank != other.rank) {
            return Integer.compare(this.rank, other.rank);
        }
        for (int i = 0; i < this.kickers.size(); i++) {
            if (!this.kickers.get(i).equals(other.kickers.get(i))) {
                return Integer.compare(this.kickers.get(i), other.kickers.get(i));
            }
        }
        return 0; // It's a tie
    }


}
