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
import com.example.habittracker.data.model.Habit;

import java.util.Calendar;
import java.util.Date; // [FIX BUG 1] Đã thêm import Date
import java.util.List;


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
                .setSmallIcon(R.drawable.ic_menu_thinking)
                .setContentTitle("Daily Briefing") // Đổi tiêu đề cho hợp ngữ cảnh
                .setContentText("Chào buổi sáng! Kiểm tra các thói quen của bạn ngay.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1001, builder.build());
    }

    // --- CÁC HÀM MỚI (SCHEDULER) ---
    // 2. Đặt lịch hẹn giờ (BẢN HACK TEST: 1 PHÚT LẶP LẠI 1 LẦN)
    // 2. Đặt lịch hẹn giờ (ĐÃ FIX LỖI LẶP VÔ TẬN)
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleDailyBriefing(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);

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
        calendar.set(Calendar.MILLISECOND, 0); // Quan trọng: Reset mili giây về 0

        // --- FIX BUG LOOP: Dùng while thay vì if ---
        // Chừng nào thời gian tính ra vẫn nhỏ hơn hoặc bằng hiện tại -> Cộng tiếp
        while (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1); // Code thật (Chạy thực tế dùng dòng này)
//            calendar.add(Calendar.MINUTE, 1);      // Code hack (Test lặp 1 phút)
            Log.e("ALARM_DEBUG", ">> Đã cộng thêm thời gian để đảm bảo ở tương lai!");
        }
        // -------------------------------------------

        if (alarmManager != null) {
            scheduleAlarmSafely(
                    context,
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
            Log.d("ALARM_DEBUG", "Đã đặt lịch Daily Briefing lúc " + calendar.getTime().toString());
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
                .setSmallIcon(R.drawable.ic_menu_thinking)
                .setContentTitle("Nhắc nhở: " + title)
                .setContentText("Đến giờ thực hiện mục tiêu rồi! Cố lên bạn ơi.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // ID Thông báo = hashCode của habitId -> Đảm bảo các thông báo không đè nhau
        NotificationManagerCompat.from(context).notify(uniqueId, builder.build());
    }

    // Đặt lịch cho Thói quen (PHIÊN BẢN HACK TEST: 1 PHÚT LẶP 1 LẦN)
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
        calendar.setTime(startDate); // Lấy ngày tháng năm bắt đầu làm gốc

        // Gán giờ nhắc vào ngày đó
        // Lưu ý: Logic này sẽ lấy ngày/tháng/năm hiện tại để làm mốc so sánh cho lần chạy đầu tiên
        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0); // Reset mili giây để so sánh chuẩn

        // --- ĐOẠN HACK LOGIC THỜI GIAN ---
        // Sử dụng WHILE để đảm bảo thời gian tính ra luôn ở tương lai (Fix lỗi lặp vô tận)
        while (calendar.getTimeInMillis() <= System.currentTimeMillis()) {

            if ("WEEKLY".equals(frequency)) {
                // Code thật: calendar.add(Calendar.WEEK_OF_YEAR, 1);
//                calendar.add(Calendar.MINUTE, 1); // HACK: 1 Tuần = 1 Phút
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                Log.e("ALARM_DEBUG", ">> HACK WEEKLY: Cộng 1 phút (giả lập 1 tuần)");
            }
            else if ("ONCE".equals(frequency)) {
                return; // ONCE qua rồi thì thôi
            }
            else {
                // DAILY và MONTHLY
                // Code thật: calendar.add(Calendar.DAY_OF_YEAR, 1);
//                calendar.add(Calendar.MINUTE, 1); // HACK: 1 Ngày = 1 Phút
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Log.e("ALARM_DEBUG", ">> HACK DAILY/MONTHLY: Cộng 1 phút (giả lập 1 ngày)");
            }
        }
        // --------------------------------

        // Tạo Intent gửi tới HabitAlarmReceiver
        Intent intent = new Intent(context, HabitAlarmReceiver.class);
        intent.putExtra("HABIT_TITLE", title);
        intent.putExtra("HABIT_ID", habitId);
        intent.putExtra("HABIT_FREQ_TYPE", frequency);
        intent.putExtra("HABIT_START_DATE", startDate.getTime());
        // Truyền timeString để Receiver biết đường gọi lại hàm này
        intent.putExtra("HABIT_TIME_STRING", timeString);

        // ID Báo thức duy nhất
        int alarmId = habitId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calendar.getTimeInMillis();

        scheduleAlarmSafely(
                context,
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
        );

        Log.d("ALARM_DEBUG", "Đã đặt lịch Habit (" + frequency + ") cho: " + title + " lúc " + calendar.getTime().toString());
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
    // 4. [MỚI] HÀM HỖ TRỢ LOGOUT/LOGIN HÀNG LOẠT
    // =========================================================================

    public static void cancelAllHabitReminders(Context context, List<Habit> habitList) {
        if (habitList == null || habitList.isEmpty()) return;

        for (Habit habit : habitList) {
            if (habit.getId() != null) {
                cancelHabitReminder(context, habit.getId());
                Log.d("ALARM_DEBUG", "Đã hủy báo thức cho: " + habit.getTitle());
            }
        }
    }

    public static void scheduleAllHabitReminders(Context context, List<Habit> habitList) {
        // 1. Log ngay khi vào hàm để biết hàm có ĐƯỢC GỌI hay không
        if (habitList == null) {
            Log.e("ALARM_DEBUG", "!!! scheduleAllHabitReminders bị gọi với habitList là NULL");
            return;
        }

        Log.d("ALARM_DEBUG", "--> Bắt đầu scheduleAllHabitReminders. Số lượng: " + habitList.size());

        // 2. Kiểm tra danh sách rỗng (Nguyên nhân chính thường gặp)
        if (habitList.isEmpty()) {
            Log.w("ALARM_DEBUG", "!!! Danh sách thói quen RỖNG. Không có gì để đặt báo thức.");
            return;
        }

        int count = 0;
        for (Habit habit : habitList) {
            // Log kiểm tra từng phần tử xem có bị lọt điều kiện if không
            if (habit.getId() != null &&
                    habit.getReminderTime() != null &&
                    !habit.getReminderTime().isEmpty() &&
                    !habit.isArchived()) {

                String freqType = "DAILY";
                if (habit.getFrequency() != null && habit.getFrequency().get("type") != null) {
                    freqType = (String) habit.getFrequency().get("type");
                }

                scheduleHabitReminder(
                        context,
                        habit.getId(),
                        habit.getTitle(),
                        habit.getReminderTime(),
                        freqType,
                        habit.getStartDate().toDate()
                );
                count++;
            } else {
                // Log nếu thói quen bị bỏ qua (để biết tại sao count không tăng)
                Log.d("ALARM_DEBUG", "Bỏ qua thói quen: " + habit.getTitle() + " (Do thiếu ID, ReminderTime hoặc đã Archive)");
            }
        }
        Log.d("ALARM_DEBUG", "Đã đặt lại báo thức thành công cho " + count + " thói quen.");
    }

    // =========================================================================
    // 5. HELPER METHODS
    // =========================================================================

    private static boolean hasPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    // =========================================================================
    // 6. [MỚI] LOGIC XỬ LÝ KHI CHECK/UNCHECK COMPLETED
    // =========================================================================

    public static void updateAlarmBasedOnStatus(Context context, Habit habit, boolean isCompleted) {
        if (habit == null || habit.getId() == null) return;

        // 1. Trường hợp UNDO (Bỏ hoàn thành) -> Trở về trạng thái PENDING
        if (!isCompleted) {
            // Gọi lại hàm đặt lịch chuẩn.
            // Logic cũ đã tự động xử lý: Nếu giờ hiện tại < giờ báo thức -> Đặt cho hôm nay.
            // Nếu giờ hiện tại > giờ báo thức -> Đặt cho ngày mai.
            String freqType = "DAILY";
            if (habit.getFrequency() != null && habit.getFrequency().get("type") != null) {
                freqType = (String) habit.getFrequency().get("type");
            }

            Log.d("ALARM_DEBUG", "User Undo: Đặt lại báo thức mặc định cho " + habit.getTitle());
            scheduleHabitReminder(
                    context,
                    habit.getId(),
                    habit.getTitle(),
                    habit.getReminderTime(),
                    freqType,
                    habit.getStartDate().toDate()
            );
            return;
        }

        // 2. Trường hợp COMPLETED (Đã xong)
        String freqType = "DAILY";
        if (habit.getFrequency() != null && habit.getFrequency().get("type") != null) {
            freqType = (String) habit.getFrequency().get("type");
        }

        // 2a. Nếu là ONCE -> Hủy luôn, không hẹn ngày gặp lại
        if ("ONCE".equals(freqType)) {
            Log.d("ALARM_DEBUG", "User Completed (ONCE): Hủy báo thức vĩnh viễn.");
            cancelHabitReminder(context, habit.getId());
            return;
        }

        // 2b. Nếu là DAILY/WEEKLY -> Dời sang chu kỳ tiếp theo
        // Chúng ta cần tính toán thủ công để ép nó nhảy cóc nếu cần
        scheduleNextCycleReminder(context, habit, freqType);
    }

    // Hàm phụ trợ: Tính toán và đặt báo thức cho chu kỳ TIẾP THEO (Bỏ qua hôm nay)
    private static void scheduleNextCycleReminder(Context context, Habit habit, String frequency) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null || habit.getReminderTime() == null) return;

        // --- [DEBUG] IN RA ID VÀ GIỜ ĐỂ KIỂM TRA ---
        Log.e("ALARM_DEBUG", "------------------------------------------------");
        Log.e("ALARM_DEBUG", "DEBUG RESCHEDULE CHO: " + habit.getTitle());
        Log.e("ALARM_DEBUG", "ID: " + habit.getId());
        Log.e("ALARM_DEBUG", "Time trong DB: " + habit.getReminderTime()); // Xem cái này có phải 15:41 không
        Log.e("ALARM_DEBUG", "Alarm ID (HashCode): " + habit.getId().hashCode());
        // ------------------------------------------------

        String[] parts = habit.getReminderTime().split(":");
        if (parts.length != 2) return;
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(habit.getStartDate().toDate());

        Calendar now = Calendar.getInstance();
        calendar.set(Calendar.YEAR, now.get(Calendar.YEAR));
        calendar.set(Calendar.MONTH, now.get(Calendar.MONTH));
        calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // Bước 1: Chạy logic đuổi bắt thời gian như bình thường
        while (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            if ("WEEKLY".equals(frequency)) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        // Bước 2: LOGIC NHẢY CÓC (QUAN TRỌNG)
        // Nếu sau khi tính xong mà ngày báo thức vẫn là HÔM NAY (tức là báo thức chưa kêu, user hoàn thành sớm)
        // Thì ta bắt buộc phải cộng thêm 1 chu kỳ nữa để dời sang ngày mai/tuần sau.
        if (isSameDay(calendar, now)) {
            if ("WEEKLY".equals(frequency)) {
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                Log.d("ALARM_DEBUG", "Completed sớm -> Dời Weekly sang tuần sau");
            } else {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Log.d("ALARM_DEBUG", "Completed sớm -> Dời Daily sang ngày mai");
            }
        }

        // Bước 3: Đặt báo thức với thời gian mới
        Intent intent = new Intent(context, HabitAlarmReceiver.class);
        intent.putExtra("HABIT_TITLE", habit.getTitle());
        intent.putExtra("HABIT_ID", habit.getId());
        intent.putExtra("HABIT_FREQ_TYPE", frequency);
        intent.putExtra("HABIT_START_DATE", habit.getStartDate().toDate().getTime());
        intent.putExtra("HABIT_TIME_STRING", habit.getReminderTime());

        int alarmId = habit.getId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = calendar.getTimeInMillis();

        // Copy logic kiểm tra quyền để tránh crash
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Log.d("ALARM_DEBUG", "Đã dời lịch (Completed) sang: " + calendar.getTime().toString());
    }

    // Hàm tiện ích kiểm tra xem 2 Calendar có cùng ngày không
    private static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
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

    // =========================================================================
    // 0. EXACT ALARM SAFETY (Android 12+)
    // =========================================================================

    private static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /** Public helper so UI layers can check whether exact alarms are currently allowed (Android 12+). */
    public static boolean isExactAlarmAllowed(Context context) {
        return canScheduleExactAlarms(context);
    }

    /**
     * Show a dialog that deep-links to the system screen to allow exact alarms (Android 12+).
     *
     * Safe no-op on Android < 12.
     */
    public static void showExactAlarmPermissionDialog(Context context) {
        if (context == null) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        // If already allowed, no need to interrupt.
        if (canScheduleExactAlarms(context)) return;

        try {
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Cho phép báo thức đúng giờ")
                    .setMessage("Để nhắc thói quen đúng giờ, ứng dụng cần quyền 'Báo thức chính xác'. Bạn có muốn mở Cài đặt để bật không?")
                    .setPositiveButton("Mở cài đặt", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        } catch (Throwable t) {
                            Log.e("ALARM_DEBUG", "Failed to open exact alarm settings", t);
                        }
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
        } catch (Throwable t) {
            // If context isn't an Activity-themed context, AlertDialog can fail.
            Log.e("ALARM_DEBUG", "Failed to show exact alarm permission dialog", t);
        }
    }

    /**
     * Schedules an alarm without crashing on Android 12+ when exact-alarm permission is denied.
     *
     * If exact alarms are allowed -> uses setExactAndAllowWhileIdle/setExact.
     * Otherwise -> uses an inexact fallback (setAndAllowWhileIdle/set).
     */
    @SuppressLint("ScheduleExactAlarm")
    private static void scheduleAlarmSafely(Context context,
                                            AlarmManager alarmManager,
                                            int alarmType,
                                            long triggerAtMillis,
                                            PendingIntent pendingIntent) {
        if (alarmManager == null) return;

        boolean allowExact = canScheduleExactAlarms(context);
        try {
            if (allowExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(alarmType, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.setExact(alarmType, triggerAtMillis, pendingIntent);
                }
            } else {
                // Fallback: still schedule a reminder, but not exact.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(alarmType, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(alarmType, triggerAtMillis, pendingIntent);
                }
                Log.w("ALARM_DEBUG", "Exact alarm permission denied; scheduled inexact alarm instead.");
            }
        } catch (SecurityException se) {
            // Double safety: some OEMs/devices can still throw even if canScheduleExactAlarms() returns true.
            Log.e("ALARM_DEBUG", "SecurityException while scheduling alarm; falling back to inexact.", se);
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(alarmType, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(alarmType, triggerAtMillis, pendingIntent);
                }
            } catch (Throwable t) {
                Log.e("ALARM_DEBUG", "Failed to schedule even fallback alarm.", t);
            }
        }
    }

}
