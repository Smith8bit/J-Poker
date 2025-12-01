package dev.gamfactory.poker;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component; // <--- Added for Spring detection
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode; // <--- FIXED: Added Import
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebsocketController extends TextWebSocketHandler {

    @Autowired
    private RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 1. Read the incoming JSON (formerly your @RequestBody)
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String action = jsonNode.get("action").asText();
        System.out.println(action);
        // 2. Route to the correct logic
        if ("create_room".equals(action)) {
            System.out.println("process handle...");
            handleCreateRoom(session, jsonNode.get("data"));
        }
        // } else if ("join_room".equals(action)) {
        //     handleJoinRoom(session, jsonNode.get("data"));
        // }
    }

    private void handleCreateRoom(WebSocketSession session, JsonNode data) throws IOException {
        
        String username = data.get("username").asText();
        System.out.println(username);

        String roomId;
        boolean exists;

        do {
            roomId = generateRandomRoomId(6);
            exists = roomRepository.existsById(roomId);
        } while (exists);

        Room newRoom = new Room(roomId);
        newRoom.addPlayer(username);
        roomRepository.save(newRoom);

        Map<String, Object> response = Map.of(
            "type", "CREATE_SUCCESS",
            "payload", Map.of("roomId", roomId)
        );

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

    private void handleJoinRoom() {}
    
}
