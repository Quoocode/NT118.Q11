package com.example.habittracker.ui.ViewModel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.DataCallback;
import com.example.habittracker.utils.NotificationHelper; // [MỚI] Import Helper
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.List;

public class HabitViewModel extends AndroidViewModel {

    // MutableLiveData: Loại dữ liệu có thể thay đổi (set/post value)
    private final MutableLiveData<List<Habit>> habitsLiveData = new MutableLiveData<>();

    // Repository không khởi tạo ngay trong Constructor nữa vì cần userId
    private HabitRepository repository;

    public HabitViewModel(@NonNull Application application) {
        super(application);
    }

    // Hàm này trả về LiveData (dữ liệu chỉ đọc) để MainActivity quan sát
    public LiveData<List<Habit>> getHabits() {
        return habitsLiveData;
    }

    // --- HÀM QUAN TRỌNG: Kích hoạt tải dữ liệu ---
    public void loadActiveHabits() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("ALARM_DEBUG", "ViewModel: Không thể tải habits vì chưa đăng nhập (User NULL)");
            return; // Thoát nếu chưa đăng nhập
        }

        String userId = currentUser.getUid();

        // Khởi tạo Repository với UserId hiện tại
        repository = new HabitRepository(userId);

        Log.d("ALARM_DEBUG", "ViewModel: Bắt đầu gọi Repository lấy danh sách Active Habits...");

        // Gọi hàm Callback của bạn
        repository.getActiveHabits(new DataCallback<List<Habit>>() {
            @Override
            public void onSuccess(List<Habit> data) {
                Log.d("ALARM_DEBUG", "ViewModel: Tải thành công " + data.size() + " habits.");
                // Đẩy dữ liệu vào LiveData -> MainActivity sẽ nhận được ở hàm onChanged
                habitsLiveData.postValue(data);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ALARM_DEBUG", "ViewModel: Lỗi tải habits: " + e.getMessage());
                // Có thể post null hoặc list rỗng tùy logic xử lý lỗi
                habitsLiveData.postValue(null);
            }
        });
    }

    // =========================================================================
    // [MỚI] HÀM XỬ LÝ CHECK/UNCHECK THÓI QUEN -> KÍCH HOẠT SỬA BÁO THỨC
    // =========================================================================
    public void updateHabitStatus(Habit habit, boolean isCompleted) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Đảm bảo Repository đã được khởi tạo
        if (repository == null) {
            repository = new HabitRepository(currentUser.getUid());
        }

        Log.d("ALARM_DEBUG", "ViewModel: User đổi trạng thái habit: " + habit.getTitle() + " -> " + isCompleted);

        // Gọi Repository để update trạng thái lên Firestore
        // LƯU Ý: Giả định HabitRepository của bạn có hàm updateHabitHistory hoặc tương tự.
        // Bạn cần đảm bảo hàm này tồn tại trong Repository.
        repository.updateHabitHistory(habit, isCompleted, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                Log.d("ALARM_DEBUG", "DB Update Success -> Bắt đầu điều chỉnh báo thức...");

                // [TRIGGER QUAN TRỌNG]
                // Gọi NotificationHelper để tính toán lại báo thức (Dời sang ngày mai hoặc đặt lại hôm nay)
                NotificationHelper.updateAlarmBasedOnStatus(getApplication(), habit, isCompleted);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ALARM_DEBUG", "Lỗi update DB, KHÔNG thay đổi báo thức: " + e.getMessage());
                // Nếu update DB lỗi, ta không làm gì với báo thức để đảm bảo tính nhất quán.
            }
        });
    }

    // =========================================================================
    // [MỚI] HÀM DÙNG CHO CHECK-IN DIALOG (Xử lý cả DB và Báo thức)
    // =========================================================================
    public void performCheckIn(String habitId, double newValue, String newStatus, final DataCallback<Boolean> uiCallback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        if (repository == null) {
            repository = new HabitRepository(currentUser.getUid());
        }

        // 1. Cập nhật Database (Value + Status)
        repository.updateHistoryStatus(habitId, java.util.Calendar.getInstance(), newStatus, newValue, (success, e) -> {
            if (success) {
                // 2. NẾU DB THÀNH CÔNG -> Tải thông tin Habit để lấy giờ báo thức/tần suất
                repository.getHabitById(habitId, new DataCallback<Habit>() {
                    @Override
                    public void onSuccess(Habit habit) {
                        // 3. Gọi NotificationHelper để điều chỉnh báo thức
                        boolean isCompleted = "DONE".equals(newStatus);
                        NotificationHelper.updateAlarmBasedOnStatus(getApplication(), habit, isCompleted);

                        // Báo về UI là xong rồi
                        uiCallback.onSuccess(true);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        // Vẫn báo success cho UI vì DB đã lưu rồi, lỗi Alarm không nên chặn UX
                        Log.e("ALARM_DEBUG", "Không thể tải habit để update alarm: " + e.getMessage());
                        uiCallback.onSuccess(true);
                    }
                });
            } else {
                uiCallback.onFailure(e);
            }
        });
    }
}