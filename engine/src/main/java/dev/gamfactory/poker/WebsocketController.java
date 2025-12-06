package dev.gamfactory.poker;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
// import java.util.stream.Collectors;

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
    
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

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
            case "game_action":
                handleGameAction(session, data);
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
                existingPlayer.setId(session.getId());
            } else {
                if (room.getPlayers().size() >= 6) { 
                    Map<String, Object> errorResponse = Map.of(
                        "type", "JOIN_ERROR", 
                        "payload", Map.of("error", "Room is full (6/6).")
                    );
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
                    return;
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
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                Map.of("type", "JOIN_ERROR", "payload", Map.of("error", "Room does not exist."))
            )));
        }
    }
    
    private void handleStartGame(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = data.get("roomId").asText();
        int bigBlind = data.has("bigBlind") ? data.get("bigBlind").asInt() : 100;
        
        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if(roomOpt.isPresent()) {
            Room room = roomOpt.get();
            room.setBigBlind(bigBlind);
            
            // Send GAME_STARTED to all players first
            broadcast(roomId, "GAME_STARTED", room, null);
            
            // Start the game engine
            room.startGame(userRepository);
            
            // เก็บ Game object ใน memory
            activeGames.put(roomId, room.getGame());
            
            roomRepository.save(room);
            
            // Send game state to all players
            broadcastGameState(roomId, room);
            System.out.println("Game started in room: " + roomId);
        }
    }

    private void handleGameAction(WebSocketSession session, JsonNode data) throws IOException {
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUsername.get(session.getId());
        
        if (roomId == null || username == null) {
            sendError(session, "Not in a room");
            return;
        }

        // ดึง Game จาก memory แทน
        Game game = activeGames.get(roomId);
        
        if (game == null) {
            sendError(session, "Game not started");
            return;
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (!roomOpt.isPresent()) {
            sendError(session, "Room not found");
            return;
        }

        Room room = roomOpt.get();

        String actionType = data.get("actionType").asText();
        String playerId = session.getId();

        try {
            switch (actionType) {
                case "fold":
                    game.fold(playerId);
                    break;
                case "check":
                    game.check();
                    break;
                case "call":
                    game.call(playerId);
                    break;
                case "bet":
                    int betAmount = data.get("amount").asInt();
                    game.bet(playerId, betAmount);
                    break;
                case "raise":
                    int raiseAmount = data.get("amount").asInt();
                    game.raise(playerId, raiseAmount);
                    break;
                default:
                    sendError(session, "Unknown action: " + actionType);
                    return;
            }
            if (game.gameOverData != null) {
                System.out.println("Saving updated credits to Database...");
                for (Player p : game.players.values()) {    
                    Optional<User> userOpt = userRepository.findByUsername(p.getUsername());
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        // อัปเดตเงินใน DB ให้ตรงกับเงินในเกม (Stack)
                        user.setUserCredit(p.getStack()); 
                        userRepository.save(user); // บันทึก!
                        System.out.println("   -> Updated " + user.getUsername() + ": " + user.getUserCredit());
                    }
                }

                room.getPlayers().clear();
                room.getPlayers().addAll(game.players.values());

                Map<String, Object> payload = new HashMap<>(game.gameOverData);
                // เพิ่มข้อมูล players ล่าสุดไปด้วย (เพื่ออัปเดตเงิน)
                payload.put("players", room.getPlayers()); 
                
                String json = objectMapper.writeValueAsString(Map.of(
                    "type", "GAME_OVER", 
                    "payload", payload
                ));
                
                // Broadcast ให้ทุกคน
                for(WebSocketSession s : roomSessions.get(roomId)) {
                    if(s.isOpen()) s.sendMessage(new TextMessage(json));
                }
                
                // คลียร์ค่าทิ้ง เพื่อไม่ให้ส่งซ้ำ
                game.gameOverData = null; 
                
            } else {
                // ถ้าเกมยังไม่จบ ก็ส่ง GAME_STATE ตามปกติ
                broadcastGameState(roomId, room);
            }

            // Save updated room state (player stacks updated in game)
            roomRepository.save(room);

            // Broadcast new game state to all players
            broadcastGameState(roomId, room);
            
        } catch (IllegalStateException e) {
            sendError(session, e.getMessage());
        }
    }

    private void handleLeaveRoom(WebSocketSession session) throws IOException {
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUsername.get(session.getId());

        cleanupSessionMaps(session);

        if (roomId != null && username != null) {
            processPlayerRemoval(roomId, username);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String roomId = sessionToRoom.get(session.getId());
        String username = sessionToUsername.get(session.getId());

        cleanupSessionMaps(session);

        if (roomId != null && username != null) {
            String userKey = roomId + ":" + username;
            
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
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

    // Helper methods
    private void registerSession(WebSocketSession session, String roomId, String username) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        sessionToRoom.put(session.getId(), roomId);
        sessionToUsername.put(session.getId(), username);
    }

    private void sendResponse(WebSocketSession session, String type, String roomId, Room room) throws IOException {
        Map<String, Object> payload = Map.of(
            "roomId", roomId, 
            "playersNum", room.getPlayersNumber(), 
            "players", room.getPlayers()
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            Map.of("type", type, "payload", payload)
        )));
    }

    private void broadcast(String roomId, String type, Room room, WebSocketSession exclude) throws IOException {
        String json = objectMapper.writeValueAsString(
            Map.of("type", type, "payload", Map.of(
                "playersNum", room.getPlayersNumber(), 
                "players", room.getPlayers()
            ))
        );
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

    private void broadcastGameState(String roomId, Room room) throws IOException {
        // ดึง Game จาก memory
        Game game = activeGames.get(roomId);
        
        if (game == null) {
            System.out.println("Warning: No active game for room " + roomId);
            return;
        }
        
        // แปลง Card objects เป็น Map
        List<Map<String, String>> boardCards = game.board.stream()
            .map(card -> Map.of("rank", card.rank(), "suit", card.suit()))
            .collect(java.util.stream.Collectors.toList());
        
        // แปลง Player hand cards เป็น Map
        List<Map<String, Object>> playersData = room.getPlayers().stream()
            .map(player -> {
                List<Map<String, String>> handCards = player.getHand().stream()
                    .map(card -> Map.of("rank", card.rank(), "suit", card.suit()))
                    .collect(java.util.stream.Collectors.toList());
                
                return Map.of(
                    "id", (Object) player.getId(),
                    "username", player.getUsername(),
                    "stack", player.getStack(),
                    "isHost", player.isHost(),
                    "hand", handCards
                );
            })
            .collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> gameState = Map.of(
            "pot", game.pot,
            "board", boardCards,
            "street", game.street.toString(),
            "currentBet", game.currentBet,
            "bigBlind", game.bigBlind,
            "currentActorId", game.activePlayerIds.isEmpty() ? "" : game.activePlayerIds.get(game.currentActorPos),
            "players", playersData,
            "roomPlayers", room.getUsername(),
            "activePlayerUsername", game.getActivePlayerStrings(),
            "playerBets", game.playerBets
        );

        String json = objectMapper.writeValueAsString(
            Map.of("type", "GAME_STATE", "payload", gameState)
        );

        Set<WebSocketSession> sessions = roomSessions.get(roomId);
        if(sessions != null) {
            for(WebSocketSession s : sessions) {
                if(s.isOpen()) {
                    s.sendMessage(new TextMessage(json));
                }
            }
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        Map<String, Object> response = Map.of(
            "type", "ERROR",
            "payload", Map.of("error", errorMessage)
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }
    
    private String generateRandomRoomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
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