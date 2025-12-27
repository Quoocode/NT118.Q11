package com.example.habittracker.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;

// Import Callbacks cũ
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.callback.StatsCallback;
import com.example.habittracker.data.repository.callback.StreakCallback;
// Import Callback mới
import com.example.habittracker.data.repository.callback.DataCallback;

// Import Models
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.model.HabitHistory;
import com.example.habittracker.data.model.HabitDailyView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    // CÁC HÀM MỚI (OVERLOAD ĐỂ HỖ TRỢ REMINDER)
    // =================================================================

    public void getActiveHabits(final DataCallback<List<Habit>> callback) {
        if (userId == null) return;

        habitsRef.whereEqualTo("archived", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Habit> habitList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Habit habit = document.toObject(Habit.class);
                        habit.setId(document.getId());
                        habitList.add(habit);
                    }
                    callback.onSuccess(habitList);
                })
                .addOnFailureListener(callback::onFailure);
    }
    // 1. ADD MỚI: Trả về String ID (Quan trọng nhất cho Giai đoạn 1)
    public void addHabit(Habit habit, DataCallback<String> callback) {
        habitsRef.add(habit)
                .addOnSuccessListener(documentReference -> {
                    String newId = documentReference.getId();
                    // Gán ID vào object để UI dùng ngay
                    habit.setId(newId);
                    // Trả ID về
                    callback.onSuccess(newId);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // 2. UPDATE MỚI: Dùng DataCallback cho đồng bộ
    public void updateHabit(Habit habit, DataCallback<Boolean> callback) {
        if (habit.getId() == null) {
            callback.onFailure(new Exception("Habit ID missing"));
            return;
        }
        habitsRef.document(habit.getId())
                .set(habit, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // 3. DELETE MỚI (Chuẩn bị cho tương lai)
    public void deleteHabit(String habitId, DataCallback<Boolean> callback) {
        if (habitId == null) {
            callback.onFailure(new Exception("Habit ID missing"));
            return;
        }
        habitsRef.document(habitId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // 4. GET BY ID MỚI
    public void getHabitById(String habitId, DataCallback<Habit> callback) {
        habitsRef.document(habitId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Habit habit = doc.toObject(Habit.class);
                if (habit != null) {
                    habit.setId(doc.getId());
                    callback.onSuccess(habit);
                }
            } else {
                callback.onFailure(new Exception("Habit not found"));
            }
        }).addOnFailureListener(callback::onFailure);
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

    // =================================================================
    // 6. CHART DATA (MỚI THÊM)
    // =================================================================

    // Lấy danh sách lịch sử hoàn thành từ ngày startDate đến nay
    public void getHistoryForChart(String habitId, long startDateMillis, HabitQueryCallback callback) {
        // Chuyển long thành Timestamp của Firestore
        Timestamp startTs = new Timestamp(new java.util.Date(startDateMillis));

        historyRef.whereEqualTo("habitId", habitId)
                .whereGreaterThanOrEqualTo("date", startTs) // Lấy từ ngày bắt đầu
                .orderBy("date") // Sắp xếp ngày tăng dần để vẽ cho đúng
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<HabitHistory> historyList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        // Lọc những cái đã hoàn thành
                        String status = doc.getString("status");
                        if (status != null && (status.equalsIgnoreCase("DONE") || status.equalsIgnoreCase("COMPLETED"))) {
                            HabitHistory history = doc.toObject(HabitHistory.class);
                            if (history != null) {
                                historyList.add(history);
                            }
                        }
                    }
                    callback.onSuccess(historyList);
                })
                .addOnFailureListener(e -> callback.onFailure(e));
    }

    // =================================================================
    // 7. USER STREAK (GLOBAL STREAK)
    // Tính toán dựa trên TOÀN BỘ lịch sử hoạt động của User
    // =================================================================
    public void calculateUserStreaks(StreakCallback callback) {
        // CHÚ Ý: Không lọc theo habitId nữa, chỉ lấy status = DONE
        historyRef.whereEqualTo("status", "DONE")
                .orderBy("date") // Sắp xếp tăng dần
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    // 1. Lấy danh sách ngày ĐỘC NHẤT (Unique Dates)
                    // Dùng HashSet để tự động loại bỏ các ngày trùng nhau
                    // Ví dụ: Làm 3 habit trong 1 ngày -> Set chỉ lưu 1 ngày đó thôi
                    HashSet<String> uniqueDates = new HashSet<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                    for (DocumentSnapshot doc : querySnapshot) {
                        Timestamp ts = doc.getTimestamp("date");
                        if (ts != null) {
                            String dateStr = sdf.format(ts.toDate());
                            uniqueDates.add(dateStr);
                        }
                    }

                    // Chuyển Set sang List và SẮP XẾP LẠI (Quan trọng vì HashSet không có thứ tự)
                    List<String> sortedDateList = new ArrayList<>(uniqueDates);
                    Collections.sort(sortedDateList);

                    // 2. Thuật toán tính Streak (Giữ nguyên như trước)
                    int currentStreak = 0;
                    int longestStreak = 0;
                    int tempStreak = 0;

                    // A. Tính Longest Streak
                    for (int i = 0; i < sortedDateList.size(); i++) {
                        if (i == 0) {
                            tempStreak = 1;
                        } else {
                            long diff = getDayDifference(sortedDateList.get(i - 1), sortedDateList.get(i));
                            if (diff == 1) {
                                tempStreak++;
                            } else {
                                tempStreak = 1;
                            }
                        }
                        if (tempStreak > longestStreak) {
                            longestStreak = tempStreak;
                        }
                    }

                    // B. Tính Current Streak
                    if (!sortedDateList.isEmpty()) {
                        String lastDateStr = sortedDateList.get(sortedDateList.size() - 1);
                        String todayStr = sdf.format(new java.util.Date());

                        long diffFromToday = getDayDifference(lastDateStr, todayStr);

                        // Nếu ngày cuối cùng làm là Hôm nay (0) hoặc Hôm qua (1) -> Chuỗi còn sống
                        if (diffFromToday <= 1) {
                            currentStreak = 1;
                            for (int i = sortedDateList.size() - 1; i > 0; i--) {
                                long diff = getDayDifference(sortedDateList.get(i - 1), sortedDateList.get(i));
                                if (diff == 1) {
                                    currentStreak++;
                                } else {
                                    break;
                                }
                            }
                        } else {
                            currentStreak = 0; // Đã lười quá 1 ngày -> Mất chuỗi
                        }
                    }

                    callback.onStreakCalculated(currentStreak, longestStreak);
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateHabitHistory(Habit habit, boolean isCompleted, final DataCallback<Boolean> callback) {
        if (habit == null || habit.getId() == null) {
            callback.onFailure(new IllegalArgumentException("Habit không hợp lệ"));
            return;
        }

        // 1. Xác định ngày hôm nay
        Calendar today = Calendar.getInstance();

        // 2. Tạo ID cho lịch sử (habitId_yyyy-MM-dd)
        String historyId = generateHistoryId(habit.getId(), today);

        // 3. Chuẩn bị dữ liệu
        Map<String, Object> data = new HashMap<>();
        data.put("habitId", habit.getId());
        data.put("date", getStartOfDayTimestamp(today));

        if (isCompleted) {
            data.put("status", "COMPLETED");
            // Nếu habit có targetValue thì gán max, không thì gán 1
            data.put("value", habit.getTargetValue() > 0 ? habit.getTargetValue() : 1);
            data.put("completedAt", Timestamp.now());
        } else {
            data.put("status", "PENDING");
            data.put("value", 0);
            data.put("completedAt", null); // Xóa thời điểm hoàn thành
        }

        // 4. Ghi vào Firestore (Dùng SetOptions.merge để không ghi đè mất dữ liệu khác nếu có)
        historyRef.document(historyId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(true))
                .addOnFailureListener(callback::onFailure);
    }

    // Hàm phụ: Tính khoảng cách giữa 2 ngày (dạng String yyyy-MM-dd)
    private long getDayDifference(String date1, String date2) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            java.util.Date d1 = sdf.parse(date1);
            java.util.Date d2 = sdf.parse(date2);
            long diffInMillies = Math.abs(d2.getTime() - d1.getTime());
            return TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    // [MỚI - BƯỚC 1] HÀM KIỂM TRA TRẠNG THÁI HISTORY TRONG NGÀY
    // Dùng để quyết định có đặt báo thức cho hôm nay hay ngày mai khi Edit
    // =========================================================================
    public void getHabitHistoryStatus(String habitId, Calendar date, DataCallback<String> callback) {
        String historyId = generateHistoryId(habitId, date);

        historyRef.document(historyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        callback.onSuccess(status != null ? status : "PENDING");
                    } else {
                        // Chưa có bản ghi history -> Coi như chưa làm (PENDING)
                        callback.onSuccess("PENDING");
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

}