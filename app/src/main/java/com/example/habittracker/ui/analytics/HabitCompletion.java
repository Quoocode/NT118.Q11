package com.example.habittracker.ui.analytics;

public class HabitCompletion {
    public enum Status {
        COMPLETED,
        MISSED,
        PENDING
    }

    private final String name;
    private final Status status;

    public HabitCompletion(String name, Status status) {
        this.name = name;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }
}

