package com.example.habittracker.data.achievements;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.StreakCallback;

import java.util.Calendar;
import java.util.List;

/**
 * Bộ luật cục bộ (rule engine) dùng để mở khóa thành tựu dựa trên các sự kiện trong app.
 *
 * Lưu ý: Cố tình chỉ dùng lưu trữ cục bộ để thành tựu mang tính "theo máy" (per-install).
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

        // Đánh giá các thành tựu theo số lượng (ví dụ 3/7 thói quen) một cách thuận tiện.
        AchievementsRuleEvaluator.evaluateCounters(appContext);
    }

    public void onCheckInCommitted(@NonNull String newStatus, double value, double targetValue) {
        if (isDone(newStatus, value, targetValue)) {
            repo.unlock(AchievementId.FIRST_CHECKIN);
        }
    }

    /**
     * Được gọi khi danh sách trong ngày đã tải xong.
     * Ta tính "tất cả đã hoàn thành" (all-done) dựa trên chính model đang hiển thị.
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

        // Đồng thời kiểm tra các thành tựu liên quan streak khi đã có snapshot theo ngày.
        evaluateStreakAchievements();

        // Đồng thời kiểm tra các thành tựu theo bộ đếm.
        AchievementsRuleEvaluator.evaluateCounters(appContext);
    }

    public void evaluateStreakAchievements() {
        // Dùng logic tính streak sẵn có (dựa trên lịch sử) làm nguồn dữ liệu streak.
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
                // Bỏ qua lỗi
            }
        });
    }

    private boolean isDone(String status, double value, double targetValue) {
        if (status != null) {
            if ("DONE".equalsIgnoreCase(status) || "COMPLETED".equalsIgnoreCase(status)) return true;
        }
        // Dự phòng: với thói quen dạng số, đạt target thì xem như hoàn thành.
        return targetValue > 0 && value >= targetValue;
    }
}
