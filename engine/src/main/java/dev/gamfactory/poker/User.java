package dev.gamfactory.poker;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id; // Unique MongoDB ID
    
    public String username;
    public double userCredit;
    private static final double startCredit = 10000;

    // 1. Required: Empty constructor for Spring Data
    public User() {
    }

    // 2. Your custom constructor
    public User(String username) {
        this.username = username;
        this.userCredit = startCredit; // Sets default 10000
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public double getUserCredit() {
        return userCredit;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserCredit(double userCredit) {
        this.userCredit = userCredit;
    }
}