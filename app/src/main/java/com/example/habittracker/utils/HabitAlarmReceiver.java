package com.example.habittracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class HabitAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "HabitAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1. Lấy dữ liệu từ gói tin (Intent)
        String habitTitle = intent.getStringExtra("HABIT_TITLE");
        String habitId = intent.getStringExtra("HABIT_ID");
        String frequencyType = intent.getStringExtra("HABIT_FREQ_TYPE");
        long startDateMillis = intent.getLongExtra("HABIT_START_DATE", 0);

        Log.d(TAG, "Đã nhận tín hiệu báo thức cho: " + habitTitle);

        if (habitTitle == null || habitId == null) {
            Log.e(TAG, "Thiếu dữ liệu habitTitle hoặc habitId!");
            return;
        }

        // 2. SMART CHECK: Kiểm tra xem hôm nay có cần nhắc không?
        if (shouldNotifyToday(frequencyType, startDateMillis)) {
            Log.d(TAG, ">> CHECK OK: Hôm nay đúng lịch -> Bắn thông báo!");

            // Gọi hàm hiển thị thông báo (Sẽ cập nhật NotificationHelper ở Giai đoạn 3)
            // Tạm thời comment lại để code không lỗi
            NotificationHelper.showHabitNotification(context, habitTitle, habitId);
        } else {
            Log.d(TAG, ">> CHECK FAIL: Hôm nay không phải lịch của thói quen này.");
        }
    }

    // Logic kiểm tra ngày (Copy logic từ Repository qua đây để Receiver tự xử lý)
    private boolean shouldNotifyToday(String frequencyType, long startDateMillis) {
        if (frequencyType == null) return true; // Mặc định báo nếu không có type

        Calendar today = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        startDate.setTimeInMillis(startDateMillis);

        // Reset giờ về 00:00:00 để tính khoảng cách ngày chính xác
        resetTime(today);
        resetTime(startDate);

        long diffMillis = today.getTimeInMillis() - startDate.getTimeInMillis();
        long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

        if (diffDays < 0) return false; // Chưa đến ngày bắt đầu

        switch (frequencyType) {
            case "DAILY":
                return true; // Luôn đúng

            case "WEEKLY":
                // Kiểm tra xem hôm nay có cùng "Thứ" với ngày bắt đầu không
                // Hoặc đơn giản là chia hết cho 7
                return (diffDays % 7) == 0;

            case "MONTHLY":
                // Kiểm tra xem hôm nay có cùng "Ngày trong tháng" không (VD: cùng ngày 15)
                // Lưu ý: Cần lấy lại Calendar gốc chưa reset time để check ngày tháng chuẩn
                Calendar checkToday = Calendar.getInstance();
                Calendar checkStart = Calendar.getInstance();
                checkStart.setTimeInMillis(startDateMillis);

                return checkToday.get(Calendar.DAY_OF_MONTH) == checkStart.get(Calendar.DAY_OF_MONTH);

            case "ONCE":
                return diffDays == 0;

            default:
                return true;
        }
    }

    private void resetTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }
}