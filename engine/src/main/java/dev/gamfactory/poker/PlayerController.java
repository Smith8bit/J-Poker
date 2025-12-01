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
}