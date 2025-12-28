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
                R.string.ach_first_step_title,
                R.string.ach_first_step_desc,
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.FIRST_CHECKIN,
                R.string.ach_first_checkin_title,
                R.string.ach_first_checkin_desc,
                R.drawable.ic_circle_check
        ));

        list.add(new AchievementDefinition(
                AchievementId.FIRST_DAY_ALL_DONE,
                R.string.ach_perfect_day_title,
                R.string.ach_perfect_day_desc,
                R.drawable.ic_cup
        ));

        list.add(new AchievementDefinition(
                AchievementId.THREE_HABITS_CREATED,
                R.string.ach_habit_builder_title,
                R.string.ach_habit_builder_desc,
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.SEVEN_HABITS_CREATED,
                R.string.ach_habit_architect_title,
                R.string.ach_habit_architect_desc,
                R.drawable.ic_plus
        ));

        list.add(new AchievementDefinition(
                AchievementId.CHECKIN_STREAK_3,
                R.string.ach_on_fire_3_title,
                R.string.ach_on_fire_3_desc,
                R.drawable.ic_fire
        ));

        list.add(new AchievementDefinition(
                AchievementId.CHECKIN_STREAK_7,
                R.string.ach_on_fire_7_title,
                R.string.ach_on_fire_7_desc,
                R.drawable.ic_fire
        ));

        list.add(new AchievementDefinition(
                AchievementId.LONGEST_STREAK_7,
                R.string.ach_legendary_title,
                R.string.ach_legendary_desc,
                R.drawable.ic_achiv_medal_gold_winner
        ));

        list.add(new AchievementDefinition(
                AchievementId.PERFECT_3_DAYS,
                R.string.ach_three_perfect_days_title,
                R.string.ach_three_perfect_days_desc,
                R.drawable.ic_achiv_medal_badge_reward
        ));

        list.add(new AchievementDefinition(
                AchievementId.WELCOME_BACK,
                R.string.ach_welcome_back_title,
                R.string.ach_welcome_back_desc,
                R.drawable.ic_sun
        ));

        return Collections.unmodifiableList(list);
    }
}
