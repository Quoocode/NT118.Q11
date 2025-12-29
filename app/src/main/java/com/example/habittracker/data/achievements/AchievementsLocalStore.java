package com.example.habittracker.data.achievements;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lưu trữ cục bộ cho thành tựu (thuần Java), an toàn khi truy cập đồng thời (synchronized).
 *
 * Lưu ý: File này dùng SharedPreferences + apply() (bất đồng bộ) để đảm bảo ghi an toàn
 * mà vẫn giữ dự án thuần Java.
 */
public class AchievementsLocalStore {

    private static final String PREFS = "achievements_local";

    // Cửa sổ khử trùng lặp cho sự kiện "mở app" để tránh làm sai điều kiện "Welcome Back"
    // khi cùng một lần mở app bị gọi recordOpen 2 lần (ví dụ: lifecycle + auth listener).
    // 2 giây đủ để bao phủ các callback trùng nhưng không ảnh hưởng tới lần mở thật sự.
    private static final long OPEN_DEDUP_WINDOW_MS = 2_000L;

    public static String safeUser(String userIdOrNull) {
        String v = userIdOrNull == null ? "anon" : userIdOrNull.trim();
        return v.isEmpty() ? "anon" : v;
    }

    private static String keyUnlocked(String uid) { return "unlocked_ids_" + uid; }
    private static String keyUnlockedAt(String uid, AchievementId id) { return "unlocked_at_" + uid + "_" + id.name(); }
    private static String keyHabitsCreated(String uid) { return "habits_created_" + uid; }
    private static String keyPerfectDays(String uid) { return "perfect_days_" + uid; }
    private static String keyLastOpen(String uid) { return "last_open_" + uid; }

    private final SharedPreferences sp;
    private final String uid;

    public AchievementsLocalStore(@NonNull Context context, String userIdOrNull) {
        Context app = context.getApplicationContext();
        this.sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.uid = safeUser(userIdOrNull);
    }

    public synchronized Set<String> getUnlockedIds() {
        Set<String> s = sp.getStringSet(keyUnlocked(uid), null);
        if (s == null) return Collections.emptySet();
        return new HashSet<>(s);
    }

    public synchronized Map<String, Long> getUnlockedAtMap() {
        Map<String, Long> map = new HashMap<>();
        for (AchievementDefinition def : AchievementCatalog.all()) {
            long ts = sp.getLong(keyUnlockedAt(uid, def.getId()), -1L);
            if (ts > 0) map.put(def.getId().name(), ts);
        }
        return map;
    }

    public synchronized void unlock(AchievementId id, long unlockedAtMillis) {
        // Luôn copy sang set có thể sửa (getUnlockedIds() có thể trả về Collections.emptySet()).
        Set<String> unlocked = new HashSet<>(getUnlockedIds());
        if (unlocked.contains(id.name())) return;

        unlocked.add(id.name());

        // Quan trọng: luôn ghi một instance Set MỚI vào SharedPreferences.
        sp.edit()
                .putStringSet(keyUnlocked(uid), new HashSet<>(unlocked))
                .putLong(keyUnlockedAt(uid, id), unlockedAtMillis)
                .apply();
    }

    public synchronized int incrementHabitsCreated() {
        int current = sp.getInt(keyHabitsCreated(uid), 0);
        int next = current + 1;
        sp.edit().putInt(keyHabitsCreated(uid), next).apply();
        return next;
    }

    public synchronized int getHabitsCreated() {
        return sp.getInt(keyHabitsCreated(uid), 0);
    }

    public synchronized Set<String> getPerfectDays() {
        Set<String> s = sp.getStringSet(keyPerfectDays(uid), null);
        if (s == null) return Collections.emptySet();
        return new HashSet<>(s);
    }

    public synchronized int addPerfectDayKey(String dayKey) {
        // Luôn copy sang set có thể sửa (getPerfectDays() có thể trả về Collections.emptySet()).
        Set<String> days = new HashSet<>(getPerfectDays());
        days.add(dayKey);
        sp.edit().putStringSet(keyPerfectDays(uid), new HashSet<>(days)).apply();
        return days.size();
    }

    /**
     * Trả về true nếu lần mở app này đủ điều kiện "welcome back".
     */
    public synchronized boolean recordOpenAndCheckWelcomeBack(long nowMillis) {
        long last = sp.getLong(keyLastOpen(uid), -1L);

        // Nếu bị gọi 2 lần trong thời gian rất ngắn, xem như cùng một sự kiện "mở app".
        // Quan trọng: KHÔNG ghi đè last_open trong trường hợp này,
        // nếu không lần gọi thứ 2 sẽ xoá timestamp cũ và làm mất khả năng phát hiện "Welcome Back".
        if (last > 0 && (nowMillis - last) >= 0 && (nowMillis - last) <= OPEN_DEDUP_WINDOW_MS) {
            return false;
        }

        sp.edit().putLong(keyLastOpen(uid), nowMillis).apply();
        if (last <= 0) return false;
        return (nowMillis - last) >= 24L * 60L * 60L * 1000L;
    }

    SharedPreferences getSharedPreferences() {
        return sp;
    }

    String getUserScopedPrefix() {
        return uid;
    }
}
