package dev.gamfactory.poker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private RoomService roomService;

    // Player joins room
    @MessageMapping("/room/{roomCode}/join")
    public void playerJoined(@DestinationVariable String roomCode, @Payload String username) {
        System.out.println("WebSocket: Player " + username + " joining room " + roomCode);
        
        Room room = roomService.getRoom(roomCode);
        if (room != null) {
            // Broadcast to all subscribers of this room
            RoomUpdateMessage message = new RoomUpdateMessage(
                "PLAYER_JOINED", 
                username, 
                room.getPlayers(),
                room.getPlayersNum(),
                room.isFull()
            );
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, message);
            System.out.println("WebSocket: Broadcast sent for player join");
        }
    }

    // Player leaves room
    @MessageMapping("/room/{roomCode}/leave")
    public void playerLeft(@DestinationVariable String roomCode, @Payload String username) {
        System.out.println("WebSocket: Player " + username + " leaving room " + roomCode);
        
        Room room = roomService.getRoom(roomCode);
        if (room != null) {
            room.removePlayer(username);
            
            RoomUpdateMessage message = new RoomUpdateMessage(
                "PLAYER_LEFT", 
                username, 
                room.getPlayers(),
                room.getPlayersNum(),
                room.isFull()
            );
            
            messagingTemplate.convertAndSend("/topic/room/" + roomCode, message);
            System.out.println("WebSocket: Broadcast sent for player leave");
        }
    }

    // Chat message
    @MessageMapping("/room/{roomCode}/chat")
    public void chatMessage(@DestinationVariable String roomCode, @Payload ChatMessage chatMsg) {
        System.out.println("WebSocket: Chat from " + chatMsg.getUsername() + " in room " + roomCode);
        
        RoomUpdateMessage message = new RoomUpdateMessage(
            "CHAT", 
            chatMsg.getUsername(),
            chatMsg.getMessage()
        );
        
        messagingTemplate.convertAndSend("/topic/room/" + roomCode, message);
    }
}

class ChatMessage {
    private String username;
    private String message;
    
    public ChatMessage() {}
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public void setMessage(String message) { 
        this.message = message; 
    }
}