package com.example.habittracker.ui.achievements;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.habittracker.data.achievements.AchievementUiModel;
import com.example.habittracker.data.achievements.AchievementsLocalStore;
import com.example.habittracker.data.achievements.AchievementsRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

/**
 * Java-only ViewModel for Achievements.
 *
 * Uses SharedPreferences-backed repository and refreshes automatically via a preference change listener.
 */
public class AchievementsViewModel extends AndroidViewModel {

    private final AchievementsRepository repo;
    private final MutableLiveData<List<AchievementUiModel>> achievements = new MutableLiveData<>();

    private final SharedPreferences sharedPreferences;
    private final String uid;

    private final SharedPreferences.OnSharedPreferenceChangeListener listener;

    private volatile boolean isRefreshing = false;

    public AchievementsViewModel(@NonNull Application application) {
        super(application);
        repo = new AchievementsRepository(application);

        String u = FirebaseAuth.getInstance().getUid();
        uid = AchievementsLocalStore.safeUser(u);

        sharedPreferences = application.getSharedPreferences("achievements_local", Application.MODE_PRIVATE);

        listener = (prefs, key) -> {
            if (key == null) return;
            if (uid == null || uid.isEmpty()) return;

            // Only react to our user-scoped keys; safe, but avoid storms.
            if (key.contains(uid)) {
                refresh();
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);

        // Welcome-back check is triggered from app lifecycle (MainActivity), not from this tab.

        refresh();
    }

    public LiveData<List<AchievementUiModel>> getAchievements() {
        return achievements;
    }

    public void refresh() {
        if (isRefreshing) return;
        isRefreshing = true;
        try {
            achievements.postValue(AchievementsUiMapper.toUi(repo.getUnlockedIds(), repo.getUnlockedAt()));
        } catch (Exception ignored) {
            // If prefs are corrupted (e.g., bad StringSet type), don't crash the app
            achievements.postValue(java.util.Collections.emptyList());
        } finally {
            isRefreshing = false;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
