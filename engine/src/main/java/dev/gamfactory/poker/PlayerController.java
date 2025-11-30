package dev.gamfactory.poker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class PlayerController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/getUserInfo")
    public ResponseEntity<User> getUserInfo(@RequestBody User request) {
        // 1. Validate input
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String username = request.getUsername().trim();

        // 2. Try to load from database
        Optional<User> existingUser = userRepository.findByUsername(username);

        if (existingUser.isPresent()) {
            // 3. User found: Return the existing user
            return ResponseEntity.ok(existingUser.get());
        } else {
            // 4. User not found: Create, Save to DB, and Return
            User newUser = new User(username);
            // You might want to set default chips/balance here, e.g., newUser.setChips(1000);
            
            User savedUser = userRepository.save(newUser);
            return ResponseEntity.ok(savedUser);
        }
    }
}