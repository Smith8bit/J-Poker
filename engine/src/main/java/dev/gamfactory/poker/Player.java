package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;

public class Player {
    String id;
    String username;
    int stack;
    boolean isHost;
    List<Card> hand;

    public Player(String id, String username, int stack) {
        this.id = id;
        this.username = username;
        this.stack = stack;
        this.isHost = false;
        this.hand = new ArrayList<>();
    }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }

    public void setId(String id) {
        this.id = id;
    }

    public void setStack(int stack) {
        this.stack = stack;
    }   
    
    public String getUsername() { return username; }
    public String getId() { return id; }
    public int getStack() { return stack; }

    @Override
    public String toString() {
        return String.format("dev.gamfactory.poker.Player[id=%s, stack=%d, hand=%s]", id, stack, hand);
    }
}
