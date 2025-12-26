package com.example.habittracker.data.achievements;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.StreakCallback;

import java.util.Calendar;
import java.util.List;

/**
 * Local rule engine that unlocks achievements based on app events.
 *
 * This intentionally uses local persistence only (DataStore) so achievements are per-install.
 */
public class AchievementService {

    private final Context appContext;
    private final AchievementsRepository repo;

    public AchievementService(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.repo = new AchievementsRepository(this.appContext);
    }

    public void onHabitCreated() {
        repo.incrementHabitsCreated();
        repo.unlock(AchievementId.FIRST_HABIT_CREATED);

        // Evaluate count-based achievements (3/7 habits) opportunistically.
        AchievementsRuleEvaluator.evaluateCounters(appContext);
    }

    public void onCheckInCommitted(@NonNull String newStatus, double value, double targetValue) {
        if (isDone(newStatus, value, targetValue)) {
            repo.unlock(AchievementId.FIRST_CHECKIN);
        }
    }

    /**
     * Called when today's list has been loaded.
     * We compute all-done locally from the same models you display.
     */
    public void onDaySnapshot(@NonNull Calendar date, @NonNull List<HabitDailyView> dayList) {
        if (dayList.isEmpty()) return;

        boolean allDone = true;
        for (HabitDailyView h : dayList) {
            if (!isDone(h.getStatus(), h.getCurrentValue(), h.getTargetValue())) {
                allDone = false;
                break;
            }
        }

        if (allDone) {
            repo.unlock(AchievementId.FIRST_DAY_ALL_DONE);
            repo.addPerfectDay(date.getTime());
        }

        // Also check streak-related achievements when we’re on a day snapshot.
        evaluateStreakAchievements();

        // Also check counter-based achievements.
        AchievementsRuleEvaluator.evaluateCounters(appContext);
    }

    public void evaluateStreakAchievements() {
        // Uses existing repo calculation (based on history) as the streak source.
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null || uid.isEmpty()) return;

        HabitRepository habitRepository = new HabitRepository(uid);
        habitRepository.calculateUserStreaks(new StreakCallback() {
            @Override
            public void onStreakCalculated(int currentStreak, int longestStreak) {
                if (currentStreak >= 3) repo.unlock(AchievementId.CHECKIN_STREAK_3);
                if (currentStreak >= 7) repo.unlock(AchievementId.CHECKIN_STREAK_7);
                if (longestStreak >= 7) repo.unlock(AchievementId.LONGEST_STREAK_7);
            }

            @Override
            public void onFailure(Exception e) {
                // ignore
            }
        });
    }

    private boolean isDone(String status, double value, double targetValue) {
        if (status != null) {
            if ("DONE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) return true;
        }
        // Fallback: for numeric habits treat reaching target as done.
        return targetValue > 0 && value >= targetValue;
    }
}
