package com.example.habittracker.data.model;
/**
 * Model View (tổng hợp) dùng để hiển thị trên Dashboard.
 * Nó KHÔNG phải là một collection trong Firestore.
 * Class này được tạo ra bằng cách ghép (join) Habit và HabitHistory.
 */
public class HabitDailyView {

    // --- ID tham chiếu ---
    private String habitId;      // ID của Habit gốc
    private String logId;        // ID của History log (để update status nhanh)

    // --- Thông tin hiển thị (Lấy từ Habit) ---
    private String title;        // Tên habit
    private String description;  // Mô tả
    private String iconName;     // Tên file icon (VD: "ic_book")
    private String unit;         // Đơn vị (VD: "km", "lần")
    private double targetValue;  // Mục tiêu (VD: 5.0)

    // --- Thông tin trạng thái (Lấy từ History) ---
    private double currentValue; // Đã làm được bao nhiêu (VD: 3.0)
    private String status;       // "PENDING", "DONE", "SKIPPED"

    // Constructor rỗng
    public HabitDailyView() {}

    // Constructor đầy đủ
    public HabitDailyView(String habitId, String logId, String title, String description,
                          String iconName, String unit, double targetValue,
                          double currentValue, String status) {
        this.habitId = habitId;
        this.logId = logId;
        this.title = title;
        this.description = description;
        this.iconName = iconName;
        this.unit = unit;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.status = status;
    }

    // --- Getters và Setters ---

    public String getHabitId() {
        return habitId;
    }

    public void setHabitId(String habitId) {
        this.habitId = habitId;
    }

    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconName() {
        return iconName != null ? iconName : "ic_launcher_foreground"; // Trả về mặc định nếu null
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Hàm tiện ích để tính % hoàn thành (cho ProgressBar)
    public int getProgressPercentage() {
        if (targetValue <= 0) return 0;
        int percent = (int) ((currentValue / targetValue) * 100);
        return Math.min(percent, 100); // Không vượt quá 100%
    }
}
