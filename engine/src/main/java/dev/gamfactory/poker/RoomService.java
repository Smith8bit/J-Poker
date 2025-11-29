package dev.gamfactory.poker;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RoomService {
    
    private Map<String, Room> rooms = new HashMap<>();
    
    public Room createRoom(String roomCode, int maxPlayers) {
        Room room = new Room(roomCode, maxPlayers);
        rooms.put(roomCode, room);
        return room;
    }
    
    public Room getRoom(String roomCode) {
        return rooms.get(roomCode);
    }
    
    public boolean roomExists(String roomCode) {
        return rooms.containsKey(roomCode);
    }
    
    public Map<String, Room> getAllRooms() {
        return rooms;
    }
    
    public void deleteRoom(String roomCode) {
        rooms.remove(roomCode);
    }
}