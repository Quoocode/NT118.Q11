package com.example.habittracker.data.model;

public class HabitStats {
    public int thisWeekCount;
    public int thisMonthCount;
    public int totalCount;

    public HabitStats(int week, int month, int total) {
        this.thisWeekCount = week;
        this.thisMonthCount = month;
        this.totalCount = total;
    }
}