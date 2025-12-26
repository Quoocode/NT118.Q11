package com.example.habittracker.data.achievements;

import com.example.habittracker.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Static list of all achievements shown in UI.
 *
 * Titles/descriptions are hardcoded for now (can be moved to string resources later).
 */
public final class AchievementCatalog {

    private AchievementCatalog() {}

    public static List<AchievementDefinition> all() {
        List<AchievementDefinition> list = new ArrayList<>();

        list.add(new AchievementDefinition(
                AchievementId.FIRST_HABIT_CREATED,
                "First Step",
                "Create your first habit.",
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.FIRST_CHECKIN,
                "First Check-in",
                "Complete (or reach target for) a habit for the first time.",
                R.drawable.ic_circle_check
        ));

        list.add(new AchievementDefinition(
                AchievementId.FIRST_DAY_ALL_DONE,
                "Perfect Day",
                "Finish all scheduled habits in a day.",
                R.drawable.ic_cup
        ));

        list.add(new AchievementDefinition(
                AchievementId.THREE_HABITS_CREATED,
                "Habit Builder",
                "Create 3 habits.",
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.SEVEN_HABITS_CREATED,
                "Habit Architect",
                "Create 7 habits.",
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.CHECKIN_STREAK_3,
                "On Fire (3)",
                "Reach a 3-day current streak.",
                R.drawable.ic_fire
        ));

        list.add(new AchievementDefinition(
                AchievementId.CHECKIN_STREAK_7,
                "On Fire (7)",
                "Reach a 7-day current streak.",
                R.drawable.ic_fire
        ));

        list.add(new AchievementDefinition(
                AchievementId.LONGEST_STREAK_7,
                "Legendary",
                "Reach 7 days as your longest streak.",
                R.drawable.ic_achiv_medal_gold_winner
        ));

        list.add(new AchievementDefinition(
                AchievementId.PERFECT_3_DAYS,
                "Three Perfect Days",
                "Finish all scheduled habits on 3 different days.",
                R.drawable.ic_achiv_medal_badge_reward
        ));

        list.add(new AchievementDefinition(
                AchievementId.WELCOME_BACK,
                "Welcome Back",
                "Open the app after being away for a day.",
                R.drawable.ic_sun
        ));

        return Collections.unmodifiableList(list);
    }
}

