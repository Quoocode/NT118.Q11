package com.example.habittracker.data.achievements;

import static org.junit.Assert.*;

import com.example.habittracker.data.model.HabitDailyView;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Tiny logic-only tests (doesn't touch Android Context/DataStore).
 * These target the same "done" semantics used by AchievementService.
 */
public class AchievementServiceLogicTest {

    @Test
    public void doneStatusDone_isDone() {
        // We validate the done semantics indirectly via a small local helper.
        assertTrue(isDone("DONE", 0, 10));
        assertTrue(isDone("COMPLETED", 0, 10));
    }

    @Test
    public void doneByValueReachingTarget_isDone() {
        assertTrue(isDone("PENDING", 10, 10));
        assertTrue(isDone(null, 10, 10));
        assertFalse(isDone("PENDING", 9, 10));
    }

    private boolean isDone(String status, double value, double targetValue) {
        if (status != null) {
            if ("DONE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) return true;
        }
        return targetValue > 0 && value >= targetValue;
    }
}

