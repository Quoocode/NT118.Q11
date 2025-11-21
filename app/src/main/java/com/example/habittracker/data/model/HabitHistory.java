package com.example.habittracker.data.model;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class HabitHistory {

    @DocumentId
    private String id; // ID:

    private String habitId;      // Link ngược về Habit gốc
    private Timestamp date;      // Ngày ghi nhận (00:00:00)

    private double value;        // Giá trị thực tế đã làm (VD: chạy được 3km / mục tiêu 5km)
    private String status;       // "DONE", "PENDING", "SKIPPED", "FAILED"
    private String notes;        // Ghi chú (VD: "Hôm nay chạy hơi mệt")

    private Timestamp completedTime; // Thời điểm bấm nút hoàn thành (lưu cả giờ phút giây)

    // Constructor rỗng (Bắt buộc)
    public HabitHistory() {}

    // Constructor để tạo log
    public HabitHistory(String habitId, Timestamp date, double value, String status, String notes) {
        this.habitId = habitId;
        this.date = date;
        this.value = value;
        this.status = status;
        this.notes = notes;
        this.completedTime = Timestamp.now();
    }

    // --- Getters & Setters ---
    @Exclude public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHabitId() { return habitId; }
    public void setHabitId(String habitId) { this.habitId = habitId; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getCompletedTime() { return completedTime; }
    public void setCompletedTime(Timestamp completedTime) { this.completedTime = completedTime; }
}
