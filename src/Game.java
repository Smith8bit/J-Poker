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

    private int currentBet;
    private Map<String, Integer> playerBets;
    private List<String> activePlayerIds;
    private int currentActorPos;
    public boolean checkable = true;

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

        this.currentActorPos = 2 % this.playerOrder.size();
    }

    public void handleAction(String playerId, String action, int amount) {
        switch (action) {
            case "Fold" :
                this.activePlayerIds.remove(playerId);
                break;
            case "Check":
                this.currentActorPos = (this.currentActorPos + 1) % this.playerOrder.size();;
                checkable = true;
                break;
            case "Bet" :
                ;
                checkable = false;
                break;
            case "Call" :
                ;
                checkable = false;
                break;
            case "Raise":
                ;
                checkable = false;
                break;
        }

        this.currentActorPos = (this.currentActorPos + 1) % this.playerOrder.size();
    }

    public void _progress() {
        // Move to the next street
        // Use in FLOP, TURN, RIVER, SHOWDOWN
        this.street = Street.values()[this.street.ordinal()+1];

        this.currentBet = 0;
        for (String id : this.playerBets.keySet()) {
            this.playerBets.put(id, 0);
        }

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

    private void _evaluateHand() {

    }
}