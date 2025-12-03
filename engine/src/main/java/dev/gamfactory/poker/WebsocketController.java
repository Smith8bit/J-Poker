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
            case "leave_room":
                handleLeaveRoom(session);
                break;
        }
    }

    private void handleCreateRoom(WebSocketSession session, JsonNode data) throws IOException {
         String username = data.get("username").asText();
         String roomId;
         boolean exists;
         do { roomId = generateRandomRoomId(6); exists = roomRepository.existsById(roomId); } while (exists);
         
         Room newRoom = new Room(roomId);
         Player host = new Player(session.getId(), username, 10000);
         host.setHost(true);
         newRoom.addPlayer(host);
         roomRepository.save(newRoom);
         
         registerSession(session, roomId, username);
         sendResponse(session, "CREATE_SUCCESS", roomId, newRoom);
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {
        String username = data.get("username").asText();
        String roomId = data.get("roomId").asText();
        
        String oldRoomId = sessionToRoom.get(session.getId());
        if(oldRoomId != null) {
            Set<WebSocketSession> oldS = roomSessions.get(oldRoomId);
            if(oldS != null) oldS.remove(session);
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            // เช็คว่า User เดิมไหม (ถ้าเดิมใช้ Object เดิม Host จะไม่หาย)
            Player existing = room.getPlayers().stream().filter(p -> p.getUsername().equals(username)).findFirst().orElse(null);
            
            if(existing != null) {
                existing.setId(session.getId());
                System.out.println("User " + username + " reconnected.");
            } else {
                Player p = new Player(session.getId(), username, 10000);
                p.setHost(false);
                room.addPlayer(p);
            }
            roomRepository.save(room);
            
            registerSession(session, roomId, username);
            
            sendResponse(session, "JOIN_SUCCESS", roomId, room);
            broadcast(roomId, "PLAYER_JOINED", room, session);
        } else {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", "JOIN_ERROR", "payload", Map.of("error", "Room not found")))));
        }
    }
    
    private void handleStartGame(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = data.get("roomId").asText();
        int bigBlind = data.has("bigBlind") ? data.get("bigBlind").asInt() : 100;
        Optional<Room> r = roomRepository.findById(roomId);
        if(r.isPresent()) {
            Room room = r.get();
            room.setBigBlind(bigBlind);
            roomRepository.save(room);
            broadcast(roomId, "GAME_STARTED", room, null);
        }
    }

    // --- ฟังก์ชันกดปุ่มออก (ลบทันที) ---
    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUsername.remove(session.getId());

        if (roomId != null && username != null) {
            removeFromSessionMap(roomId, session);
            
            // ลบจาก DB ทันที
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                room.removePlayer(username);

                if (room.getPlayers().isEmpty()) {
                    roomRepository.deleteById(roomId);
                    roomSessions.remove(roomId);
                } else {
                    roomRepository.save(room);
                    broadcast(roomId, "PLAYER_LEFT", room, session);
                }
            }
        }
    }

    // ---ฟังก์ชันปิดแท็บ/F5 (รอ 5 วิ ค่อยลบ)---
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // 1. ลบออกจาก Memory Map ก่อน (เพื่อให้รู้ว่า Socket นี้ตายแล้ว)
        String roomId = sessionToRoom.remove(session.getId());
        String username = sessionToUsername.remove(session.getId());

        if (roomId != null && username != null) {
            removeFromSessionMap(roomId, session);

            // ยังไม่ลบออกจาก DB รอเช็คก่อนว่าเขากลับมาไหม
            System.out.println("User " + username + " disconnected. Waiting 5s...");

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    // 2. เช็คว่า User กลับมาออนไลน์ในห้องเดิมหรือยัง? (โดยดูจาก roomSessions)
                    boolean isUserBack = false;
                    Set<WebSocketSession> currentSessions = roomSessions.get(roomId);
                    if (currentSessions != null) {
                        for (WebSocketSession s : currentSessions) {
                            // เช็คว่ามี Session ไหนที่เป็นของ username นี้ไหม
                            if (username.equals(sessionToUsername.get(s.getId()))) {
                                isUserBack = true;
                                break;
                            }
                        }
                    }

                    if (isUserBack) {
                        System.out.println("User " + username + " reconnected. No action needed.");
                    } else {
                        // 3. ถ้าไม่กลับมา -> ลบออกจาก DB จริงๆ
                        try {
                            Optional<Room> roomOpt = roomRepository.findById(roomId);
                            if (roomOpt.isPresent()) {
                                Room room = roomOpt.get();
                                room.removePlayer(username); // ลบผู้เล่น

                                if (room.getPlayers().isEmpty()) {
                                    roomRepository.deleteById(roomId); // ลบห้อง
                                    roomSessions.remove(roomId);
                                    System.out.println("Room " + roomId + " deleted (Timeout).");
                                } else {
                                    roomRepository.save(room); // บันทึกห้อง
                                    // แจ้งคนอื่นว่าออกแล้ว
                                    broadcast(roomId, "PLAYER_LEFT", room, null); 
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, 5000); // รอ 5 วินาที
        }
    }

    // Helper methods ...
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
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of("type", type, "payload", payload))));
    }

    private void broadcast(String roomId, String type, Room room, WebSocketSession exclude) throws IOException {
        String json = objectMapper.writeValueAsString(Map.of("type", type, "payload", Map.of("playersNum", room.getPlayersNumber(), "players", room.getPlayers())));
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