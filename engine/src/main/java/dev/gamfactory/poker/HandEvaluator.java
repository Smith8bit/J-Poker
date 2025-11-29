package dev.gamfactory.poker;

import java.util.*;
import java.util.stream.Collectors;

public class HandEvaluator {

    // Hand rankings (higher = better)
    public enum HandRank {
        HIGH_CARD(0),
        PAIR(1),
        TWO_PAIR(2),
        THREE_OF_A_KIND(3),
        STRAIGHT(4),
        FLUSH(5),
        FULL_HOUSE(6),
        FOUR_OF_A_KIND(7),
        STRAIGHT_FLUSH(8),
        ROYAL_FLUSH(9);

        private final int value;

        HandRank(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    // Result of hand evaluation
    public static class HandResult implements Comparable<HandResult> {
        private HandRank rank;
        private List<Integer> kickers; // Tiebreaker values (card ranks in order of importance)
        private String playerId;
        private List<Card> bestHand; // The actual 5 cards that make the hand

        public HandResult(String playerId, HandRank rank, List<Integer> kickers) {
            this.playerId = playerId;
            this.rank = rank;
            this.kickers = kickers;
            this.bestHand = new ArrayList<>();
        }

        public HandResult(String playerId, HandRank rank, List<Integer> kickers, List<Card> bestHand) {
            this.playerId = playerId;
            this.rank = rank;
            this.kickers = kickers;
            this.bestHand = bestHand;
        }

        @Override
        public int compareTo(HandResult other) {
            // First compare hand ranks
            if (this.rank.getValue() != other.rank.getValue()) {
                return Integer.compare(this.rank.getValue(), other.rank.getValue());
            }

            // If same rank, compare kickers
            for (int i = 0; i < Math.min(this.kickers.size(), other.kickers.size()); i++) {
                if (!this.kickers.get(i).equals(other.kickers.get(i))) {
                    return Integer.compare(this.kickers.get(i), other.kickers.get(i));
                }
            }

            return 0; // Exact tie
        }

        public String getPlayerId() {
            return playerId;
        }

        public HandRank getRank() {
            return rank;
        }

        public List<Card> getBestHand() {
            return bestHand;
        }

        @Override
        public String toString() {
            return playerId + ": " + rank + " " + bestHand;
        }
    }

    /**
     * Evaluate the best 5-card hand from 7 cards (2 hole + 5 board)
     */
    public static HandResult evaluateHand(String playerId, List<Card> holeCards, List<Card> board) {
        // Combine hole cards and board
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(holeCards);
        allCards.addAll(board);

        // Find best 5-card combination from 7 cards
        HandResult bestHand = null;

        // Generate all combinations of 5 cards from 7
        List<List<Card>> combinations = generateCombinations(allCards, 5);

        for (List<Card> fiveCards : combinations) {
            HandResult result = evaluateFiveCards(playerId, fiveCards);
            if (bestHand == null || result.compareTo(bestHand) > 0) {
                bestHand = result;
            }
        }

        return bestHand;
    }

    /**
     * Evaluate exactly 5 cards
     */
    private static HandResult evaluateFiveCards(String playerId, List<Card> cards) {
        // Sort cards by rank (descending)
        List<Card> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> Integer.compare(getRankValue(b.rank()), getRankValue(a.rank())));

        // Check for each hand type (from best to worst)
        HandResult result;

        if ((result = checkRoyalFlush(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkStraightFlush(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkFourOfAKind(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkFullHouse(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkFlush(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkStraight(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkThreeOfAKind(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkTwoPair(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);
        if ((result = checkPair(sorted)) != null) return new HandResult(playerId, result.rank, result.kickers, sorted);

        return new HandResult(playerId, HandRank.HIGH_CARD, sorted.stream()
                .map(c -> getRankValue(c.rank()))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList()), sorted);
    }

    private static HandResult checkRoyalFlush(List<Card> cards) {
        HandResult sf = checkStraightFlush(cards);
        if (sf != null && sf.kickers.get(0) == 14) { // Ace-high straight flush
            return new HandResult(null, HandRank.ROYAL_FLUSH, sf.kickers);
        }
        return null;
    }

    private static HandResult checkStraightFlush(List<Card> cards) {
        if (isFlush(cards) && isStraight(cards)) {
            List<Integer> kickers = getStraightHighCard(cards);
            return new HandResult(null, HandRank.STRAIGHT_FLUSH, kickers);
        }
        return null;
    }

    private static HandResult checkFourOfAKind(List<Card> cards) {
        Map<Integer, Integer> rankCounts = getRankCounts(cards);
        Integer fourRank = null;

        // Find four of a kind (should only be one)
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 4) {
                fourRank = entry.getKey();
                break;
            }
        }

        if (fourRank != null) {
            List<Integer> kickers = new ArrayList<>();
            kickers.add(fourRank); // Four of a kind rank
            // Add highest kicker
            for (Card c : cards) {
                int rank = getRankValue(c.rank());
                if (rank != fourRank) {
                    kickers.add(rank);
                    break;
                }
            }
            return new HandResult(null, HandRank.FOUR_OF_A_KIND, kickers);
        }
        return null;
    }

    private static HandResult checkFullHouse(List<Card> cards) {
        Map<Integer, Integer> rankCounts = getRankCounts(cards);
        Integer threeRank = null;
        Integer pairRank = null;

        // Find the highest three of a kind and highest pair
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) {
                if (threeRank == null || entry.getKey() > threeRank) {
                    threeRank = entry.getKey();
                }
            } else if (entry.getValue() == 2) {
                if (pairRank == null || entry.getKey() > pairRank) {
                    pairRank = entry.getKey();
                }
            }
        }

        if (threeRank != null && pairRank != null) {
            return new HandResult(null, HandRank.FULL_HOUSE, Arrays.asList(threeRank, pairRank));
        }
        return null;
    }

    private static HandResult checkFlush(List<Card> cards) {
        if (isFlush(cards)) {
            List<Integer> kickers = cards.stream()
                    .map(c -> getRankValue(c.rank()))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            return new HandResult(null, HandRank.FLUSH, kickers);
        }
        return null;
    }

    private static HandResult checkStraight(List<Card> cards) {
        if (isStraight(cards)) {
            List<Integer> kickers = getStraightHighCard(cards);
            return new HandResult(null, HandRank.STRAIGHT, kickers);
        }
        return null;
    }

    private static HandResult checkThreeOfAKind(List<Card> cards) {
        Map<Integer, Integer> rankCounts = getRankCounts(cards);
        Integer threeRank = null;

        // Find the highest three of a kind
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 3) {
                if (threeRank == null || entry.getKey() > threeRank) {
                    threeRank = entry.getKey();
                }
            }
        }

        if (threeRank != null) {
            List<Integer> kickers = new ArrayList<>();
            List<Integer> otherCards = new ArrayList<>();

            // Collect non-trips cards
            for (Card c : cards) {
                int rank = getRankValue(c.rank());
                if (rank != threeRank) {
                    otherCards.add(rank);
                }
            }
            otherCards.sort(Comparator.reverseOrder());

            // Build kicker list: trips rank first, then top 2 kickers
            kickers.add(threeRank);
            kickers.addAll(otherCards.subList(0, Math.min(2, otherCards.size())));

            return new HandResult(null, HandRank.THREE_OF_A_KIND, kickers);
        }
        return null;
    }

    private static HandResult checkTwoPair(List<Card> cards) {
        Map<Integer, Integer> rankCounts = getRankCounts(cards);
        List<Integer> pairs = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 2) {
                pairs.add(entry.getKey());
            }
        }

        if (pairs.size() >= 2) {
            pairs.sort(Comparator.reverseOrder());
            List<Integer> kickers = new ArrayList<>();
            kickers.add(pairs.get(0)); // High pair
            kickers.add(pairs.get(1)); // Low pair

            // Add kicker
            for (Card c : cards) {
                int rank = getRankValue(c.rank());
                if (!pairs.contains(rank)) {
                    kickers.add(rank);
                    break;
                }
            }
            return new HandResult(null, HandRank.TWO_PAIR, kickers);
        }
        return null;
    }

