package com.example.habittracker.data.achievements;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Java-only repository for unlocking + reading achievements locally.
 */
public class AchievementsRepository {

    private final Context appContext;

    public AchievementsRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    private String userId() {
        String uid = FirebaseAuth.getInstance().getUid();
        return uid == null ? "anon" : uid;
    }

    private AchievementsLocalStore store() {
        return new AchievementsLocalStore(appContext, userId());
    }

    public Set<String> getUnlockedIds() {
        return store().getUnlockedIds();
    }

    public Map<String, Long> getUnlockedAt() {
        return store().getUnlockedAtMap();
    }

    public int getHabitsCreated() {
        return store().getHabitsCreated();
    }

    public Set<String> getPerfectDays() {
        return store().getPerfectDays();
    }

    public void unlock(@NonNull AchievementId id) {
        store().unlock(id, System.currentTimeMillis());
    }

    public void unlock(@NonNull AchievementId id, long unlockedAtMillis) {
        store().unlock(id, unlockedAtMillis);
    }

    public int incrementHabitsCreated() {
        return store().incrementHabitsCreated();
    }

    public int addPerfectDay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return store().addPerfectDayKey(sdf.format(date));
    }

    public void recordAppOpenAndMaybeWelcomeBack() {
        long now = System.currentTimeMillis();
        boolean welcomeBack = store().recordOpenAndCheckWelcomeBack(now);
        if (welcomeBack) {
            unlock(AchievementId.WELCOME_BACK, now);
        }
    }
}
