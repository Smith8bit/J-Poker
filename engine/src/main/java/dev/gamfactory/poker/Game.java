package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Game {
    private Map<String, Player> players;
    public List<String> playerOrder;
    private Deck deck;
    private List<Card> board;
    private int pot;
    private Street street;
    private int bigBlind;
    private int smallBlind;
    private boolean isCheckable = true;
    private int CHECK_COUNTER;

    private int currentBet;
    public Map<String, Integer> playerBets;
    public List<String> activePlayerIds;
    public int currentActorPos; // Index into activePlayerIds

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
        this.CHECK_COUNTER = 0;
    }

    public void start() {
        // set up table
        this.deck = new Deck();
        this.board.clear();
        this.pot = 0;
        this.street = Street.PREFLOP;

        this.activePlayerIds = new ArrayList<>(this.playerOrder);
        this.playerBets = new HashMap<>();
        for (String id : this.playerOrder) {
            this.playerBets.put(id, 0);
        }

        // BLINDS
        String sbPlayerId = this.playerOrder.get(0);
        String bbPlayerId = this.playerOrder.get(1);

        this.pot += _postBet(sbPlayerId, this.smallBlind);
        this.pot += _postBet(bbPlayerId, this.bigBlind);
        this.currentBet = this.bigBlind;

        // DEAL
        deck.shuffle();
        for (int i = 0; i < 2; i++) {
            for (String id : this.playerOrder) {
                this.players.get(id).hand.add(this.deck.deal());
            }
        }

        // First to act is player after big blind
        String firstToActId = this.playerOrder.get(2 % this.playerOrder.size());
        this.currentActorPos = this.activePlayerIds.indexOf(firstToActId);
    }

    public void fold(String playerId) {
        validateAction(playerId);

        this.activePlayerIds.remove(playerId);
        System.out.println("dev.gamfactory.poker.Player " + playerId + " folds.");

        if (this.activePlayerIds.size() <= 1) {
            _handOver();
            return;
        }

        // Adjust currentActorPos if needed (if player after current folded)
        if (this.currentActorPos >= this.activePlayerIds.size()) {
            this.currentActorPos = 0;
        }

        if (_isRoundOver()) {
            _progress();
        }
    }

    public void check() {
        String currentPlayer = getCurrentPlayerId();
        validateAction(currentPlayer);

        if (this.isCheckable) {
            this.CHECK_COUNTER++;
            System.out.println("dev.gamfactory.poker.Player " + currentPlayer + " checks");

            if (_isRoundOver()) {
                _progress();
            } else {
                _rotatePlayer();
            }
        } else {
            System.out.println("Can't check. Must call, raise or fold");
        }
    }

    public void bet(String playerId, int amount) {
        validateAction(playerId);

        this.isCheckable = false;
        this.pot += _postBet(playerId, amount);
        this.currentBet = this.playerBets.get(playerId);
        System.out.println("dev.gamfactory.poker.Player " + playerId + " bets " + amount);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void call(String playerId) {
        validateAction(playerId);

        this.isCheckable = false;
        int callAmount = this.currentBet - this.playerBets.get(playerId);
        this.pot += _postBet(playerId, callAmount);
        System.out.println("dev.gamfactory.poker.Player " + playerId + " calls " + callAmount);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void raise(String playerId, int amount) {
        validateAction(playerId);

        this.isCheckable = false;
        int totalAmount = this.currentBet + amount;
        this.pot += _postBet(playerId, totalAmount - this.playerBets.get(playerId));
        this.currentBet = this.playerBets.get(playerId);
        System.out.println("dev.gamfactory.poker.Player " + playerId + " raises to " + this.currentBet);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    private void _rotatePlayer() {
        this.currentActorPos = (this.currentActorPos + 1) % this.activePlayerIds.size();
    }

    private boolean _isRoundOver() {
        // Get only active players' bets
        Map<String, Integer> activeBets = new HashMap<>();
        for (String id : activePlayerIds) {
            activeBets.put(id, playerBets.get(id));
        }

        // Check if all active players have matched the bet
        if (allValuesEqual(activeBets) && !activeBets.isEmpty()) {
            int betValue = activeBets.values().iterator().next();
            if (betValue != 0) return true; // All called
            if (betValue == 0 && CHECK_COUNTER == activePlayerIds.size()) return true; // All checked
        }
        return false;
    }

    public void _progress() {
        // Move to the next street
        this.street = Street.values()[this.street.ordinal() + 1];

        this.currentBet = 0;
        this.isCheckable = true;
        this.CHECK_COUNTER = 0;
        for (String id : this.playerBets.keySet()) {
            this.playerBets.put(id, 0);
        }

        // Reset position to first active player
        this.currentActorPos = 0;

        switch (this.street) {
            case FLOP:
                this.board = this.deck.deal(3);
                System.out.println("--- FLOP ---: " + this.board);
                break;
            case TURN:
                this.board.add(this.deck.deal());
                System.out.println("--- TURN ---: " + this.board);
                break;
            case RIVER:
                this.board.add(this.deck.deal());
                System.out.println("--- RIVER ---: " + this.board);
                break;
            case SHOWDOWN:
                _doShowdown();
                break;
            default:
                break;
        }
    }

    private int _postBet(String playerId, int amount) {
        Player player = this.players.get(playerId);

        // Check if player is going all-in
        if (amount >= player.stack) {
            int allInAmount = player.stack;
            player.stack = 0;
            this.playerBets.put(playerId, this.playerBets.get(playerId) + allInAmount);
            System.out.println("dev.gamfactory.poker.Player " + playerId + " is ALL-IN for " + allInAmount);
            return allInAmount;
        }

        // Normal bet/raise
        player.stack -= amount;
        this.playerBets.put(playerId, this.playerBets.get(playerId) + amount);
        return amount;
    }

    private void _doShowdown() {
        System.out.println("\n=== SHOWDOWN ===");
        System.out.println("Board: " + this.board);
        System.out.println();

        // Evaluate each player's hand
        Map<String, HandEvaluator.HandResult> playerHands = new HashMap<>();
        for (String playerId : this.activePlayerIds) {
            Player player = this.players.get(playerId);
            HandEvaluator.HandResult result = HandEvaluator.evaluateHand(
                    playerId,
                    player.hand,
                    this.board
            );
            playerHands.put(playerId, result);

            // Show each player's hole cards and best hand
            System.out.println("dev.gamfactory.poker.Player " + playerId + " shows: " + player.hand);
            System.out.println("  → " + result.getRank() + ": " + result.getBestHand());
            System.out.println();
        }

        // Find winner(s)
        List<String> winners = HandEvaluator.findWinners(playerHands);

        System.out.println("───────────────────────");
        if (winners.size() == 1) {
            String winnerId = winners.get(0);
            HandEvaluator.HandResult winningHand = playerHands.get(winnerId);
            System.out.println("🏆 dev.gamfactory.poker.Player " + winnerId + " wins " + this.pot + " with " + winningHand.getRank() + "!");
            this.players.get(winnerId).stack += this.pot;
        } else {
            // Split pot
            int splitAmount = this.pot / winners.size();
            HandEvaluator.HandResult winningHand = playerHands.get(winners.get(0));
            System.out.println("🤝 Split pot (" + splitAmount + " each) - " + winningHand.getRank());
            System.out.println("Winners: " + winners);
            for (String winnerId : winners) {
                this.players.get(winnerId).stack += splitAmount;
            }
        }

        _handOver();
    }

    private void _handOver() {
        if (this.activePlayerIds.size() == 1) {
            String winnerId = this.activePlayerIds.get(0);
            System.out.println("dev.gamfactory.poker.Player " + winnerId + " wins " + this.pot + " (everyone folded)");
            this.players.get(winnerId).stack += this.pot;
        }

        System.out.println("--- HAND OVER ---");

        // Clear hands for next hand
        for (Player player : this.players.values()) {
            player.hand.clear();
        }
    }

    private String getCurrentPlayerId() {
        return this.activePlayerIds.get(this.currentActorPos);
    }

    private void validateAction(String playerId) {
        if (!activePlayerIds.contains(playerId)) {
            throw new IllegalStateException("dev.gamfactory.poker.Player " + playerId + " is not active");
        }
        if (!getCurrentPlayerId().equals(playerId)) {
            throw new IllegalStateException("Not player " + playerId + "'s turn (current: " + getCurrentPlayerId() + ")");
        }
    }

    private <K, V> boolean allValuesEqual(Map<K, V> map) {
        Set<V> uniqueValues = new HashSet<>(map.values());
        return uniqueValues.size() <= 1;
    }
}