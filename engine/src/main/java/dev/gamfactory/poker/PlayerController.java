package dev.gamfactory.poker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PlayerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @PostMapping("/getUserInfo")
    public ResponseEntity<User> getUserInfo(@RequestBody User request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String username = request.getUsername().trim();
        Optional<User> existingUser = userRepository.findByUsername(username);

        if (existingUser.isPresent()) {
            return ResponseEntity.ok(existingUser.get());
        } else {
            User newUser = new User(username);
            User savedUser = userRepository.save(newUser);
            return ResponseEntity.ok(savedUser);
        }
    }

    @PostMapping("/createRoom")
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, String> payload) {
        // Extract username from the JSON payload (e.g. { "username": "JohnDoe" })
        String username = payload.get("username");
        
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String roomId;
        boolean exists;

        // 1. Generate unique roomId (6 chars, All Caps & Numbers)
        // 2. Check if roomId is in database; if so, regenerate
        do {
            roomId = generateRandomRoomId(6);
            exists = roomRepository.existsById(roomId);
        } while (exists);

        // 3. Create new room
        Room newRoom = new Room(roomId);

        // 4. Add received username to new room
        newRoom.addPlayer(username);

        // 5. Upload to database
        Room savedRoom = roomRepository.save(newRoom);

        // 6. Return roomId and list of players (The Room object contains both)
        return ResponseEntity.ok(savedRoom);
    }

    // Helper method to generate random string
    private String generateRandomRoomId(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}