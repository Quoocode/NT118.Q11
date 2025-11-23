package com.example.habittracker.DatabaseStructure;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class History {
    @DocumentId
    private String historyId; // habitId_YYYYMMDD
    private String habitId;
    private String userId;
    private int value;
    private String notes;
    private String status;
    private Timestamp date;

    // Empty Constructor (Required)
    public History() {}

    public History(String historyId, String habitId, String userId, int value, String notes) {
        this.historyId = historyId;
        this.habitId = habitId;
        this.userId = userId;
        this.value = value;
        this.notes = notes;
        this.status = "completed";
        this.date = Timestamp.now();
    }

    // Getters and Setters
    public String getHistoryId() { return historyId; }
    public void setHistoryId(String historyId) { this.historyId = historyId; }

    public String getHabitId() { return habitId; }
    public void setHabitId(String habitId) { this.habitId = habitId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
}