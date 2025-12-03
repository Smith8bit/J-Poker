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
import org.springframework.web.socket.CloseStatus;
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

    // Map: sessionId -> username
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();

    @Autowired
    private RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Read the incoming JSON
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        String action = jsonNode.get("action").asText();

        switch (action) {
            case "create_room":
                handleCreateRoom(session, jsonNode.get("data"));
                break;
            case "join_room":
                handleJoinRoom(session, jsonNode.get("data"));
            case "start_game":
                handleStartGame(session, jsonNode.get("data"));
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
        Player hostPlayer = new Player(session.getId(), username, 10000);
        hostPlayer.setHost(true);
        newRoom.addPlayer(hostPlayer);
        System.out.println("DEBUG: Trying to save Room " + roomId + " to MongoDB...");
        roomRepository.save(newRoom);
        System.out.println("DEBUG: Room " + roomId + " Saved Successfully!");

        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);
        sessionToUsername.put(session.getId(), username);


        Map<String, Object> response = Map.of(
            "type", "CREATE_SUCCESS",
            "payload", Map.of(
                "roomId", roomId,
                "playersNum", newRoom.getPlayersNumber(),
                "players", newRoom.getPlayers()
            )
        );

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        System.out.println("Sent");
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String username = data.get("username").asText();
        String roomId = data.get("roomId").asText();

        // Remove old session if this user was already in a room
        String oldRoomId = sessionToRoom.get(session.getId());
        if (oldRoomId != null) {
            Set<WebSocketSession> oldSessions = roomSessions.get(oldRoomId);
            if (oldSessions != null) {
                oldSessions.remove(session);
            }
        }

        Optional<Room> existingRoom = roomRepository.findById(roomId);
        Map<String, Object> response = new HashMap<>();

        if (existingRoom.isPresent()) {
            Room room = existingRoom.get();
            // Only add player if not already in room

            Player existingPlayer = room.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);

            if (existingPlayer != null) {
                // ✅ กรณี 1: ผู้เล่นเดิม (กด F5 หรือ Reconnect)
                // ไม่ต้องสร้างใหม่! ใช้คนเดิม สถานะ Host ก็จะยังเป็น true เหมือนเดิม
                // แค่อัปเดต Session ID ใหม่ให้ตรงกับ Connection ปัจจุบัน
                existingPlayer.setId(session.getId());
                
                System.out.println("User " + username + " reconnected. Host status: " + existingPlayer.isHost());
            } else {
                // ✅ กรณี 2: ผู้เล่นใหม่จริงๆ
                // สร้างใหม่ และให้เป็น Host = false
                Player newPlayer = new Player(session.getId(), username, 10000); // 10000 หรือดึงจาก DB
                newPlayer.setHost(false); 
                room.addPlayer(newPlayer);
            }
            
            roomRepository.save(room);
            
            roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionToRoom.put(session.getId(), roomId);
            sessionToUsername.put(session.getId(), username);

            response.put("type", "JOIN_SUCCESS");
            response.put("payload", Map.of("roomId", roomId, "playersNum", room.getPlayersNumber(), "players", room.getPlayers()));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

            broadcastToRoom(roomId, Map.of(
                "type", "PLAYER_JOINED",
                "payload", Map.of("playersNum", room.getPlayersNumber(), "players", room.getPlayers())
            ), session);

        } else {
            response.put("type", "JOIN_ERROR");
            response.put("payload", Map.of("error", "Room does not exist."));
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        }
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
    private void broadcastToRoom(String roomId, Map<String, Object> message, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            String messageJson = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(messageJson);
            
            for (WebSocketSession session : sessions) {
                // Only send if open AND it is not the excluded session
                if (session.isOpen() && !session.getId().equals(excludeSession.getId())) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    private void handleStartGame(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = data.get("roomId").asText();
        int bigBlind = 100;
        if (data.has("bigBlind")) bigBlind = data.get("bigBlind").asInt();

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            
            room.setBigBlind(bigBlind); // <--- อัปเดต Big Blind
            roomRepository.save(room);  // บันทึกลง MongoDB

            // Broadcast บอกทุกคน
            broadcastToRoom(roomId, Map.of(
                "type", "GAME_STARTED",
                "payload", Map.of("status", "STARTED", "bigBlind", bigBlind, "players", room.getPlayers())
            ), null);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUsername.remove(session.getId());
        
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                
                Optional<Room> existingRoom = roomRepository.findById(roomId);
                if (existingRoom.isPresent()) {
                    Room room = existingRoom.get();
                    room.removePlayer(username);
                    if (room.getPlayers().isEmpty()) {
                        // CASE 1: ห้องว่าง -> ลบทิ้ง
                        System.out.println("DEBUG: Room " + roomId + " is empty.");
                        // roomRepository.deleteById(roomId);   // ลบจาก MongoDB ระเบิด
                        roomSessions.remove(roomId);         // ลบจาก Memory Map
                        
                        System.out.println("Room " + roomId + " has been deleted (No players left).");
                    } else {
                        // CASE 2: ยังมีคนอยู่ -> อัปเดตและแจ้งเตือน
                        roomRepository.save(room); // บันทึกค่าใหม่ลง MongoDB
                        
                        // บอกคนที่เหลือว่ามีคนออก
                        broadcastToRoom(roomId, Map.of(
                            "type", "PLAYER_LEFT",
                            "payload", Map.of(
                                "playersNum", room.getPlayersNumber(), 
                                "players", room.getPlayers()
                            )
                        ), session);
                    }
                }
            }
        }
    }
}
