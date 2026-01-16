package com.example.habittracker.ui.achievements;

import android.content.Context;

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

    // Chuyển đổi dữ liệu thành tựu sang mô hình UI.
    // Sắp xếp sao cho thành tựu đã mở khóa hiển thị trước.
    // Tiêu đề được sắp xếp theo thứ tự bảng chữ cái.
    static List<AchievementUiModel> toUi(Context context, Set<String> unlockedIds, Map<String, Long> unlockedAt) {
        List<AchievementUiModel> ui = new ArrayList<>();

        for (AchievementDefinition def : AchievementCatalog.all()) {
            boolean unlocked = unlockedIds != null && unlockedIds.contains(def.getId().name());
            Long ts = unlockedAt == null ? null : unlockedAt.get(def.getId().name());

            String title = context.getString(def.getTitleRes());
            String desc = context.getString(def.getDescriptionRes());

            ui.add(new AchievementUiModel(
                    def.getId(),
                    title,
                    desc,
                    def.getIconRes(),
                    unlocked,
                    ts
            ));
        }

        // Sắp xếp: Thành tựu đã mở khóa lên trước, sau đó theo tiêu đề bảng chữ cái.
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
