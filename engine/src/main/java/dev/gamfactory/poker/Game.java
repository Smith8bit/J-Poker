package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class Game {
    public Map<String, Player> players;
    public List<String> playerOrder;
    public Deck deck;
    public List<Card> board;
    public int pot;
    public Street street;
    public int bigBlind;
    public int smallBlind;
    
    public int currentBet;
    public Map<String, Integer> playerBets;
    public List<String> activePlayerIds;
    public int currentActorPos;

    public Set<String> playersActed; 
    public Map<String, Object> gameOverData = null;

    public Game() {
        this.players = new HashMap<>();
        this.playerOrder = new ArrayList<>();
        this.board = new ArrayList<>();
        this.activePlayerIds = new ArrayList<>();
        this.playerBets = new HashMap<>();
        this.playersActed = new HashSet<>();
        this.deck = new Deck();
    }

    public Game(List<Player> initialPlayers, int bigBlind) {
        this();
        this.bigBlind = bigBlind;
        this.smallBlind = bigBlind / 2;
        
        for (Player p : initialPlayers) {
            this.players.put(p.getId(), p); 
            this.playerOrder.add(p.getId());
        }
    }

    public void start() {
        this.deck = new Deck();
        this.deck.shuffle();
        this.board.clear();
        this.pot = 0;
        this.street = Street.PREFLOP;
        
        this.playersActed.clear();
        this.activePlayerIds = new ArrayList<>(this.playerOrder);
        this.playerBets = new HashMap<>();
        
        for (String id : this.playerOrder) {
            this.playerBets.put(id, 0);
        }

        // --- BLINDS ---
        if (this.playerOrder.size() >= 2) {
            String sbPlayerId = this.playerOrder.get(0);
            String bbPlayerId = this.playerOrder.get(1);

            this.pot += _postBet(sbPlayerId, this.smallBlind);
            this.pot += _postBet(bbPlayerId, this.bigBlind);
            this.currentBet = this.bigBlind;
        }

        // --- DEAL ---
        for (int i = 0; i < 2; i++) {
            for (String id : this.playerOrder) {
                Player p = this.players.get(id);
                if (p.getHand() == null) p.setHand(new ArrayList<>());
                p.addCard(this.deck.deal());
            }
        }

        // คนเริ่มเล่นคือคนถัดจาก BB
        int startPos = (this.playerOrder.size() >= 3) ? 2 : 0;
        this.currentActorPos = startPos % this.activePlayerIds.size();
    }

    // --- ACTIONS ---

    public void fold(String playerId) {
        validateAction(playerId);
        this.activePlayerIds.remove(playerId);
        
        System.out.println("Player " + playerId + " folds.");

        if (this.activePlayerIds.size() <= 1) {
            _handOver();
            return;
        }
        
        if (this.currentActorPos >= this.activePlayerIds.size()) {
            this.currentActorPos = 0;
        }

        if (_isRoundOver()) { _progress(); }
    }

    public void check() {
        String playerId = getCurrentPlayerId();
        validateAction(playerId);
        
        // เช็คว่าเงินถึงหรือยัง ถ้าถึงแล้วให้ Check ได้เลย
        int myBet = this.playerBets.getOrDefault(playerId, 0);
        if (myBet < this.currentBet) {
            System.out.println("Cannot Check! You must Call " + (this.currentBet - myBet));
            return; // หรือ throw Exception
        }

        this.playersActed.add(playerId); // บันทึกว่าเล่นแล้ว
        
        System.out.println("Player " + playerId + " checks");
        if (_isRoundOver()) { _progress(); } else { _rotatePlayer(); }
    }

    public void call(String playerId) {
        validateAction(playerId);
        
        this.playersActed.add(playerId);

        int currentContribution = this.playerBets.getOrDefault(playerId, 0);
        int callAmount = this.currentBet - currentContribution;
        
        // ถ้า callAmount เป็น 0 ก็คือการ Check นั่นแหละ
        int posted = _postBet(playerId, callAmount);
        this.pot += posted;

        System.out.println("Player " + playerId + " calls/checks " + posted);
        if (_isRoundOver()) { _progress(); } else { _rotatePlayer(); }
    }

    public void bet(String playerId, int amount) {
        // Bet ถือเป็นการตั้งยอดใหม่
        raise(playerId, amount);
    }

    public void raise(String playerId, int amount) {
        validateAction(playerId);
        
        // Raise ต้องรีเซ็ตให้คนอื่นตัดสินใจใหม่
        this.playersActed.clear();
        this.playersActed.add(playerId);

        if (amount <= this.currentBet) amount = this.currentBet * 2; 
        this.currentBet = amount;
        
        int currentContribution = this.playerBets.getOrDefault(playerId, 0);
        int diff = amount - currentContribution;

        int posted = _postBet(playerId, diff);
        this.pot += posted;

        System.out.println("Player " + playerId + " raises to " + this.currentBet);
        if (_isRoundOver()) { _progress(); } else { _rotatePlayer(); }
    }

    // --- INTERNAL LOGIC ---

    private void _rotatePlayer() {
        this.currentActorPos = (this.currentActorPos + 1) % this.activePlayerIds.size();
    }

    private boolean _isRoundOver() {
        if (activePlayerIds.size() <= 1) return true;

        //ทุกคนต้อง Action แล้ว
        for (String id : activePlayerIds) {
            Player p = players.get(id);
            if (p.getStack() == 0) continue; // ข้ามคน All-in

            if (!playersActed.contains(id)) return false;
        }

        //เงินต้องเท่ากัน
        for (String id : activePlayerIds) {
            Player p = players.get(id);
            if (p.getStack() == 0) continue; 

            int bet = playerBets.getOrDefault(id, 0);
            if (bet != currentBet) return false;
        }

        return true;
    }

    public void _progress() {
        if (this.street == Street.SHOWDOWN) {
            _doShowdown();
            return;
        }
        //เลื่อน Street
        if (this.street == Street.RIVER) {
            this.street = Street.SHOWDOWN;
        } else {
            this.street = Street.values()[this.street.ordinal() + 1];
        }

        // รีเซ็ตค่ารอบใหม่
        this.currentBet = 0;
        this.playersActed.clear(); 
        
        for (String id : this.playerBets.keySet()) {
            this.playerBets.put(id, 0);
        }

        this.currentActorPos = 0; 

        // เปิดไพ่
        if (this.street == Street.FLOP) {
            this.board.addAll(this.deck.deal(3));
        } else if (this.street == Street.TURN || this.street == Street.RIVER) {
            this.board.add(this.deck.deal());
        } else if (this.street == Street.SHOWDOWN) {
            _doShowdown();
            return;
        }

        if (this.street == Street.SHOWDOWN) {
            _doShowdown();
            return;
        }
        
        System.out.println("--- " + this.street + " --- Board: " + this.board);
    }

    private int _postBet(String playerId, int amount) {
        Player player = this.players.get(playerId);
        if (amount > player.getStack()) amount = player.getStack(); // All-in logic
        
        player.setStack(player.getStack() - amount);
        System.out.println(player.getUsername()+" stack: "+player.getStack());
        this.playerBets.put(playerId, this.playerBets.get(playerId) + amount);
        return amount;
    }

    public String getCurrentPlayerId() {
        if(activePlayerIds.isEmpty()) return "";
        return this.activePlayerIds.get(this.currentActorPos);
    }
    
    private void validateAction(String playerId) {
        if (!activePlayerIds.contains(playerId)) throw new IllegalStateException("Player not active");
        if (!getCurrentPlayerId().equals(playerId)) throw new IllegalStateException("Not your turn");
    }
    
    // Getters
    public int getPot() { return pot; }
    public List<Card> getBoard() { return board; }
    public int getCurrentBet() { return currentBet; }
    
    private void _doShowdown() {
        System.out.println("\n=== SHOWDOWN ===");
        System.out.println("Board: " + this.board);
        System.out.println();

        // คำนวณไพ่ของผู้เล่นทุกคนที่ยังเหลืออยู่
        Map<String, HandEvaluator.HandResult> playerHands = new HashMap<>();
        List<Map<String, Object>> winnersInfo = new ArrayList<>();

        for (String playerId : this.activePlayerIds) {
            Player player = this.players.get(playerId);
            
            List<Card> hand = player.getHand(); 
            
            try {
                HandEvaluator.HandResult result = HandEvaluator.evaluateHand(
                        playerId,
                        hand,
                        this.board
                );
                playerHands.put(playerId, result);

                System.out.println("Player " + player.getUsername() + " shows: " + hand);
                System.out.println("  -> " + result.getRank() + ": " + result.getBestHand());
            } catch (Exception e) {
                System.out.println("Error evaluating hand for " + player.getUsername() + ": " + e.getMessage());
            }
            System.out.println();
        }

        //หาผู้ชนะ
        List<String> winners = HandEvaluator.findWinners(playerHands);

        System.out.println("───────────────────────");
        
        if (winners.isEmpty()) {
             // กันเหนียว กรณีไม่มีใครชนะ
             System.out.println("No winners calculated. Pot remains.");
             _handOver();
             return;
        }

        // 3. แจกเงินรางวัล
        if (winners.size() == 1) {
            // --- ชนะคนเดียว ---
            String winnerId = winners.get(0);
            HandEvaluator.HandResult winningHand = playerHands.get(winnerId);
            Player winner = this.players.get(winnerId);
            
            System.out.println(" Player " + winner.getUsername() + 
                               " WINS " + this.pot + " with " + winningHand.getRank() + "!");
            
            //อัปเดตเงินผู้ชนะ (ใช้ setter)
            winner.setStack(winner.getStack() + this.pot);
            winnersInfo.add(Map.of(
                "username", winner.getUsername(),
                "amount", this.pot,
                "handRank", playerHands.get(winnerId).getRank().toString()
            ));
            
        } else {
            // --- เสมอ (Split Pot) ---
            int splitAmount = this.pot / winners.size();
            
            HandEvaluator.HandResult winningHand = playerHands.get(winners.get(0));
            System.out.println(" Split pot (" + splitAmount + " each) - " + winningHand.getRank());
            
            for (String winnerId : winners) {
                Player winner = this.players.get(winnerId);
                // อัปเดตเงิน
                winner.setStack(winner.getStack() + splitAmount);
                System.out.println("   -> " + winner.getUsername() + " gets " + splitAmount);
                winnersInfo.add(Map.of(
                "username", winner.getUsername(),
                "amount", splitAmount,
                "handRank", playerHands.get(winnerId).getRank().toString()
            ));
            }
        }
        this.gameOverData = Map.of(
            "winners", winnersInfo,
            "communityCards", new ArrayList<>(this.board) // ส่งไพ่กลางไปด้วย
        );
        _handOver();
    }
    
    private void _handOver() {
        if (this.activePlayerIds.size() == 1) {
            String winnerId = this.activePlayerIds.get(0);
            Player winner = this.players.get(winnerId);
            
            System.out.println(" Player " + winner.getUsername() + 
                               " WINS " + this.pot + " (everyone else folded)");
            
            winner.setStack(winner.getStack() + this.pot);

            this.gameOverData = Map.of(
                "winners", List.of(Map.of(
                    "username", winner.getUsername(),
                    "amount", this.pot,
                    "handRank", "Fold Win"
                )),
                "communityCards", new ArrayList<>(this.board)
            );
        }

        System.out.println("\n--- HAND OVER ---");
        System.out.println("Waiting for next game...\n");

        // --- รีเซ็ตค่าต่างๆ เพื่อรอเริ่มเกมใหม่ ---
        this.pot = 0;
        this.currentBet = 0;
        this.board.clear();
        this.playerBets.clear();
        this.playersActed.clear();
        this.deck = new Deck(); // เตรียมสำรับใหม่

        // เคลียร์สถานะผู้เล่นทุกคน
        for (Player player : this.players.values()) {
            // ล้างไพ่ในมือ
            if (player.getHand() != null) {
                player.getHand().clear();
            }
            // รีเซ็ตสถานะ
            player.setCurrentRoundBet(0);
            player.setFolded(false);
            player.setHasActed(false);
        }
    }

    public List<String> getActivePlayerStrings() {
        List<String> ActiveUserList =  new ArrayList<>();
        for (String activeId : activePlayerIds) {
            ActiveUserList.add(this.players.get(activeId).getUsername());
        }
        return ActiveUserList;
    }
}