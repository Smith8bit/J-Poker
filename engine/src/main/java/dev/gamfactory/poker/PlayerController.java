package dev.gamfactory.poker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // Adjust port if needed
public class PlayerController {

    @PostMapping("/join")
    public ResponseEntity<PlayerResponse> joinGame(@RequestBody JoinRequest request) {
        // Validate username
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        // Create player with starting money (e.g., $1000)
        PlayerResponse response = new PlayerResponse(
            request.getUsername().trim(),
            1000.0
        );
        
        return ResponseEntity.ok(response);
    }
}

// DTO Classes
class JoinRequest {
    private String username;
    
    public JoinRequest() {}
    
    public JoinRequest(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}

class PlayerResponse {
    private String username;
    private double moneyAmount;
    
    public PlayerResponse() {}
    
    public PlayerResponse(String username, double moneyAmount) {
        this.username = username;
        this.moneyAmount = moneyAmount;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public double getMoneyAmount() {
        return moneyAmount;
    }
    
    public void setMoneyAmount(double moneyAmount) {
        this.moneyAmount = moneyAmount;
    }
}