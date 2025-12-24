package com.example.habittracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
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
        // [QUAN TRỌNG] Lấy thêm timeString để đặt lại lịch
        String timeString = intent.getStringExtra("HABIT_TIME_STRING");

        Log.d(TAG, "⏰ Báo thức đã nổ cho: " + habitTitle);

        if (habitId == null) return;

        // 2. SMART CHECK: Kiểm tra xem hôm nay có cần nhắc không?
        if (shouldNotifyToday(frequencyType, startDateMillis)) {
            Log.d(TAG, ">> CHECK OK: Hôm nay đúng lịch -> Bắn thông báo!");
            NotificationHelper.showHabitNotification(context, habitTitle, habitId);
        } else {
            Log.d(TAG, ">> CHECK FAIL: Hôm nay không phải lịch (Frequency check) -> Bỏ qua.");
        }

        // 3. RESCHEDULE (QUAN TRỌNG: Đặt lại cho lần sau)
        // Vì setExact chỉ chạy 1 lần, ta phải thủ công đặt cái tiếp theo ngay bây giờ
        // Loại trừ ONCE vì nó chỉ báo 1 lần là xong
        if (timeString != null && !"ONCE".equals(frequencyType)) {
            Log.d(TAG, ">> Đang tự động đặt lịch kế tiếp...");

            // Gọi lại hàm đặt lịch. Hàm này có logic:
            // "Nếu giờ đặt <= giờ hiện tại (đã qua), tự động cộng thêm ngày/tuần"
            // Nhờ đó báo thức sẽ nhảy sang ngày mai.
            NotificationHelper.scheduleHabitReminder(
                    context,
                    habitId,
                    habitTitle,
                    timeString,
                    frequencyType,
                    new Date(startDateMillis)
            );
        }
    }

    // Logic kiểm tra ngày (Giữ nguyên)
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
                return (diffDays % 7) == 0;

            case "MONTHLY":
                // Kiểm tra xem hôm nay có cùng "Ngày trong tháng" không
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