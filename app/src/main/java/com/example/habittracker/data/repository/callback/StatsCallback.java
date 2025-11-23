package com.example.habittracker.data.repository.callback;

// Callback riêng cho thống kê
public interface StatsCallback {
    void onStatsLoaded(int week, int month, int total);
    void onError(Exception e);
}
