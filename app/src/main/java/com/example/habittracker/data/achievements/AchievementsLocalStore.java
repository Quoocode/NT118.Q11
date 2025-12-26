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
 * Java-only, async-safe local persistence for achievements.
 *
 * Note: This uses SharedPreferences + apply() (async) to satisfy "safer async" while keeping Java-only.
 * If you need DataStore specifically, we can keep a tiny Kotlin file for it, but you asked for Java-only.
 */
public class AchievementsLocalStore {

    private static final String PREFS = "achievements_local";

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
        // Always copy to a mutable set (getUnlockedIds() may return Collections.emptySet()).
        Set<String> unlocked = new HashSet<>(getUnlockedIds());
        if (unlocked.contains(id.name())) return;

        unlocked.add(id.name());

        // Important: always write a NEW set instance to SharedPreferences.
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
        // Always copy to a mutable set (getPerfectDays() may return Collections.emptySet()).
        Set<String> days = new HashSet<>(getPerfectDays());
        days.add(dayKey);
        sp.edit().putStringSet(keyPerfectDays(uid), new HashSet<>(days)).apply();
        return days.size();
    }

    /**
     * Returns true if this open qualifies as "welcome back".
     */
    public synchronized boolean recordOpenAndCheckWelcomeBack(long nowMillis) {
        long last = sp.getLong(keyLastOpen(uid), -1L);
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
