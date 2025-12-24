package com.example.habittracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.habittracker.MainActivity;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ALARM_DEBUG", "⏰ ReminderReceiver (Daily Briefing) đã bị đánh thức!");

        SharedPreferences prefs = context.getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);

        // 1. Kiểm tra Switch tổng
        boolean isAllowed = prefs.getBoolean("allow_notifs", false);
        if (!isAllowed) {
            Log.d("ALARM_DEBUG", ">> Switch tắt -> Không làm gì cả.");
            return;
        }

        // 2. Logic hiện thông báo (Smart Check)
        if (isTodaySelected(prefs)) {
            Log.d("ALARM_DEBUG", ">> Hôm nay có lịch -> Bắn thông báo Daily Briefing!");
            NotificationHelper.showTestNotification(context, MainActivity.class);
        } else {
            Log.d("ALARM_DEBUG", ">> Hôm nay không có lịch -> Im lặng.");
        }

        // 3. [QUAN TRỌNG NHẤT] TỰ ĐỘNG ĐẶT LẠI LỊCH CHO LẦN SAU (Reschedule)
        // Lấy giờ đã lưu
        int hour = prefs.getInt("reminder_hour", 8);
        int minute = prefs.getInt("reminder_minute", 0);

        Log.d("ALARM_DEBUG", ">> Đang Reschedule cho lần tiếp theo...");

        // Gọi lại hàm đặt lịch. Vì giờ này (ví dụ 14:52) đã qua so với hiện tại (14:52:01),
        // nên logic bên trong NotificationHelper sẽ tự động cộng thêm 1 ngày (hoặc 1 phút hack).
        NotificationHelper.scheduleDailyBriefing(context, hour, minute);
    }

    // Hàm kiểm tra ngày (Giữ nguyên)
    private boolean isTodaySelected(SharedPreferences prefs) {
        String savedIndices = prefs.getString("selected_days_indices", "0,1,2,3,4,5,6");
        if (savedIndices.isEmpty()) return false;

        Calendar calendar = Calendar.getInstance();
        int currentDayAndroid = calendar.get(Calendar.DAY_OF_WEEK);
        int currentDayAppIndex = -1;

        if (currentDayAndroid == Calendar.MONDAY) currentDayAppIndex = 0;
        else if (currentDayAndroid == Calendar.TUESDAY) currentDayAppIndex = 1;
        else if (currentDayAndroid == Calendar.WEDNESDAY) currentDayAppIndex = 2;
        else if (currentDayAndroid == Calendar.THURSDAY) currentDayAppIndex = 3;
        else if (currentDayAndroid == Calendar.FRIDAY) currentDayAppIndex = 4;
        else if (currentDayAndroid == Calendar.SATURDAY) currentDayAppIndex = 5;
        else if (currentDayAndroid == Calendar.SUNDAY) currentDayAppIndex = 6;

        String[] split = savedIndices.split(",");
        for (String s : split) {
            if (s.equals(String.valueOf(currentDayAppIndex))) {
                return true;
            }
        }
        return false;
    }
}