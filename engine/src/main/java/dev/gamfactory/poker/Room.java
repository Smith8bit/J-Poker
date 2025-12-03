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
    
    private List<Player> players;
    
    private int maxPlayers;

    private Game game;

    private int bigBlind;

    public Room() {}

    public Room(String roomId) {
        this.roomId = roomId;
        this.maxPlayers = 6;
        this.players = new ArrayList<>();
        this.bigBlind = 100;
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
    
    // Getters are required for the JSON response
    public String getRoomId() {
        return roomId;
    }

    public List<Player> getPlayers() { return players; }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayersNumber() {
        return players.size();
    }

    public boolean hasPlayer(String username) {
        return players.stream().anyMatch(p -> p.getUsername().equals(username));
    }
    
    public void setBigBlind(int bigBlind) { this.bigBlind = bigBlind; }
    public int getBigBlind() { return bigBlind; }

    public void startGame(UserRepository userRepository) {
        
        for (Player p : this.players) {
            // 1. ดึง Username จาก Player Object ที่เก็บไว้
            String username = p.getUsername();
            
            // 2. เช็คเงินล่าสุดจาก Database (เผื่อเขาเติมเงินมา)
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (userOpt.isPresent()) {
                // 3. อัปเดตเงิน (Stack) ให้ตรงกับ Database
                p.setStack(userOpt.get().getUserCredit());
            }
        }
        
        // 4. ส่ง List<Player> ตัวเดิม (ที่อัปเดตเงินแล้ว) ไปเข้า Game Engine
        this.game = new Game(this.players, bigBlind);
        this.game.start();
    }
}