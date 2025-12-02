package dev.gamfactory.poker;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // <--- Added for Spring detection
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode; // <--- FIXED: Added Import
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebsocketController extends TextWebSocketHandler {

    // Map: roomId -> Set of sessions in that room
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    // Map: sessionId -> roomId (to track which room a session is in)
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    @Autowired
    private RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Read the incoming JSON (formerly your @RequestBody)
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String action = jsonNode.get("action").asText();

        switch (action) {
            case "create_room":
                handleCreateRoom(session, jsonNode.get("data"));
                break;
            case "join_room":
                handleJoinRoom(session, jsonNode.get("data"));
            default:
                break;
        }
    }

    private void handleCreateRoom(WebSocketSession session, JsonNode data) throws IOException {
        
        String username = data.get("username").asText();
        String roomId;
        boolean exists;

        do {
            roomId = generateRandomRoomId(6);
            exists = roomRepository.existsById(roomId);
        } while (exists);

        Room newRoom = new Room(roomId);
        newRoom.addPlayer(username);
        roomRepository.save(newRoom);

        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);

        Map<String, Object> response = Map.of(
            "type", "CREATE_SUCCESS",
            "payload", Map.of(
                "roomId", roomId,
                "playersNum", newRoom.getPlayersNumber()
            )
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String username = data.get("username").asText();
        String roomId = data.get("roomId").asText();

        Optional<Room> existingRoom = roomRepository.findById(roomId);

        Map<String, Object> response = new HashMap<>();

        if (existingRoom.isPresent()) {
            Room room = existingRoom.get();
            room.addPlayer(username);

            roomRepository.save(room);
            
            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionToRoom.put(session.getId(), roomId);

            response.put("type", "JOIN_SUCCESS");
            response.put("payload", Map.of("roomId", roomId, "playersNum", room.getPlayersNumber()));
            
            // send updated room data to all user in the room 
            broadcastToRoom(roomId, Map.of(
                "type", "PLAYER_JOINED",
                "payload", Map.of("playersNum", room.getPlayersNumber())
            ));

        } else {
            response.put("type", "JOIN_ERROR");
            response.put("payload", Map.of("error", "Room does not exist."));
        }
        
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    // Helper method to generate random string
    private String generateRandomRoomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    // Send Message to user in the room
    private void broadcastToRoom(String roomId, Map<String, Object> message) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }
    
}
