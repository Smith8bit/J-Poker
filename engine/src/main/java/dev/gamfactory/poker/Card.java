package dev.gamfactory.poker;
record Card(String rank, String suit) {
    @Override
    public String toString() {
        return rank + suit;
    }
    public String getRank() {
        return rank;
    }
    
    public String getSuit() {
        return suit;
    }
}