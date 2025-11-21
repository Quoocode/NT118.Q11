package com.example.habittracker.DatabaseStructure;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

public class User {
    @DocumentId // Automatically maps the document ID (uid) to this field
    private String uid;
    private String username;
    private String email;
    private String photoUrl;
    private String fcmToken;
    private String timezone; // e.g., "Asia/Ho_Chi_Minh"
    private int globalStreak;

    @ServerTimestamp // Automatically sets server time on creation
    private Timestamp createdAt;

    // 1. CRITICAL: Empty Constructor for Firestore
    public User() {}

    // 2. Full Constructor for you to use
    public User(String username, String email, String timezone) {
        this.username = username;
        this.email = email;
        this.timezone = timezone;
        this.globalStreak = 0;
    }

    // 3. Getters and Setters (Required for Java)
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public int getGlobalStreak() { return globalStreak; }
    public void setGlobalStreak(int globalStreak) { this.globalStreak = globalStreak; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}