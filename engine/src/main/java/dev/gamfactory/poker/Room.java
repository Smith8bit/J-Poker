package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rooms")
public class Room {
    
    @Id
    private String roomId;
    
    private List<String> players;
    
    private int maxPlayers;

    public Room() {

    }

    public Room(String roomId) {
        this.roomId = roomId;
        this.maxPlayers = 6;
        this.players = new ArrayList<>();
    }
    
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    public void addPlayer(String username) {
        if (!isFull() && !players.contains(username)) {
            players.add(username);
        }
    }

    // Getters are required for the JSON response
    public String getRoomId() {
        return roomId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayersNumber() {
        return players.size();
    }
}