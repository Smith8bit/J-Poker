package dev.gamfactory.poker;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WebsocketController extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();

    @Autowired
    private RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        if (!jsonNode.has("action")) return;
        String action = jsonNode.get("action").asText();

        switch (action) {
            case "create_room":
                handleCreateRoom(session, jsonNode.get("data"));
                break;
            case "join_room":
                handleJoinRoom(session, jsonNode.get("data"));
                break;
            case "start_game":
                handleStartGame(session, jsonNode.get("data"));
                break;
            case "leave_room": // ✅ รองรับปุ่มกดออก
                handleLeaveRoom(session);
                break;
        }
    }

    // ... handleCreateRoom และ handleJoinRoom (ใช้โค้ดเดิมของคุณได้เลยครับ) ...
    // ผมละไว้เพื่อความสั้น แต่ให้คง Logic เดิมที่คุณมีไว้นะครับ 
    
    private void handleCreateRoom(WebSocketSession session, JsonNode data) throws IOException {
         String username = data.get("username").asText();
         // ... (Logic random ID) ...
         String roomId = generateRandomRoomId(6); // สมมติว่า random มาแล้ว
         
         Room newRoom = new Room(roomId);
         Player host = new Player(session.getId(), username, 10000);
         host.setHost(true);
         newRoom.addPlayer(host);
         System.out.println("Create Room " + roomId + " by " + username);
         roomRepository.save(newRoom);
         
         registerSession(session, roomId, username);
         
         // Response ...
         sendResponse(session, "CREATE_SUCCESS", roomId, newRoom);
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String username = data.get("username").asText();
        String roomId = data.get("roomId").asText();
        
        // Clear session เก่าถ้ามี ...
        
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            // Logic เช็คคนเดิม หรือ สร้างใหม่ ...
            Player existing = room.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
            if(existing != null) {
                existing.setId(session.getId());
            } else {
                Player p = new Player(session.getId(), username, 10000);
                p.setHost(false);
                room.addPlayer(p);
            }
            roomRepository.save(room);
            
            registerSession(session, roomId, username);
            
            sendResponse(session, "JOIN_SUCCESS", roomId, room);
            broadcast(roomId, "PLAYER_JOINED", room, session);
        }
    }
    
    private void handleStartGame(WebSocketSession session, JsonNode data) {
         // ... Logic เดิม ...
    }

    // ✅ 1. ฟังก์ชันกดปุ่มออก (ลบทันที)
    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUsername.remove(session.getId());

        if (roomId != null && username != null) {
            removeFromSessionMap(roomId, session);

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                room.removePlayer(username); // เรียกใช้ฟังก์ชัน removePlayer ที่แก้แล้วใน Room.java

                if (room.getPlayers().isEmpty()) {
                    roomRepository.deleteById(roomId); // ลบห้องทันที
                    roomSessions.remove(roomId);
                    System.out.println("Room " + roomId + " deleted (User left).");
                } else {
                    roomRepository.save(room); // บันทึกห้องที่ไม่มีคนนี้แล้ว
                    broadcast(roomId, "PLAYER_LEFT", room, session);
                }
            }
        }
    }

    // ✅ 2. ฟังก์ชันปิดแท็บ/เน็ตหลุด (รอ 5 วิค่อยลบ)
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUsername.get(session.getId()); // ดึงชื่อมาก่อนลบ
        sessionToUsername.remove(session.getId());

        if (roomId != null && username != null) {
            removeFromSessionMap(roomId, session);

            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                room.removePlayer(username); // ลบคนออกจาก Object Room (ใน Memory)

                if (room.getPlayers().isEmpty()) {
                    System.out.println("Room " + roomId + " is empty. Waiting 5s...");
                    // เริ่มนับถอยหลัง 5 วินาที
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // เช็คอีกรอบว่ายังว่างจริงไหม
                            Optional<Room> checkRoom = roomRepository.findById(roomId);
                            if (checkRoom.isPresent() && checkRoom.get().getPlayers().isEmpty()) {
                                roomRepository.deleteById(roomId);
                                roomSessions.remove(roomId);
                                System.out.println("Room " + roomId + " deleted (Timeout).");
                            }
                        }
                    }, 5000);
                } else {
                    roomRepository.save(room);
                    broadcast(roomId, "PLAYER_LEFT", room, session);
                }
            }
        }
    }

    // Helper methods
    private void registerSession(WebSocketSession session, String roomId, String username) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);
        sessionToUsername.put(session.getId(), username);
    }
    
    private void removeFromSessionMap(String roomId, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if (sessions != null) sessions.remove(session);
    }

    private void sendResponse(WebSocketSession session, String type, String roomId, Room room) throws IOException {
        Map<String, Object> payload = Map.of("roomId", roomId, "playersNum", room.getPlayersNumber(), "players", room.getPlayers());
        Map<String, Object> response = Map.of("type", type, "payload", payload);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void broadcast(String roomId, String type, Room room, WebSocketSession exclude) throws IOException {
        Map<String, Object> payload = Map.of("playersNum", room.getPlayersNumber(), "players", room.getPlayers());
        Map<String, Object> msg = Map.of("type", type, "payload", payload);
        String json = objectMapper.writeValueAsString(msg);
        
        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if(sessions != null) {
            for(WebSocketSession s : sessions) {
                if(s.isOpen() && (exclude == null || !s.getId().equals(exclude.getId()))) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        }
    }
    
    private String generateRandomRoomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        return sb.toString();
    }
}