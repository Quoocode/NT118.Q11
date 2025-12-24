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
}