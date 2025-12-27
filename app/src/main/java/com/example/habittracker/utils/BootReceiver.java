package com.example.habittracker.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.DataCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("ALARM_DEBUG", "BootReceiver: Máy vừa khởi động lại! Bắt đầu khôi phục báo thức...");

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                // Lấy ID user để query database
                String userId = user.getUid();
                HabitRepository repository = new HabitRepository(userId);

                // Lấy danh sách thói quen đang hoạt động (Active)
                repository.getActiveHabits(new DataCallback<List<Habit>>() {
                    @Override
                    public void onSuccess(List<Habit> habits) {
                        if (habits != null && !habits.isEmpty()) {
                            Log.d("ALARM_DEBUG", "BootReceiver: Tìm thấy " + habits.size() + " thói quen. Đang đặt lại lịch...");

                            // Gọi hàm trong NotificationHelper để đặt lại báo thức
                            NotificationHelper.scheduleAllHabitReminders(context, habits);

                            // Đặt lại cả Daily Briefing nếu cần (Logic này tùy mày)
                            // NotificationHelper.scheduleDailyBriefing(context, 8, 0);
                        } else {
                            Log.d("ALARM_DEBUG", "BootReceiver: Không có thói quen nào để đặt lịch.");
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("ALARM_DEBUG", "BootReceiver: Lỗi tải dữ liệu habit: " + e.getMessage());
                    }
                });
            } else {
                Log.d("ALARM_DEBUG", "BootReceiver: User chưa đăng nhập, bỏ qua.");
            }
        }
    }
}