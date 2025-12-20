package com.example.habittracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.habittracker.MainActivity;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Đây là nơi code sẽ chạy khi đến giờ hẹn (VD: 8:00 AM)

        // 1. Lấy dữ liệu ngày đã chọn từ SharedPreferences để kiểm tra
        SharedPreferences prefs = context.getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);

        // Kiểm tra xem Switch tổng có đang bật không? (Double check cho chắc)
        boolean isAllowed = prefs.getBoolean("allow_notifs", false);
        if (!isAllowed) {
            return; // Nếu tắt rồi thì thôi, không làm gì cả
        }

        // 2. Kiểm tra hôm nay có phải ngày được chọn không (Logic Phương án A)
        if (isTodaySelected(prefs)) {
            // Nếu đúng ngày -> Bắn thông báo Daily Briefing
            // Truyền MainActivity.class để khi bấm vào thông báo thì mở App
            NotificationHelper.showTestNotification(context, MainActivity.class);
        }
    }

    // Hàm kiểm tra xem hôm nay có nằm trong danh sách user chọn không
    private boolean isTodaySelected(SharedPreferences prefs) {
        String savedIndices = prefs.getString("selected_days_indices", "0,1,2,3,4,5,6"); // Mặc định full tuần
        if (savedIndices.isEmpty()) return false;

        // Lấy thứ trong tuần hiện tại (Android tính: CN=1, T2=2, ..., T7=7)
        Calendar calendar = Calendar.getInstance();
        int currentDayAndroid = calendar.get(Calendar.DAY_OF_WEEK);

        // Quy đổi sang hệ số của App mình (T2=0, T3=1, ..., CN=6)
        // Android: Sun(1), Mon(2), Tue(3), Wed(4), Thu(5), Fri(6), Sat(7)
        // App mình: Mon(0), Tue(1), Wed(2), Thu(3), Fri(4), Sat(5), Sun(6)
        int currentDayAppIndex = -1;

        if (currentDayAndroid == Calendar.MONDAY) currentDayAppIndex = 0;
        else if (currentDayAndroid == Calendar.TUESDAY) currentDayAppIndex = 1;
        else if (currentDayAndroid == Calendar.WEDNESDAY) currentDayAppIndex = 2;
        else if (currentDayAndroid == Calendar.THURSDAY) currentDayAppIndex = 3;
        else if (currentDayAndroid == Calendar.FRIDAY) currentDayAppIndex = 4;
        else if (currentDayAndroid == Calendar.SATURDAY) currentDayAppIndex = 5;
        else if (currentDayAndroid == Calendar.SUNDAY) currentDayAppIndex = 6;

        // Kiểm tra xem index hôm nay có trong chuỗi đã lưu không
        String[] split = savedIndices.split(",");
        for (String s : split) {
            if (s.equals(String.valueOf(currentDayAppIndex))) {
                return true; // Có trùng -> Hôm nay cần báo
            }
        }
        return false; // Không trùng
    }
}