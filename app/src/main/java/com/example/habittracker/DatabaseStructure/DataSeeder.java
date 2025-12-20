package com.example.habittracker.DatabaseStructure;

import android.util.Log;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DataSeeder {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String targetUid; // ID của người mà chúng ta muốn nạp dữ liệu

    // --- SỬA ĐỔI Ở ĐÂY ---
    // Constructor nhận trực tiếp ID từ bên ngoài
    public DataSeeder(String targetUid) {
        this.targetUid = targetUid;
    }

    public void seedData() {
        if (targetUid == null || targetUid.isEmpty()) return;

        WriteBatch batch = db.batch();

        // ---------------------------------------------------------
        // 1. TẠO HABITS CHO USER NÀY
        // ---------------------------------------------------------

        // Habit 1: Uống nước
        String habitId1 = "habit_water_01";
        Habit h1 = new Habit("Uống nước", "2 lít mỗi ngày", 2000, "ml");
        h1.setHabitId(habitId1);
        h1.setColor("#3498db");
        h1.setIcon("ic_water_drop");
        // Ghi vào đúng đường dẫn của targetUid
        batch.set(db.collection("users").document(targetUid).collection("habits").document(habitId1), h1);

        // Habit 2: Tập Gym
        String habitId2 = "habit_gym_02";
        Habit h2 = new Habit("Tập Gym", "Đẩy tạ ngực", 45, "phút");
        h2.setHabitId(habitId2);
        h2.setColor("#e74c3c");
        h2.setIcon("ic_fitness_center");
        batch.set(db.collection("users").document(targetUid).collection("habits").document(habitId2), h2);

        // ---------------------------------------------------------
        // 2. TẠO HISTORY (CHECK-IN)
        // ---------------------------------------------------------
        String todayStr = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        // Check-in cho Habit 1
        String histId1 = habitId1 + "_" + todayStr;
        History hist1 = new History();
        hist1.setHistoryId(histId1);
        hist1.setHabitId(habitId1);
        hist1.setUserId(targetUid); // Gán đúng ID user
        hist1.setValue(500);
        hist1.setNotes("Uống lúc sáng sớm");
        hist1.setDate(Timestamp.now());

        batch.set(db.collection("users").document(targetUid).collection("history").document(histId1), hist1);

        // ---------------------------------------------------------
        // THỰC THI
        // ---------------------------------------------------------
        batch.commit().addOnSuccessListener(aVoid -> {
            Log.d("DataSeeder", "Đã nạp xong dữ liệu cho User: " + targetUid);
        }).addOnFailureListener(e -> {
            Log.e("DataSeeder", "Lỗi nạp dữ liệu: " + e.getMessage());
        });
    }
}