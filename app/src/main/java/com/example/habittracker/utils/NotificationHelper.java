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
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.habittracker.MainActivity;
import com.example.habittracker.R;

import java.util.Calendar;
import java.util.Date; // [FIX BUG 1] Đã thêm import Date


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

    // =========================================================================
    // 3. HABIT REMINDER LOGIC (LOGIC MỚI - CHO TỪNG THÓI QUEN)
    // =========================================================================

    // Hàm hiển thị thông báo cho từng thói quen
    @SuppressLint("MissingPermission")
    public static void showHabitNotification(Context context, String title, String habitId) {
        if (!hasPermission(context)) return;

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Dùng hashCode của habitId làm requestCode để pendingIntent là duy nhất
        int uniqueId = habitId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueId, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle("Nhắc nhở: " + title)
                .setContentText("Đến giờ thực hiện mục tiêu rồi! Cố lên bạn ơi.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // ID Thông báo = hashCode của habitId -> Đảm bảo các thông báo không đè nhau
        NotificationManagerCompat.from(context).notify(uniqueId, builder.build());
    }

    // Đặt lịch cho Thói quen
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleHabitReminder(Context context, String habitId, String title, String timeString, String frequency, Date startDate) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Parse giờ (09:00 -> 9, 0)
        String[] parts = timeString.split(":");
        if (parts.length != 2) return;
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        // Tính thời gian báo thức đầu tiên
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Xử lý nếu giờ đã qua
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            if ("WEEKLY".equals(frequency)) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        // Tạo Intent gửi tới HabitAlarmReceiver (Receiver MỚI)
        Intent intent = new Intent(context, HabitAlarmReceiver.class);
        intent.putExtra("HABIT_TITLE", title);
        intent.putExtra("HABIT_ID", habitId);
        intent.putExtra("HABIT_FREQ_TYPE", frequency);
        intent.putExtra("HABIT_START_DATE", startDate.getTime());

        // ID Báo thức duy nhất = HashCode của Habit ID
        int alarmId = habitId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calendar.getTimeInMillis();

        // Đặt lịch theo tần suất
        if ("ONCE".equals(frequency)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else if ("WEEKLY".equals(frequency)) {
            // Lặp mỗi 7 ngày
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY * 7, pendingIntent);
        } else {
            // DAILY và MONTHLY: Đều đặt lặp mỗi ngày (Receiver sẽ tự lọc ngày Monthly sau)
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, AlarmManager.INTERVAL_DAY, pendingIntent);
        }
    }

    // Hủy lịch cho Thói quen (Dùng khi Edit/Delete)
    public static void cancelHabitReminder(Context context, String habitId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, HabitAlarmReceiver.class);

        int alarmId = habitId.hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    // =========================================================================
    // 4. HELPER METHODS
    // =========================================================================

    private static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // Thêm vào cuối file NotificationHelper.java

    public static void debugAlarmPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            boolean canSchedule = alarmManager.canScheduleExactAlarms();

            Log.e("ALARM_DEBUG", "========================================");
            Log.e("ALARM_DEBUG", "Kiểm tra quyền SCHEDULE_EXACT_ALARM:");
            Log.e("ALARM_DEBUG", "Kết quả: " + (canSchedule ? "ĐƯỢC PHÉP (TRUE)" : "BỊ CHẶN (FALSE)"));

            if (!canSchedule) {
                Log.e("ALARM_DEBUG", ">> CẢNH BÁO: App không có quyền đặt báo thức chính xác!");
                Log.e("ALARM_DEBUG", ">> Vui lòng vào Cài đặt -> Ứng dụng -> HabitTracker -> Báo thức & Nhắc nhở -> Bật lên.");
            } else {
                Log.e("ALARM_DEBUG", ">> OK: Quyền đã được cấp. Nếu vẫn không kêu, kiểm tra Receiver.");
            }
            Log.e("ALARM_DEBUG", "========================================");
        } else {
            Log.e("ALARM_DEBUG", "Android phiên bản cũ (<12), mặc định có quyền.");
        }
    }

}