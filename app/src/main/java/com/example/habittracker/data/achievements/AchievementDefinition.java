package com.example.habittracker.data.achievements;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

public class AchievementDefinition {
    private final AchievementId id;
    @StringRes private final int titleRes;
    @StringRes private final int descriptionRes;
    @DrawableRes private final int iconRes;

    public AchievementDefinition(AchievementId id, @StringRes int titleRes, @StringRes int descriptionRes, @DrawableRes int iconRes) {
        this.id = id;
        this.titleRes = titleRes;
        this.descriptionRes = descriptionRes;
        this.iconRes = iconRes;
    }

    public AchievementId getId() {
        return id;
    }

    public int getTitleRes() {
        return titleRes;
    }

    public int getDescriptionRes() {
        return descriptionRes;
    }

    public int getIconRes() {
        return iconRes;
    }
}
