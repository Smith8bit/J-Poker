package dev.gamfactory.poker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PlayerController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/join")
    public ResponseEntity<PlayerResponse> joinGame(@RequestBody JoinRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        PlayerResponse response = new PlayerResponse(
            request.getUsername().trim(),
            1000.0
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/createRoom")
    public ResponseEntity<CreateRoomResponse> createRoom(@RequestBody CreateRoomRequest request) {
        String username = request.getUsername();
        String roomCode = generateRoomCode();
        
        Room room = roomService.createRoom(roomCode, 6);
        room.addPlayer(username);
        
        return ResponseEntity.ok(
            new CreateRoomResponse(roomCode, room.isFull(), room.getPlayersNum(), room.getPlayers())
        );
    }

    @PostMapping("/joinRoom")
    public ResponseEntity<RoomResponse> joinRoom(@RequestBody JoinRoomRequest request) {
        String roomCode = request.getRoomCode();
        String username = request.getUsername();
        
        if (!roomService.roomExists(roomCode)) {
            return ResponseEntity.badRequest().body(
                new RoomResponse(true, 0, new ArrayList<>(), "Room not found")
            );
        }
        
        Room room = roomService.getRoom(roomCode);
        
        if (room.isFull()) {
            return ResponseEntity.ok(
                new RoomResponse(true, room.getPlayersNum(), room.getPlayers(), null)
            );
        }
        
        room.addPlayer(username);
        
        return ResponseEntity.ok(
            new RoomResponse(room.isFull(), room.getPlayersNum(), room.getPlayers(), null)
        );
    }
    
    @GetMapping("/room/{roomCode}/players")
    public ResponseEntity<RoomResponse> getRoomPlayers(@PathVariable String roomCode) {
        if (!roomService.roomExists(roomCode)) {
            return ResponseEntity.notFound().build();
        }
        
        Room room = roomService.getRoom(roomCode);
        return ResponseEntity.ok(
            new RoomResponse(room.isFull(), room.getPlayersNum(), room.getPlayers(), null)
        );
    }
    
    @PostMapping("/leaveRoom")
    public ResponseEntity<Void> leaveRoom(@RequestBody LeaveRoomRequest request) {
        String roomCode = request.getRoomCode();
        String username = request.getUsername();
        
        if (roomService.roomExists(roomCode)) {
            Room room = roomService.getRoom(roomCode);
            room.removePlayer(username);
            
            if (room.getPlayersNum() == 0) {
                roomService.deleteRoom(roomCode);
            }
        }
        
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody HeartbeatRequest request) {
        String roomCode = request.getRoomCode();
        String username = request.getUsername();
        
        System.out.println("Heartbeat from: " + username + " in room: " + roomCode);
        
        if (roomService.roomExists(roomCode)) {
            Room room = roomService.getRoom(roomCode);
            room.updatePlayerHeartbeat(username);
        }
        
        return ResponseEntity.ok().build();
    }
    
    private String generateRoomCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

// DTO Classes
class JoinRequest {
    private String username;
    
    public JoinRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}

class PlayerResponse {
    private String username;
    private double moneyAmount;
    
    public PlayerResponse() {}
    
    public PlayerResponse(String username, double moneyAmount) {
        this.username = username;
        this.moneyAmount = moneyAmount;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public double getMoneyAmount() {
        return moneyAmount;
    }
    
    public void setMoneyAmount(double moneyAmount) {
        this.moneyAmount = moneyAmount;
    }
}

class HeartbeatRequest {
    private String username;
    private String roomCode;
    
    public HeartbeatRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}

class LeaveRoomRequest {
    private String username;
    private String roomCode;
    
    public LeaveRoomRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}

class CreateRoomRequest {
    private String username;
    
    public CreateRoomRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}

class CreateRoomResponse {
    private String roomCode;
    private boolean isFull;
    private int playersNum;
    private List<String> players;
    
    public CreateRoomResponse() {}
    
    public CreateRoomResponse(String roomCode, boolean isFull, int playersNum, List<String> players) {
        this.roomCode = roomCode;
        this.isFull = isFull;
        this.playersNum = playersNum;
        this.players = players;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
    
    public boolean isFull() {
        return isFull;
    }
    
    public void setFull(boolean full) {
        isFull = full;
    }
    
    public int getPlayersNum() {
        return playersNum;
    }
    
    public void setPlayersNum(int playersNum) {
        this.playersNum = playersNum;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<String> players) {
        this.players = players;
    }
}

class JoinRoomRequest {
    private String username;
    private String roomCode;
    
    public JoinRoomRequest() {}
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getRoomCode() {
        return roomCode;
    }
    
    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }
}

class RoomResponse {
    private boolean isFull;
    private int playersNum;
    private List<String> players;
    private String message;
    
    public RoomResponse() {}
    
    public RoomResponse(boolean isFull, int playersNum, List<String> players, String message) {
        this.isFull = isFull;
        this.playersNum = playersNum;
        this.players = players;
        this.message = message;
    }
    
    public boolean isFull() {
        return isFull;
    }
    
    public void setFull(boolean full) {
        isFull = full;
    }
    
    public int getPlayersNum() {
        return playersNum;
    }
    
    public void setPlayersNum(int playersNum) {
        this.playersNum = playersNum;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<String> players) {
        this.players = players;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}

class Room {
    private String roomCode;
    private List<String> players;
    private Map<String, Long> playerHeartbeats;
    private int maxPlayers;
    
    public Room(String roomCode, int maxPlayers) {
        this.roomCode = roomCode;
        this.maxPlayers = maxPlayers;
        this.players = new ArrayList<>();
        this.playerHeartbeats = new HashMap<>();
    }
    
    public boolean isFull() {
        return players.size() >= maxPlayers;
    }
    
    public void addPlayer(String username) {
        if (!isFull() && !players.contains(username)) {
            players.add(username);
            playerHeartbeats.put(username, System.currentTimeMillis());
        }
    }
    
    public void removePlayer(String username) {
        players.remove(username);
        playerHeartbeats.remove(username);
    }
    
    public void updatePlayerHeartbeat(String username) {
        if (players.contains(username)) {
            playerHeartbeats.put(username, System.currentTimeMillis());
        }
    }
    
    public void removeInactivePlayers(long timeoutMs) {
        long currentTime = System.currentTimeMillis();
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Long> entry : playerHeartbeats.entrySet()) {
            if (currentTime - entry.getValue() > timeoutMs) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String username : toRemove) {
            removePlayer(username);
        }
    }
    
    public int getPlayersNum() {
        return players.size();
    }
    
    public List<String> getPlayers() {
        return new ArrayList<>(players);
    }
    
    public String getRoomCode() {
        return roomCode;
    }
}