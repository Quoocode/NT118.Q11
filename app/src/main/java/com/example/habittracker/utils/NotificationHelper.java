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
    private static final int DAILY_BRIEFING_ID = 1001;


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

    // 2. Đặt lịch Daily Briefing (CẬP NHẬT THEO YÊU CẦU CỦA BẠN)
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleDailyBriefing(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class); // Vẫn dùng ReminderReceiver cũ cho Daily Briefing

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                DAILY_BRIEFING_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // Nếu giờ đặt đã qua -> Lùi sang ngày mai
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (alarmManager != null) {
            // THAY THẾ setRepeating BẰNG setExactAndAllowWhileIdle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d("ALARM_DEBUG", "Đã đặt lịch Daily Briefing chính xác lúc " + calendar.getTime().toString());
        }
    }

    // 4. Hủy lịch hẹn (Gọi khi user tắt switch)
    public static void cancelDailyBriefing(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, DAILY_BRIEFING_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

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
    // Đặt lịch cho Thói quen (CẬP NHẬT: Dùng setExactAndAllowWhileIdle cho MỌI TẦN SUẤT)
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
        // Lấy ngày hiện tại làm mốc
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Xử lý nếu giờ đặt đã qua trong ngày hôm nay
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            if ("WEEKLY".equals(frequency)) {
                // Nếu là Weekly mà qua giờ rồi -> Chuyển sang tuần sau (cộng 7 ngày)
                // Lưu ý: Logic này tạm thời đơn giản, đúng ra phải check ngày trong tuần
                calendar.add(Calendar.DAY_OF_YEAR, 7);
            } else if ("ONCE".equals(frequency)) {
                // ONCE qua rồi thì thôi, không đặt nữa
                return;
            } else {
                // DAILY và MONTHLY: Cộng 1 ngày để đặt cho ngày mai
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        // Tạo Intent gửi tới HabitAlarmReceiver (Receiver MỚI)
        Intent intent = new Intent(context, HabitAlarmReceiver.class);
        intent.putExtra("HABIT_TITLE", title);
        intent.putExtra("HABIT_ID", habitId);
        intent.putExtra("HABIT_FREQ_TYPE", frequency);
        intent.putExtra("HABIT_START_DATE", startDate.getTime());
        // [QUAN TRỌNG] Truyền timeString để Receiver biết đường đặt lại
        intent.putExtra("HABIT_TIME_STRING", timeString);

        // ID Báo thức duy nhất = HashCode của Habit ID
        int alarmId = habitId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calendar.getTimeInMillis();

        // [CẬP NHẬT] Luôn dùng setExactAndAllowWhileIdle cho tất cả các loại tần suất
        // Receiver sẽ chịu trách nhiệm đặt lại (Reschedule) sau khi báo thức nổ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d("ALARM_DEBUG", "Đã đặt lịch Habit chính xác cho: " + title + " lúc " + calendar.getTime().toString());
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