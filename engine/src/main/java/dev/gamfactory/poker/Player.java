package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;

public class Player {
    
    private String id;        
    private String username;  
    private int stack;        
    private boolean isHost;   

    private List<Card> hand;     
    private int currentRoundBet; 
    private boolean folded;      
    private boolean hasActed;    

    public Player(String id, String username, int stack) {
        this.id = id;
        this.username = username;
        this.stack = stack;
        this.isHost = false;
        
        this.hand = new ArrayList<>();
        this.currentRoundBet = 0;
        this.folded = false;
        this.hasActed = false;
    }
    
    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    // ฟังก์ชันเพิ่มไพ่ทีละใบ
    public void addCard(Card card) {
        if (this.hand == null) {
            this.hand = new ArrayList<>();
        }
        this.hand.add(card);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }

    public int getStack() { return stack; }
    public void setStack(int stack) { this.stack = stack; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    public int getCurrentRoundBet() { return currentRoundBet; }
    public void setCurrentRoundBet(int currentRoundBet) { this.currentRoundBet = currentRoundBet; }

    public boolean isFolded() { return folded; }
    public void setFolded(boolean folded) { this.folded = folded; }

    public boolean hasActed() { return hasActed; }
    public void setHasActed(boolean hasActed) { this.hasActed = hasActed; }

    @Override
    public String toString() {
        return String.format("Player[user=%s, stack=%d, hand=%s]", username, stack, hand);
    }
}