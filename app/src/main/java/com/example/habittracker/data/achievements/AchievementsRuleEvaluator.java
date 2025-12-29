package com.example.habittracker.data.achievements;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Các kiểm tra thành tựu định kỳ/theo lô (batch) dựa trên các bộ đếm đã lưu.
 */
public final class AchievementsRuleEvaluator {

    private AchievementsRuleEvaluator() {}

    public static void evaluateCounters(@NonNull Context context) {
        AchievementsRepository repo = new AchievementsRepository(context.getApplicationContext());

        int created = repo.getHabitsCreated();
        if (created >= 3) repo.unlock(AchievementId.THREE_HABITS_CREATED);
        if (created >= 7) repo.unlock(AchievementId.SEVEN_HABITS_CREATED);

        int perfectDays = repo.getPerfectDays().size();
        if (perfectDays >= 3) repo.unlock(AchievementId.PERFECT_3_DAYS);
    }
}
