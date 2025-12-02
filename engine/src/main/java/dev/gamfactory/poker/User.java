package dev.gamfactory.poker;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

    @Id
    private String id; // Unique MongoDB ID
    
    public String username;
    public int userCredit;
    private static final int startCredit = 10000;

    public User() {
    }

    public User(String username) {
        this.username = username;
        this.userCredit = startCredit; // Sets default 10000
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public int getUserCredit() {
        return userCredit;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserCredit(int userCredit) {
        this.userCredit = userCredit;
    }
}