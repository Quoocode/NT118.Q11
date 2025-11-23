package com.example.habittracker.data.repository.callback;

public interface StreakCallback {
    void onStreakCalculated(int currentStreak, int longestStreak);
    void onFailure(Exception e);
}