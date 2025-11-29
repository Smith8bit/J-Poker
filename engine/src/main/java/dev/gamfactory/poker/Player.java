package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;

public class Player {
    String id;
    int stack;
    List<Card> hand;

    public Player(String id, int stack) {
        this.id = id;
        this.stack = stack;
        this.hand = new ArrayList<>();
    }

    @Override
    public String toString() {
        return String.format("dev.gamfactory.poker.Player[id=%s, stack=%d, hand=%s]", id, stack, hand);
    }
}
