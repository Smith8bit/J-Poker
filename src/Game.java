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

    public void Fold(String playerId) {
        this.activePlayerIds.remove(playerId);
        System.out.println("Player " + playerId + " folds.");

        if (_isRoundOver()) {
            _progress();
        } else {
            _rotatePlayer();
        }
    }

    public void Check() {
        if (this.isCheckable) {
            this.CHECK_COUNTER++;

            if (_isRoundOver()) {
                _progress();
            } else {
                _rotatePlayer();
            }

        } else {
            System.out.println("Can't Check. Must Call, Raise or Fold");
        }
    }

    public void Bet(String playerId, int amount) {
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

    public void Call(String playerId) {
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

    public void Raise(String playerId, int amount) {
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
        // true if all current have same bet
        // true if bet is 0 and CHECK_COUNTER is equal to activePlayer size
        // true when activePlayer size = 1
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

}