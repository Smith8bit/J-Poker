package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rooms")
public class Room {
    
    @Id
    private String roomId;
    
    private List<String> players;
    
    private int maxPlayers;

    private Game game;

    private int bigBlind;

    public Room() {

    }

    public Room(String roomId) {
        this.roomId = roomId;
        this.maxPlayers = 6;
        this.players = new ArrayList<>();
        this.bigBlind = 20;
    }
    
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    public void addPlayer(String username) {
        if (!isFull() && !players.contains(username)) {
            players.add(username);
        }
    }

    public void removePlayer(String username) {
        if (players.contains(username)) {
            players.remove(username);
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

    public boolean hasPlayer(String username) {
        return players.contains(username);
    }

    public void startGame(UserRepository userRepository) {
        
        List<Player> participants = new ArrayList<>();
        
        for (String username : players) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            participants.add(new Player(username, userOpt.get().getUserCredit()));
        }
        
        this.game = new Game(participants, bigBlind);
        this.game.start();
    }
}