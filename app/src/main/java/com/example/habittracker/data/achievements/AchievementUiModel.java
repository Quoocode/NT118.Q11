package com.example.habittracker.data.achievements;

import androidx.annotation.DrawableRes;

public class AchievementUiModel {
    private final AchievementId id;
    private final String title;
    private final String description;
    @DrawableRes private final int iconRes;
    private final boolean unlocked;
    private final Long unlockedAtMillis; // nullable

    public AchievementUiModel(
            AchievementId id,
            String title,
            String description,
            int iconRes,
            boolean unlocked,
            Long unlockedAtMillis
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.unlocked = unlocked;
        this.unlockedAtMillis = unlockedAtMillis;
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

    public boolean isUnlocked() {
        return unlocked;
    }

    public Long getUnlockedAtMillis() {
        return unlockedAtMillis;
    }
}

