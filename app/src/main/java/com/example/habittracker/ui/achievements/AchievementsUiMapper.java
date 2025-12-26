package com.example.habittracker.ui.achievements;

import com.example.habittracker.data.achievements.AchievementCatalog;
import com.example.habittracker.data.achievements.AchievementDefinition;
import com.example.habittracker.data.achievements.AchievementUiModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AchievementsUiMapper {

    private AchievementsUiMapper() {}

    static List<AchievementUiModel> toUi(Set<String> unlockedIds, Map<String, Long> unlockedAt) {
        List<AchievementUiModel> ui = new ArrayList<>();

        for (AchievementDefinition def : AchievementCatalog.all()) {
            boolean unlocked = unlockedIds != null && unlockedIds.contains(def.getId().name());
            Long ts = unlockedAt == null ? null : unlockedAt.get(def.getId().name());
            ui.add(new AchievementUiModel(
                    def.getId(),
                    def.getTitle(),
                    def.getDescription(),
                    def.getIconRes(),
                    unlocked,
                    ts
            ));
        }

        Collections.sort(ui, new Comparator<AchievementUiModel>() {
            @Override
            public int compare(AchievementUiModel a, AchievementUiModel b) {
                if (a.isUnlocked() != b.isUnlocked()) return a.isUnlocked() ? -1 : 1;
                return a.getTitle().compareToIgnoreCase(b.getTitle());
            }
        });

        return ui;
    }
}

