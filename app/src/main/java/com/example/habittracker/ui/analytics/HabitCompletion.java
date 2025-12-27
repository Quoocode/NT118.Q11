package com.example.habittracker.ui.analytics;

import androidx.annotation.Nullable;

public class HabitCompletion {
    public enum Status {
        COMPLETED,
        MISSED,
        PENDING
    }

    @Nullable
    private final String habitId;
    private final String name;
    private final Status status;

    // Optional check-in metadata (for opening HabitCheckInDialogFragment)
    private final double targetValue;
    private final double currentValue;
    @Nullable
    private final String unit;
    @Nullable
    private final String rawStatus;

    public HabitCompletion(@Nullable String habitId,
                           String name,
                           Status status,
                           double targetValue,
                           double currentValue,
                           @Nullable String unit,
                           @Nullable String rawStatus) {
        this.habitId = habitId;
        this.name = name;
        this.status = status;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.unit = unit;
        this.rawStatus = rawStatus;
    }

    public HabitCompletion(@Nullable String habitId, String name, Status status) {
        this(habitId, name, status, 0d, 0d, null, null);
    }

    public HabitCompletion(String name, Status status) {
        this(null, name, status, 0d, 0d, null, null);
    }

    @Nullable
    public String getHabitId() {
        return habitId;
    }

    public String getName() {
        return name;
    }

    public Status getStatus() {
        return status;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    @Nullable
    public String getUnit() {
        return unit;
    }

    @Nullable
    public String getRawStatus() {
        return rawStatus;
    }
}
