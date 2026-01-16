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
 * ViewModel (thuần Java) cho màn hình Thành tựu (Achievements).
 *
 * Dùng repository dựa trên SharedPreferences và tự làm mới dữ liệu thông qua
 * listener lắng nghe thay đổi SharedPreferences.
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

            // Chỉ phản ứng với các key theo phạm vi user; an toàn nếu có nhiều user dùng chung app.
            if (key.contains(uid)) {
                refresh();
            }
        };

        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
        refresh();
    }

    public LiveData<List<AchievementUiModel>> getAchievements() {
        return achievements;
    }

    public void refresh() {
        if (isRefreshing) return;
        isRefreshing = true;
        try {
            achievements.postValue(AchievementsUiMapper.toUi(getApplication(), repo.getUnlockedIds(), repo.getUnlockedAt()));
        } catch (Exception ignored) {
            // Nếu SharedPreferences bị lỗi (ví dụ sai kiểu StringSet), không để app crash
            achievements.postValue(java.util.Collections.emptyList());
        } finally {
            isRefreshing = false;
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Hủy đăng ký listener khi ViewModel bị hủy để tránh rò rỉ memory leak.
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }
}
