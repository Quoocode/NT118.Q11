package com.example.habittracker.data.repository.callback;
import java.util.List;

/**
 * Interface callback để xử lý kết quả truy vấn dữ liệu bất đồng bộ từ Firestore.
 * Sử dụng List<?> để có thể trả về List<Habit>, List<HabitDailyView>, v.v.
 */
public interface HabitQueryCallback {

    // Khi thành công: Trả về một danh sách dữ liệu
    void onSuccess(List<?> result);

    // Khi thất bại: Trả về lỗi để xử lý (hiện Toast, Log...)
    void onFailure(Exception e);
}