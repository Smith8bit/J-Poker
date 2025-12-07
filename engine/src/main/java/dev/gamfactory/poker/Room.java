package dev.gamfactory.poker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rooms")
public class Room {
    
    @Id
    private String roomId;
    
    private List<Player> players;
    
    private int maxPlayers;

    @Transient
    private Game game;

    private int bigBlind;
    private boolean isPlaying;
    public Room() {}

    public Room(String roomId) {
        this.roomId = roomId;
        this.maxPlayers = 6;
        this.players = new ArrayList<>();
        this.bigBlind = 100;
        this.isPlaying = false;
    }

    public void addPlayer(Player player) {
        if (!isFull() && !hasPlayer(player.getUsername())) {
            players.add(player);
        }
    }
    
    public void removePlayer(String username) {
        if (players != null) {
             players.removeIf(p -> p.getUsername().equals(username));
        }
    }

    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    // Getters
    public String getRoomId() {
        return roomId;
    }

    public List<Player> getPlayers() { 
        return players; 
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayersNumber() {
        return players.size();
    }

    public boolean hasPlayer(String username) {
        return players.stream().anyMatch(p -> p.getUsername().equals(username));
    }
    
    public void setBigBlind(int bigBlind) { 
        this.bigBlind = bigBlind; 
    }
    
    public int getBigBlind() { 
        return bigBlind; 
    }

    public Game getGame() {
        return this.game;
    }

    public void startGame(UserRepository userRepository) {
        
        for (Player p : this.players) {
            String username = p.getUsername();
            p.resetHand();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                p.setStack(userOpt.get().getUserCredit());
            }
        }
        
        this.game = new Game(this.players, bigBlind);
        this.isPlaying = true;
        this.game.start();
    }

    public List<String> getUsername() {
        List<String> userList =  new ArrayList<>();
        for (Player player : this.players) {
            userList.add(player.getUsername());
        }
        return userList;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean status) {
        this.isPlaying =  status;
    }
}