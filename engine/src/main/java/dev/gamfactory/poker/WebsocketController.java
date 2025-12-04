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
    private final Map<String, TimerTask> scheduledDisconnects = new ConcurrentHashMap<>();

    @Autowired
    private RoomRepository roomRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long DISCONNECT_TIMEOUT = 5000;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(message.getPayload());
        if (!jsonNode.has("action")) return;
        
        String action = jsonNode.get("action").asText();
        JsonNode data = jsonNode.get("data");

        switch (action) {
            case "create_room":
                handleCreateRoom(session, data);
                break;
            case "join_room":
                handleJoinRoom(session, data);
                break;
            case "start_game":
                handleStartGame(session, data);
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

        do {
            roomId = generateRandomRoomId(6);
            exists = roomRepository.existsById(roomId);
        } while (exists);
         
        Room newRoom = new Room(roomId);
        Player host = new Player(session.getId(), username, 10000);
        
        host.setHost(true);
        newRoom.addPlayer(host);
        
        roomRepository.save(newRoom);
        
        registerSession(session, roomId, username);
        
        sendResponse(session, "CREATE_SUCCESS", roomId, newRoom);
        System.out.println("Create Room " + roomId + " by " + username);
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode data) throws IOException {

        String username = data.get("username").asText();
        String roomId = data.get("roomId").asText();

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();

            String userKey = roomId + ":" + username;
           if (scheduledDisconnects.containsKey(userKey)) {
                scheduledDisconnects.get(userKey).cancel();
                scheduledDisconnects.remove(userKey);
                System.out.println("Reconnection detected for: " + username + ". Removal cancelled.");
            }

            Player existingPlayer = room.getPlayers().stream()
                .filter(p -> p.getUsername().equals(username))
                .findFirst()
                .orElse(null);

            if (existingPlayer != null) {
                // UPDATE existing player's session ID (Don't reset chips)
                existingPlayer.setId(session.getId());
            } else {
                // NEW player
                if (room.getPlayers().size() >= 6) { 
                    //ห้องเต็ม ส่ง Error กลับไป
                    Map<String, Object> errorResponse = Map.of(
                        "type", "JOIN_ERROR", 
                        "payload", Map.of("error", "Room is full (6/6).")
                    );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
                    return; //จบการทำงานทันที ไม่ให้เข้า ไม่ให้บันทึก
                } else {
                    Player p = new Player(session.getId(), username, 10000);
                    p.setHost(false);
                    room.addPlayer(p);
                }
            }
            
            roomRepository.save(room);
            registerSession(session, roomId, username);
            
            sendResponse(session, "JOIN_SUCCESS", roomId, room);
            System.out.println(username+" has join Room: "+roomId);

            broadcast(roomId, "PLAYER_JOINED", room, session);
        } else {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString( Map.of("type", "JOIN_ERROR", "payload", Map.of("error", "Room does not exist.")))));
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
    // Exist Button Handler
    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUsername.get(session.getId());

        cleanupSessionMaps(session);

        if (roomId != null && username != null) {
            processPlayerRemoval(roomId, username);
        }
    }

    // New Reconnect Logic
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUsername.get(session.getId());

        // Remove from WebSocket maps immediately so we don't try to send them messages
        cleanupSessionMaps(session);

        if (roomId != null && username != null) {
            // Do NOT remove from Room object yet. Schedule a check.
            String userKey = roomId + ":" + username;
            
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    // Logic to run if time expires
                    try {
                        System.out.println("Timeout reached for " + username + ". Removing from room.");
                        processPlayerRemoval(roomId, username);
                        scheduledDisconnects.remove(userKey);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            scheduledDisconnects.put(userKey, task);
            new Timer().schedule(task, DISCONNECT_TIMEOUT);
            System.out.println("Session closed for " + username + ". Waiting " + DISCONNECT_TIMEOUT + "ms for reconnect...");
        }
    }

    // Helper methods ...
    private void registerSession(WebSocketSession session, String roomId, String username) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);
        sessionToUsername.put(session.getId(), username);
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
        
        System.out.println("Broadcast "+type+" to room: "+roomId);
    }
    
    private String generateRandomRoomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        return sb.toString();
    }

    private synchronized void processPlayerRemoval(String roomId, String username) throws IOException {
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.removePlayer(username);

            System.out.println(username+" has left Room: "+roomId);


            if (room.getPlayers().isEmpty()) {
                roomRepository.deleteById(roomId);
                roomSessions.remove(roomId);
                System.out.println("Room " + roomId + " deleted (Empty).");
            } else {
                room.getPlayers().get(0).setHost(true);
                roomRepository.save(room);
                broadcast(roomId, "PLAYER_LEFT", room, null);
            }
        }
    }

    private void cleanupSessionMaps(WebSocketSession session) {
        String roomId = sessionToRoom.remove(session.getId());
        sessionToUsername.remove(session.getId());
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) sessions.remove(session);
        }
    }
}