    private static HandResult checkPair(List<Card> cards) {
        Map<Integer, Integer> rankCounts = getRankCounts(cards);
        Integer pairRank = null;

        // Find the highest pair
        for (Map.Entry<Integer, Integer> entry : rankCounts.entrySet()) {
            if (entry.getValue() == 2) {
                if (pairRank == null || entry.getKey() > pairRank) {
                    pairRank = entry.getKey();
                }
            }
        }

        if (pairRank != null) {
            List<Integer> kickers = new ArrayList<>();
            List<Integer> otherCards = new ArrayList<>();

            // Collect non-pair cards
            for (Card c : cards) {
                int rank = getRankValue(c.rank());
                if (rank != pairRank) {
                    otherCards.add(rank);
                }
            }
            otherCards.sort(Comparator.reverseOrder());

            // Build kicker list: pair rank first, then top 3 kickers
            kickers.add(pairRank);
            kickers.addAll(otherCards.subList(0, Math.min(3, otherCards.size())));

            return new HandResult(null, HandRank.PAIR, kickers);
        }
        return null;
    }

    private static HandResult checkHighCard(String playerId, List<Card> cards) {
        List<Integer> kickers = cards.stream()
                .map(c -> getRankValue(c.rank()))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
        return new HandResult(playerId, HandRank.HIGH_CARD, kickers);
    }

