package com.example.habittracker.data.repository;

import android.util.Log;
import android.widget.Toast;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// Import các Model mới
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.model.HabitHistory;
import com.example.habittracker.data.model.HabitDailyView;

// Import Callbacks
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.callback.StatsCallback;

public class HabitRepository {

    private static final String TAG = "HabitRepository";
    private final FirebaseFirestore db;
    private final String userId;

    private final CollectionReference habitsRef;
    private final CollectionReference historyRef;

    public HabitRepository(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID must not be null or empty. Authentication required.");
        }
        this.userId = userId;

        this.db = FirebaseFirestore.getInstance();
        String userPath = "users/" + this.userId;

        // Mô hình 2 Collection Phẳng (Flat)
        this.habitsRef = db.collection(userPath + "/habits");
        this.historyRef = db.collection(userPath + "/history");
    }

    // =================================================================
    // 1. DASHBOARD LOGIC (QUAN TRỌNG NHẤT)
    // =================================================================

    /**
     * Lấy danh sách Habit và History cho một ngày cụ thể.
     * Kết hợp dữ liệu từ 2 collection để tạo ra List<HabitDailyView>.
     */
    public void getHabitsAndHistoryForDate(Calendar date, HabitQueryCallback callback) {
        Timestamp targetDateTs = getStartOfDayTimestamp(date);
        Log.d(TAG, "Tải Dashboard cho ngày: " + targetDateTs.toDate());

        // B1: Lấy Habit (Chưa lưu trữ)
        habitsRef.whereEqualTo("archived", false)
                .get()
                .addOnSuccessListener(habitSnapshots -> {
                    List<Habit> scheduledHabits = new ArrayList<>();

                    // B2: Lọc theo Frequency (Client-side logic)
                    for (DocumentSnapshot doc : habitSnapshots) {
                        Habit habit = doc.toObject(Habit.class);
                        if (habit != null) {
                            habit.setId(doc.getId());
                            if (isHabitScheduledForDay(habit, date)) {
                                scheduledHabits.add(habit);
                            }
                        }
                    }

                    if (scheduledHabits.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    // B3: Lấy History (Trạng thái) của ngày đó
                    historyRef.whereEqualTo("date", targetDateTs)
                            .get()
                            .addOnSuccessListener(historySnapshots -> {
                                // Map để tra cứu nhanh: HabitId -> History
                                Map<String, HabitHistory> historyMap = new HashMap<>();
                                for (DocumentSnapshot doc : historySnapshots) {
                                    HabitHistory history = doc.toObject(HabitHistory.class);
                                    if (history != null) {
                                        history.setId(doc.getId());
                                        historyMap.put(history.getHabitId(), history);
                                    }
                                }

                                // B4: Ghép Habit + History -> HabitDailyView
                                List<HabitDailyView> dailyViews = new ArrayList<>();
                                for (Habit habit : scheduledHabits) {
                                    HabitHistory history = historyMap.get(habit.getId());

                                    String status = "PENDING";
                                    String historyId = null;
                                    double currentValue = 0;

                                    if (history != null) {
                                        status = history.getStatus();
                                        historyId = history.getId();
                                        currentValue = history.getValue();
                                    } else {
                                        // Tạo sẵn ID dự kiến: habitId_yyyy-MM-dd
                                        historyId = generateHistoryId(habit.getId(), date);
                                    }

                                    // Tạo View Model (Sử dụng Setters để an toàn)
                                    HabitDailyView view = new HabitDailyView();
                                    view.setHabitId(habit.getId());
                                    view.setLogId(historyId); // Quan trọng để update status

                                    // Thông tin hiển thị
                                    view.setTitle(habit.getTitle());
                                    view.setDescription(habit.getDescription());
                                    view.setIconName(habit.getIconName());
                                    view.setUnit(habit.getUnit());
                                    view.setTargetValue(habit.getTargetValue());

                                    // Thông tin trạng thái
                                    view.setCurrentValue(currentValue);
                                    view.setStatus(status);

                                    dailyViews.add(view);
                                }
                                callback.onSuccess(dailyViews);
                            })
                            .addOnFailureListener(e -> callback.onFailure(e));

                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // =================================================================
    // 2. CHECK-IN / UPDATE STATUS
    // =================================================================

    /**
     * Cập nhật trạng thái History (Done/Pending)
     */
    public void updateHistoryStatus(String habitId, Calendar date, String newStatus, double value, SimpleCallback callback) {
        String historyId = generateHistoryId(habitId, date);
        Timestamp dateTs = getStartOfDayTimestamp(date);

        Map<String, Object> data = new HashMap<>();
        data.put("habitId", habitId);
        data.put("date", dateTs);
        data.put("status", newStatus);
        data.put("value", value);
        data.put("completedTime", Timestamp.now());

        // Dùng merge() để tự động Tạo mới hoặc Cập nhật
        historyRef.document(historyId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Check-in thành công: " + newStatus);
                    callback.onComplete(true, null);
                })
                .addOnFailureListener(e -> callback.onComplete(false, e));
    }

    // =================================================================
    // 3. CRUD HABIT (THÊM - SỬA - XÓA)
    // =================================================================

    public void addHabit(Habit habit, SimpleCallback callback) {
        habitsRef.add(habit)
                .addOnSuccessListener(doc -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e));
    }

    public void updateHabit(Habit habit, SimpleCallback callback) {
        if (habit.getId() == null) return;
        habitsRef.document(habit.getId())
                .set(habit, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e));
    }

    // Soft Delete (Lưu trữ)
    public void archiveHabit(String habitId, SimpleCallback callback) {
        habitsRef.document(habitId)
                .update("archived", true) // Đánh dấu là đã lưu trữ
                .addOnSuccessListener(aVoid -> callback.onComplete(true, null))
                .addOnFailureListener(e -> callback.onComplete(false, e));
    }

    public void getHabitById(String habitId, HabitQueryCallback callback) {
        habitsRef.document(habitId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Habit habit = doc.toObject(Habit.class);
                habit.setId(doc.getId());
                List<Habit> list = new ArrayList<>();
                list.add(habit);
                callback.onSuccess(list);
            } else {
                callback.onFailure(new Exception("Không tìm thấy Habit"));
            }
        }).addOnFailureListener(e -> callback.onFailure(e));
    }

    // =================================================================
    // 4. CÁC HÀM TIỆN ÍCH (HELPER)
    // =================================================================

    /**
     * Tạo ID duy nhất cho History: habitId_yyyy-MM-dd
     */
    private String generateHistoryId(String habitId, Calendar date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateStr = sdf.format(date.getTime());
        return habitId + "_" + dateStr;
    }

    /**
     * Lấy Timestamp 00:00:00 (theo giờ địa phương)
     */
    private Timestamp getStartOfDayTimestamp(Calendar calendar) {
        Calendar cal = (Calendar) calendar.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return new Timestamp(cal.getTime());
    }

    /**
     * Logic kiểm tra Frequency (ĐÃ CẬP NHẬT DAILY THEO YÊU CẦU)
     */
    private boolean isHabitScheduledForDay(Habit habit, Calendar targetDay) {
        Map<String, Object> freq = habit.getFrequency();
        if (freq == null) return false;

        String type = (String) freq.get("type");
        Timestamp startTs = habit.getStartDate();
        if (startTs == null) return false;

        // 1. Lấy ngày bắt đầu (Local Time)
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startTs.toDate());

        // 2. Chuẩn hóa về 00:00:00 để tính khoảng cách ngày chính xác
        long startMillis = getStartOfDayTimestamp(startCal).toDate().getTime();
        long targetMillis = getStartOfDayTimestamp(targetDay).toDate().getTime();

        // Nếu ngày mục tiêu < ngày bắt đầu -> Không hiện
        if (targetMillis < startMillis) {
            return false;
        }

        // Tính số ngày chênh lệch
        long diffMillis = targetMillis - startMillis;
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

        switch (type) {
            case "ONCE":
                // Chỉ hiện đúng vào ngày bắt đầu (chênh lệch = 0)
                return diffDays == 0;

            case "DAILY":
                // CẬP NHẬT: Hiện tất cả các ngày trong CÙNG THÁNG và CÙNG NĂM của ngày bắt đầu
                // (Tính từ ngày bắt đầu trở đi, đã check ở trên)
                return startCal.get(Calendar.YEAR) == targetDay.get(Calendar.YEAR) &&
                        startCal.get(Calendar.MONTH) == targetDay.get(Calendar.MONTH);

            case "WEEKLY":
                // Hiện vào đúng thứ đó hàng tuần (cách nhau 7, 14, 21... ngày)
                // Phép chia lấy dư cho 7 phải bằng 0
                return (diffDays % 7) == 0;

            case "MONTHLY":
                // Hiện vào đúng NGÀY ĐÓ của tháng sau
                // Ví dụ: Bắt đầu ngày 15/1, thì chỉ hiện vào 15/2, 15/3...
                int startDayOfMonth = startCal.get(Calendar.DAY_OF_MONTH);
                int targetDayOfMonth = targetDay.get(Calendar.DAY_OF_MONTH);

                // Cách kiểm tra đơn giản nhất: Ngày trong tháng phải trùng nhau
                return startDayOfMonth == targetDayOfMonth;

            default:
                return false;
        }
    }
    // =================================================================
    // 5. STATISTICS
    // =================================================================



    /**
     * Lấy thống kê: Số lần hoàn thành trong Tuần này, Tháng này, và Tổng cộng.
     */
    public void getHabitStatistics(String habitId, StatsCallback callback) {
        // Lấy tất cả lịch sử đã hoàn thành của thói quen này
        historyRef.whereEqualTo("habitId", habitId)
                .whereEqualTo("status", "DONE") // Chỉ đếm những cái đã hoàn thành
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int weekCount = 0;
                    int monthCount = 0;
                    int totalCount = querySnapshot.size();

                    // Mốc thời gian để so sánh
                    Calendar now = Calendar.getInstance();

                    // Đầu tuần (Thứ 2)
                    Calendar startOfWeek = (Calendar) now.clone();
                    startOfWeek.setFirstDayOfWeek(Calendar.MONDAY);
                    startOfWeek.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    startOfWeek.set(Calendar.HOUR_OF_DAY, 0);
                    startOfWeek.set(Calendar.MINUTE, 0);
                    startOfWeek.set(Calendar.SECOND, 0);

                    // Đầu tháng (Ngày 1)
                    Calendar startOfMonth = (Calendar) now.clone();
                    startOfMonth.set(Calendar.DAY_OF_MONTH, 1);
                    startOfMonth.set(Calendar.HOUR_OF_DAY, 0);
                    startOfMonth.set(Calendar.MINUTE, 0);
                    startOfMonth.set(Calendar.SECOND, 0);

                    long weekMillis = startOfWeek.getTimeInMillis();
                    long monthMillis = startOfMonth.getTimeInMillis();

                    // Duyệt qua từng bản ghi để đếm
                    for (DocumentSnapshot doc : querySnapshot) {
                        Timestamp ts = doc.getTimestamp("date");
                        if (ts != null) {
                            long recordMillis = ts.toDate().getTime();

                            // Nếu ngày ghi nhận >= đầu tuần -> +1 cho tuần
                            if (recordMillis >= weekMillis) {
                                weekCount++;
                            }

                            // Nếu ngày ghi nhận >= đầu tháng -> +1 cho tháng
                            if (recordMillis >= monthMillis) {
                                monthCount++;
                            }
                        }
                    }

                    callback.onStatsLoaded(weekCount, monthCount, totalCount);
                })
                .addOnFailureListener(callback::onError);
    }
}