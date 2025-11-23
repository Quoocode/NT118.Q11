package com.example.habittracker.data.model;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
public class User {
    @DocumentId
    private String uid;

    private String email;
    private String displayName;
    private String photoUrl; // Avatar
    private String fcmToken; // Token để bắn thông báo (Cloud Messaging)
    private String timezone; // Quan trọng: Lưu múi giờ (VD: "Asia/Ho_Chi_Minh")

    // Constructor rỗng (Bắt buộc cho Firestore)
    public User() {}

    public User(String email, String displayName, String photoUrl, String fcmToken, String timezone) {
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.fcmToken = fcmToken;
        this.timezone = timezone;
    }

    // --- Getters & Setters ---
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
