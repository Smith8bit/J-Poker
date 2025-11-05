import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.*;
import java.util.stream.Collectors;

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
    public int currentActorPos;

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
        this.playerBets.put(bbPlayerId, bigBlind);

        // DEAL
        deck.shuffle();
        for (int i = 0; i < 2; i++) {
            for (String id : this.playerOrder) {
                this.players.get(id).hand.add(this.deck.deal());
            }
        }

        this.currentActorPos = 2 % this.playerOrder.size();
    }

    public void fold(String playerId) {
        this.activePlayerIds.remove(playerId);
        System.out.println("Player " + playerId + " folds.");

        if (this.activePlayerIds.size() <= 1) {
            _handOver();
            return;
        }
        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void check() {
        if (this.isCheckable) {
            this.CHECK_COUNTER++;
            System.out.println("Player " + this.currentActorPos + " check");

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
        this.isCheckable = false;
        this.pot += _postBet(playerId, amount);
        this.currentBet = this.playerBets.get(playerId); // New current bet
        System.out.println("Player " + playerId + " bets " + amount);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void call(String playerId) {
        this.isCheckable = false;
        int callAmount = this.currentBet - this.playerBets.get(playerId);
        this.pot += _postBet(playerId, callAmount);
        System.out.println("Player " + playerId + " calls " + callAmount);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void raise(String playerId, int amount) {
        this.isCheckable = false;
        this.pot += _postBet(playerId, amount);
        this.currentBet = this.playerBets.get(playerId); // New current bet
        System.out.println("Player " + playerId + " raise to " + amount);

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    private void _rotatePlayer() {
        this.currentActorPos = (this.currentActorPos + 1) % this.playerOrder.size();
    }

    private boolean _isRoundOver() {
        // all call
        if (allValuesEqual(this.playerBets) && this.playerBets.values().iterator().next() != 0) {
            return true;
        }

        // all check
        if (this.playerBets.containsValue(0) && allValuesEqual(this.playerBets) && CHECK_COUNTER == activePlayerIds.size()) {
            return true;
        }
        return false;
    }

    public void _progress() {
        // Move to the next street
        // Use in FLOP, TURN, RIVER, SHOWDOWN
        this.street = Street.values()[this.street.ordinal()+1];

        this.currentBet = 0;
        this.isCheckable = true;
        this.CHECK_COUNTER = 0;
        for (String id : this.playerBets.keySet()) {
            this.playerBets.put(id, 0);
        }

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
        }
    }

    private int _postBet(String playerId, int amount) {
        // Bet in front of player
        Player player = this.players.get(playerId);

        // Check if player is going all-in
        if (amount >= player.stack) {
            // Player goes all-in with whatever they have left
            int allInAmount = player.stack;
            player.stack = 0;
            this.playerBets.put(playerId, this.playerBets.get(playerId) + allInAmount);
            System.out.println("Player " + playerId + " is ALL-IN for " + allInAmount);
            return allInAmount;
        }

        // Normal bet/raise
        player.stack -= amount;
        this.playerBets.put(playerId, this.playerBets.get(playerId) + amount);
        return amount;
    }

    private void _doShowdown () {

    }

    private void _handOver() {
        if (this.activePlayerIds.size() == 1) {
            String winnerId = this.activePlayerIds.get(0);
            System.out.println("Player " + winnerId + " wins " + this.pot + " (everyone folded)");
            this.players.get(winnerId).stack += this.pot;
        }

        System.out.println("--- HAND OVER ---");
    }

    private  <K, V> boolean allValuesEqual(Map<K, V> map) {
        Set<V> uniqueValues = new HashSet<>(map.values());
        return uniqueValues.size() <= 1;
    }

}