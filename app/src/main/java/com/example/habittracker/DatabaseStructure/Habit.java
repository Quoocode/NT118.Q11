package com.example.habittracker.DatabaseStructure;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;

public class Habit {
    @DocumentId
    private String habitId;
    private String title;
    private String description;
    private String icon;
    private String color;
    private String reminderTime;
    private Timestamp startDate;
    private boolean isArchived;
    private int targetValue;

    // Empty Constructor (Required)
    public Habit() {}

    // Constructor for creating new habits
    public Habit(String title, String description, int targetValue, String unit) {
        this.title = title;
        this.description = description;
        this.targetValue = targetValue;
        this.icon = "ic_default";
        this.color = "#FF5733";
        this.isArchived = false;
        this.startDate = Timestamp.now();
    }

    // Getters and Setters...
    public String getHabitId() { return habitId; }
    public void setHabitId(String habitId) { this.habitId = habitId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public int getTargetValue() { return targetValue; }
    public void setTargetValue(int targetValue) { this.targetValue = targetValue; }

}