package com.example.habittracker.data.achievements;

import androidx.annotation.DrawableRes;

public class AchievementDefinition {
    private final AchievementId id;
    private final String title;
    private final String description;
    @DrawableRes private final int iconRes;

    public AchievementDefinition(AchievementId id, String title, String description, @DrawableRes int iconRes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
    }

    public AchievementId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconRes() {
        return iconRes;
    }
}

