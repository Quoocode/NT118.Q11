package com.example.habittracker.ui.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent; // Import Mới
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri; // Import Mới
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings; // Import Mới
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.MainActivity;
import com.example.habittracker.databinding.FragmentSettingsReminderBinding;
import com.example.habittracker.utils.NotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class SettingsReminderFragment extends Fragment {

    private FragmentSettingsReminderBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabitTrackerPrefs";

    // Keys lưu trữ
    private static final String KEY_ALLOW_NOTIFS = "allow_notifs";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_TIME_DISPLAY = "reminder_time_display";
    private static final String KEY_TIME_HOUR = "reminder_hour";
    private static final String KEY_TIME_MINUTE = "reminder_minute";
    private static final String KEY_FREQ_DISPLAY = "frequency_display";
    private static final String KEY_SELECTED_DAYS = "selected_days_indices";

    // Biến cho Day Picker
    private boolean[] selectedDays;
    private ArrayList<Integer> dayList = new ArrayList<>();
    private final String[] dayArray = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // Launcher xin quyền POST_NOTIFICATIONS (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
                    saveBoolean(KEY_ALLOW_NOTIFS, true);
                    updateUIState(true);

                    // Quyền đã cấp -> Kích hoạt ngay lịch hẹn giờ
                    rescheduleAlarm();

                    // Bắn test và debug
                    NotificationHelper.showTestNotification(requireContext(), MainActivity.class);
                    NotificationHelper.debugAlarmPermission(requireContext());
                } else {
                    Toast.makeText(getContext(), "Bạn đã từ chối quyền thông báo.", Toast.LENGTH_SHORT).show();
                    binding.switchAllowNotifs.setChecked(false);
                    saveBoolean(KEY_ALLOW_NOTIFS, false);
                    updateUIState(false);
                    NotificationHelper.cancelDailyBriefing(requireContext());
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsReminderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo
        sharedPreferences = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        NotificationHelper.createNotificationChannel(requireContext());
        selectedDays = new boolean[dayArray.length];

        // Debug ngay khi vào màn hình
        NotificationHelper.debugAlarmPermission(requireContext());

        NavController navController = NavHostFragment.findNavController(this);

        // 2. Load cài đặt cũ
        loadSavedSettings();

        // 3. Sự kiện Click
        binding.btnBackReminders.setOnClickListener(v -> navController.popBackStack());

        // --- SỬA LOGIC SWITCH ---
        binding.switchAllowNotifs.setOnClickListener(v -> {
            boolean isChecked = binding.switchAllowNotifs.isChecked();
            if (isChecked) {
                // [MỚI] Bước 1: Kiểm tra quyền Báo thức chính xác trước
                if (!checkExactAlarmPermission()) {
                    binding.switchAllowNotifs.setChecked(false); // Tắt lại switch vì chưa có quyền
                    showPermissionDialog(); // Hiện popup bắt bật
                    return;
                }

                // [MỚI] Bước 2: Nếu có quyền báo thức rồi -> Kiểm tra quyền Thông báo
                checkAndRequestPermission();
            } else {
                saveBoolean(KEY_ALLOW_NOTIFS, false);
                updateUIState(false);
                NotificationHelper.cancelDailyBriefing(requireContext());
            }
        });

        binding.btnSetTime.setOnClickListener(v -> showTimePickerDialog());

        binding.btnSetFrequency.setOnClickListener(v -> showDayPickerDialog());

        binding.switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBoolean(KEY_SOUND, isChecked));

        binding.switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBoolean(KEY_VIBRATION, isChecked));
    }

    // --- HÀM KIỂM TRA QUYỀN ALARM (MỚI THÊM) ---
    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) requireContext().getSystemService(android.content.Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                return false;
            }
        }
        return true;
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Cấp quyền Báo thức")
                .setMessage("Để sử dụng tính năng nhắc nhở hàng ngày, ứng dụng cần quyền đặt báo thức chính xác. Nhấn OK để mở Cài đặt.")
                .setPositiveButton("OK", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    // -------------------------------------------

    // --- LOGIC TIME PICKER ---
    private void showTimePickerDialog() {
        int hour = sharedPreferences.getInt(KEY_TIME_HOUR, 8);
        int minute = sharedPreferences.getInt(KEY_TIME_MINUTE, 0);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    Calendar timeSet = Calendar.getInstance();
                    timeSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeSet.set(Calendar.MINUTE, minuteOfHour);

                    CharSequence formattedTime = DateFormat.format("hh:mm a", timeSet);
                    binding.txtTimeDisplay.setText(formattedTime);

                    // Lưu giờ mới
                    saveString(KEY_TIME_DISPLAY, formattedTime.toString());
                    saveInt(KEY_TIME_HOUR, hourOfDay);
                    saveInt(KEY_TIME_MINUTE, minuteOfHour);

                    // Cập nhật lại báo thức
                    rescheduleAlarm();

                }, hour, minute, false);

        timePickerDialog.show();
    }

    // --- LOGIC DAY PICKER ---
    private void showDayPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Days");
        builder.setMultiChoiceItems(dayArray, selectedDays, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!dayList.contains(which)) dayList.add(which);
            } else {
                dayList.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            StringBuilder stringBuilder = new StringBuilder();
            Collections.sort(dayList);

            if (dayList.size() == 7) stringBuilder.append("Every Day");
            else if (dayList.isEmpty()) stringBuilder.append("Never");
            else {
                for (int i = 0; i < dayList.size(); i++) {
                    stringBuilder.append(dayArray[dayList.get(i)]);
                    if (i != dayList.size() - 1) stringBuilder.append(", ");
                }
            }

            String result = stringBuilder.toString();
            binding.txtFrequencyDisplay.setText(result);
            saveString(KEY_FREQ_DISPLAY, result);
            saveDayListToPrefs();

            // Cập nhật lại báo thức (vì thay đổi ngày)
            rescheduleAlarm();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private void rescheduleAlarm() {
        if (binding.switchAllowNotifs.isChecked()) {
            // [MỚI] Kiểm tra quyền trước khi đặt
            if (!checkExactAlarmPermission()) {
                Log.e("ALARM_PERMISSION", "Daily Briefing: Bị chặn quyền Exact Alarm.");
                showPermissionDialog();
                return;
            }

            int hour = sharedPreferences.getInt(KEY_TIME_HOUR, 8);
            int minute = sharedPreferences.getInt(KEY_TIME_MINUTE, 0);

            // Debug log
            NotificationHelper.debugAlarmPermission(requireContext());

            NotificationHelper.scheduleDailyBriefing(requireContext(), hour, minute);
            Toast.makeText(getContext(), "Đã đặt lịch Daily Briefing!", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                saveBoolean(KEY_ALLOW_NOTIFS, true);
                updateUIState(true);
                rescheduleAlarm(); // Đặt giờ ngay
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            saveBoolean(KEY_ALLOW_NOTIFS, true);
            updateUIState(true);
            rescheduleAlarm(); // Đặt giờ ngay
        }
    }

    private void updateUIState(boolean isAllowed) {
        float alpha = isAllowed ? 1.0f : 0.5f;
        binding.btnSetTime.setAlpha(alpha);
        binding.btnSetTime.setEnabled(isAllowed);
        binding.btnSetFrequency.setAlpha(alpha);
        binding.btnSetFrequency.setEnabled(isAllowed);
        binding.switchSound.setAlpha(alpha);
        binding.switchSound.setEnabled(isAllowed);
        binding.switchVibration.setAlpha(alpha);
        binding.switchVibration.setEnabled(isAllowed);
    }

    private void loadSavedSettings() {
        boolean isAllowed = sharedPreferences.getBoolean(KEY_ALLOW_NOTIFS, false);
        binding.switchAllowNotifs.setChecked(isAllowed);
        updateUIState(isAllowed);

        binding.switchSound.setChecked(sharedPreferences.getBoolean(KEY_SOUND, true));
        binding.switchVibration.setChecked(sharedPreferences.getBoolean(KEY_VIBRATION, true));
        binding.txtTimeDisplay.setText(sharedPreferences.getString(KEY_TIME_DISPLAY, "08:00 AM"));
        binding.txtFrequencyDisplay.setText(sharedPreferences.getString(KEY_FREQ_DISPLAY, "Every Day"));

        restoreDayListFromPrefs();
    }

    private void saveDayListToPrefs() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : dayList) sb.append(i).append(",");
        saveString(KEY_SELECTED_DAYS, sb.toString());
    }

    private void restoreDayListFromPrefs() {
        String savedIndices = sharedPreferences.getString(KEY_SELECTED_DAYS, "0,1,2,3,4,5,6");
        dayList.clear();
        for(int i=0; i<selectedDays.length; i++) selectedDays[i] = false;

        if (!savedIndices.isEmpty()) {
            String[] split = savedIndices.split(",");
            for (String s : split) {
                if (!s.isEmpty()) {
                    try {
                        int index = Integer.parseInt(s);
                        if(index >=0 && index < dayArray.length) {
                            dayList.add(index);
                            selectedDays[index] = true;
                        }
                    } catch (NumberFormatException e) { e.printStackTrace(); }
                }
            }
        }
    }

    private void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    private void saveString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    private void saveInt(String key, int value) {
        sharedPreferences.edit().putInt(key, value).apply();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}