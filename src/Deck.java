import java.util.*;

public class Deck {
    private static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K", "A"};
    private static final String[] SUITS = {"C", "D", "H", "S"};
    public List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
        for (String suit : SUITS) {
            for (String rank : RANKS) {
                this.cards.add(new Card(rank, suit));
            }
        }
        this.shuffle();
    }

    public void shuffle() {
        Collections.shuffle(this.cards);
    }

    public Card deal() {
        if (this.cards.isEmpty()) {
            return null; // Or throw exception
        }
        return this.cards.removeLast();
    }

    // Multi-card deal
    // Use in FLOP
    public List<Card> deal(int n) {
        List<Card> dealtCards = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Card card = this.deal();
            if (card != null) {
                dealtCards.add(card);
            }
        }
        return dealtCards;
    }
}

record Card(String rank, String suit) {
    @Override
    public String toString() {
        return rank + suit;
    }
}