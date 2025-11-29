package dev.gamfactory.poker;

import java.util.List;

public class RoomUpdateMessage {
    private String type;
    private String username;
    private List<String> players;
    private Integer playersNum;
    private Boolean isFull;
    private String message;
    
    public RoomUpdateMessage() {}
    
    public RoomUpdateMessage(String type, String username, List<String> players, int playersNum, boolean isFull) {
        this.type = type;
        this.username = username;
        this.players = players;
        this.playersNum = playersNum;
        this.isFull = isFull;
    }
    
    public RoomUpdateMessage(String type, String username, String message) {
        this.type = type;
        this.username = username;
        this.message = message;
    }
    
    // Getters and setters
    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }
    
    public String getUsername() { 
        return username; 
    }
    
    public void setUsername(String username) { 
        this.username = username; 
    }
    
    public List<String> getPlayers() { 
        return players; 
    }
    
    public void setPlayers(List<String> players) { 
        this.players = players; 
    }
    
    public Integer getPlayersNum() { 
        return playersNum; 
    }
    
    public void setPlayersNum(Integer playersNum) { 
        this.playersNum = playersNum; 
    }
    
    public Boolean getIsFull() { 
        return isFull; 
    }
    
    public void setIsFull(Boolean isFull) { 
        this.isFull = isFull; 
    }
    
    public String getMessage() { 
        return message; 
    }
    
    public void setMessage(String message) { 
        this.message = message; 
    }
}