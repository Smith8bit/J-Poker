package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;

public class Player {
    
    // ข้อมูลพื้นฐาน
    private String id;        // Session ID
    private String username;  // ชื่อผู้เล่น
    private int stack;        // เงินเดิมพัน
    private boolean isHost;   // เป็นหัวห้องหรือไม่
    
    // ข้อมูลการเล่นเกม (Game Logic)
    private List<Card> hand;     // ไพ่ในมือ
    private int currentRoundBet; // เงินที่ลงในรอบปัจจุบัน
    private boolean folded;      // หมอบหรือยัง
    private boolean hasActed;    // เล่นหรือยังในรอบนี้

    // Constructor
    public Player(String id, String username, int stack) {
        this.id = id;
        this.username = username;
        this.stack = stack;
        this.isHost = false;
        
        // Init ค่าเริ่มต้น
        this.hand = new ArrayList<>();
        this.currentRoundBet = 0;
        this.folded = false;
        this.hasActed = false;
    }

    // 🔥🔥🔥 เมธอดที่ขาดไป (เพิ่มให้แล้วครับ) 🔥🔥🔥
    
    public List<Card> getHand() {
        return hand;
    }

    public void setHand(List<Card> hand) {
        this.hand = hand;
    }

    // ฟังก์ชันเพิ่มไพ่ทีละใบ (ใช้ตอนแจกไพ่)
    public void addCard(Card card) {
        if (this.hand == null) {
            this.hand = new ArrayList<>();
        }
        this.hand.add(card);
    }

    // --- Getters & Setters อื่นๆ ---

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    // username ไม่มี setter เพราะไม่ควรเปลี่ยน

    public int getStack() { return stack; }
    public void setStack(int stack) { this.stack = stack; }

    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    // --- Game Logic Getters & Setters ---

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