    // Helper methods

    private static boolean isFlush(List<Card> cards) {
        String firstSuit = cards.get(0).suit();
        return cards.stream().allMatch(c -> c.suit().equals(firstSuit));
    }

    private static boolean isStraight(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(c -> getRankValue(c.rank()))
                .sorted()
                .collect(Collectors.toList());

        // Check regular straight
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i + 1) - ranks.get(i) != 1) {
                // Check for wheel (A-2-3-4-5)
                if (ranks.equals(Arrays.asList(2, 3, 4, 5, 14))) {
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private static List<Integer> getStraightHighCard(List<Card> cards) {
        List<Integer> ranks = cards.stream()
                .map(c -> getRankValue(c.rank()))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        // Check for wheel (A-2-3-4-5) - Ace counts as 1
        if (ranks.equals(Arrays.asList(14, 5, 4, 3, 2))) {
            return Arrays.asList(5); // Wheel is 5-high
        }

        return Arrays.asList(ranks.get(0)); // Highest card
    }

    private static Map<Integer, Integer> getRankCounts(List<Card> cards) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Card c : cards) {
            int rank = getRankValue(c.rank());
            counts.put(rank, counts.getOrDefault(rank, 0) + 1);
        }
        return counts;
    }

    private static int getRankValue(String rank) {
        return switch (rank) {
            case "2" -> 2;
            case "3" -> 3;
            case "4" -> 4;
            case "5" -> 5;
            case "6" -> 6;
            case "7" -> 7;
            case "8" -> 8;
            case "9" -> 9;
            case "10" -> 10;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            case "A" -> 14;
            default -> 0;
        };
    }

    /**
     * Generate all combinations of k elements from list
     */
    private static <T> List<List<T>> generateCombinations(List<T> list, int k) {
        List<List<T>> combinations = new ArrayList<>();
        generateCombinationsHelper(list, k, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private static <T> void generateCombinationsHelper(List<T> list, int k, int start,
                                                       List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }

        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            generateCombinationsHelper(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    /**
     * Find winner(s) from multiple players
     */
    public static List<String> findWinners(Map<String, HandResult> playerHands) {
        if (playerHands.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the best hand
        HandResult bestHand = null;
        for (HandResult result : playerHands.values()) {
            if (bestHand == null || result.compareTo(bestHand) > 0) {
                bestHand = result;
            }
        }

        // Find all players with the best hand (for split pots)
        List<String> winners = new ArrayList<>();
        for (Map.Entry<String, HandResult> entry : playerHands.entrySet()) {
            if (entry.getValue().compareTo(bestHand) == 0) {
                winners.add(entry.getKey());
            }
        }

        return winners;
    }
}