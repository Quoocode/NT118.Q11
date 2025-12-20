package com.example.habittracker.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.habittracker.R;

import java.util.Calendar;

public class NotificationHelper {

    private static final String CHANNEL_ID = "habit_tracker_reminder_channel";
    private static final String CHANNEL_NAME = "Habit Reminders";
    private static final String CHANNEL_DESC = "Thông báo nhắc nhở thói quen hàng ngày";
    private static final int DAILY_REMINDER_REQUEST_CODE = 1001; // Mã định danh cho báo thức này

    // 1. Tạo kênh thông báo
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 2. Hàm bắn thông báo (Đã có từ trước)
    @SuppressLint("MissingPermission")
    public static void showTestNotification(Context context, Class<?> targetActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }
        Intent intent = new Intent(context, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle("Daily Briefing") // Đổi tiêu đề cho hợp ngữ cảnh
                .setContentText("Chào buổi sáng! Kiểm tra các thói quen của bạn ngay.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }

    // --- CÁC HÀM MỚI (SCHEDULER) ---

    // 3. Đặt lịch hẹn giờ (Gọi hàm này khi user lưu giờ hoặc bật switch)
    @SuppressLint("ScheduleExactAlarm") // Bỏ qua cảnh báo quyền Exact Alarm (cần thêm quyền vào manifest sau)
    public static void scheduleDailyBriefing(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

        // FLAG_UPDATE_CURRENT: Nếu đã có lịch cũ thì cập nhật đè lên
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, DAILY_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Tính thời gian báo thức
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Nếu giờ đặt đã qua rồi (VD: giờ là 9h mà đặt 8h), thì lùi sang ngày mai
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        // Đặt báo thức lặp lại hàng ngày (RTC_WAKEUP: Đánh thức máy kể cả khi ngủ)
        if (alarmManager != null) {
            // Dùng setRepeating cho đơn giản (Độ chính xác tương đối)
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, // Lặp lại sau mỗi 24h
                    pendingIntent
            );
        }
    }

    // 4. Hủy lịch hẹn (Gọi khi user tắt switch)
    public static void cancelDailyBriefing(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, DAILY_REMINDER_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}