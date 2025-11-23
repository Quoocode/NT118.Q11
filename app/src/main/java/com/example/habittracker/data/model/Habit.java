package com.example.habittracker.data.model;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.Map;

@IgnoreExtraProperties
public class Habit {

    @DocumentId
    private String id; // habitId

    // --- Thông tin cơ bản ---
    private String title;       // Tên thói quen
    private String description; // Mô tả
    private String iconName;    // Tên file icon (VD: "ic_book")
    private String unit;        // Đơn vị (VD: "km", "trang", "phút")

    // --- Cấu hình lịch trình ---
    // Map chứa: type (ONCE/WEEKLY...), startDate (Timestamp), daysOfWeek (Array)
    private Map<String, Object> frequency;
    private String reminderTime; // Giờ nhắc (VD: "09:00")
    private Timestamp startDate; // Ngày bắt đầu chính thức

    // --- Mục tiêu & Thống kê ---
    private double targetValue;  // Mục tiêu định lượng (VD: 5 km)
    private int currentStreak;   // Chuỗi hiện tại (tính toán sau mỗi lần done)
    private int longestStreak;   // Chuỗi dài nhất

    // --- Trạng thái quản lý ---
    private boolean isArchived;  // Đã lưu trữ (ẩn khỏi Dashboard nhưng không xóa)
    private Timestamp createdAt; // Ngày tạo

    // Constructor rỗng (Bắt buộc)
    public Habit() {}

    // Constructor đầy đủ (để tạo mới)
    public Habit(String title, String description, String iconName, String unit,
                 Map<String, Object> frequency, String reminderTime, Timestamp startDate,
                 double targetValue) {
        this.title = title;
        this.description = description;
        this.iconName = iconName;
        this.unit = unit;
        this.frequency = frequency;
        this.reminderTime = reminderTime;
        this.startDate = startDate;
        this.targetValue = targetValue;

        // Giá trị mặc định khi tạo mới
        this.currentStreak = 0;
        this.longestStreak = 0;
        this.isArchived = false;
        this.createdAt = Timestamp.now();
    }

    // --- Getters & Setters ---
    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Map<String, Object> getFrequency() { return frequency; }
    public void setFrequency(Map<String, Object> frequency) { this.frequency = frequency; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) { this.targetValue = targetValue; }

    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }

    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}