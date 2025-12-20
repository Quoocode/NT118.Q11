package com.example.habittracker.ui.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
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
import java.util.HashSet;
import java.util.Set;

public class SettingsReminderFragment extends Fragment {

    private FragmentSettingsReminderBinding binding;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "HabitTrackerPrefs";

    // Keys để lưu dữ liệu
    private static final String KEY_ALLOW_NOTIFS = "allow_notifs";
    private static final String KEY_SOUND = "sound_enabled";
    private static final String KEY_VIBRATION = "vibration_enabled";
    private static final String KEY_TIME_DISPLAY = "reminder_time_display";
    private static final String KEY_TIME_HOUR = "reminder_hour";
    private static final String KEY_TIME_MINUTE = "reminder_minute";
    private static final String KEY_FREQ_DISPLAY = "frequency_display";
    // Chúng ta sẽ lưu các ngày đã chọn dưới dạng String (VD: "0,2,4" cho T2, T4, T6)
    private static final String KEY_SELECTED_DAYS = "selected_days_indices";

    // Biến tạm cho Logic chọn ngày
    private boolean[] selectedDays;
    private ArrayList<Integer> dayList = new ArrayList<>();
    private final String[] dayArray = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    // Launcher xin quyền
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Notifications enabled!", Toast.LENGTH_SHORT).show();
                    saveBoolean(KEY_ALLOW_NOTIFS, true);
                    updateUIState(true);
                    NotificationHelper.showTestNotification(requireContext(), MainActivity.class);
                } else {
                    Toast.makeText(getContext(), "Permission denied.", Toast.LENGTH_SHORT).show();
                    binding.switchAllowNotifs.setChecked(false);
                    saveBoolean(KEY_ALLOW_NOTIFS, false);
                    updateUIState(false);
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

        NavController navController = NavHostFragment.findNavController(this);

        // 2. Load dữ liệu cũ lên giao diện
        loadSavedSettings();

        // 3. Sự kiện Nút Back
        binding.btnBackReminders.setOnClickListener(v -> navController.popBackStack());

        // 4. Sự kiện Switch Tổng (Allow Notifications)
        binding.switchAllowNotifs.setOnClickListener(v -> {
            boolean isChecked = binding.switchAllowNotifs.isChecked();
            if (isChecked) {
                checkAndRequestPermission(); // Nếu bật -> Xin quyền
            } else {
                saveBoolean(KEY_ALLOW_NOTIFS, false);
                updateUIState(false); // Nếu tắt -> Disable các nút dưới
            }
        });

        // 5. Sự kiện Time Picker (Chọn giờ)
        binding.btnSetTime.setOnClickListener(v -> showTimePickerDialog());

        // 6. Sự kiện Frequency (Chọn ngày)
        binding.btnSetFrequency.setOnClickListener(v -> showDayPickerDialog());

        // 7. Sự kiện Switch Sound & Vibration
        binding.switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBoolean(KEY_SOUND, isChecked));

        binding.switchVibration.setOnCheckedChangeListener((buttonView, isChecked) ->
                saveBoolean(KEY_VIBRATION, isChecked));
    }

    // --- LOGIC TIME PICKER ---
    private void showTimePickerDialog() {
        // Lấy giờ hiện tại hoặc giờ đã lưu làm mặc định
        int hour = sharedPreferences.getInt(KEY_TIME_HOUR, 8);
        int minute = sharedPreferences.getInt(KEY_TIME_MINUTE, 0);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    // Xử lý khi chọn xong
                    Calendar timeSet = Calendar.getInstance();
                    timeSet.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    timeSet.set(Calendar.MINUTE, minuteOfHour);

                    // Format giờ (AM/PM)
                    CharSequence formattedTime = DateFormat.format("hh:mm a", timeSet);

                    // Cập nhật UI
                    binding.txtTimeDisplay.setText(formattedTime);

                    // Lưu vào bộ nhớ
                    saveString(KEY_TIME_DISPLAY, formattedTime.toString());
                    saveInt(KEY_TIME_HOUR, hourOfDay);
                    saveInt(KEY_TIME_MINUTE, minuteOfHour);
                }, hour, minute, false); // false = Chế độ 12h (AM/PM)

        timePickerDialog.show();
    }

    // --- LOGIC DAY PICKER ---
    private void showDayPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Days");

        // Load lại các ngày đã tick trước đó vào mảng boolean selectedDays
        // (Logic này đảm bảo khi mở lại dialog, các dấu tick cũ vẫn còn)
        // Phần này hơi phức tạp nên làm đơn giản ở Phase 1 là reset,
        // nhưng để UX tốt thì nên giữ lại. Ở đây tôi giữ lại list dayList.

        builder.setMultiChoiceItems(dayArray, selectedDays, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!dayList.contains(which)) {
                    dayList.add(which);
                }
            } else {
                dayList.remove(Integer.valueOf(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            // Xử lý text hiển thị
            StringBuilder stringBuilder = new StringBuilder();
            Collections.sort(dayList); // Sắp xếp thứ tự T2, T3...

            if (dayList.size() == 7) {
                stringBuilder.append("Every Day");
            } else if (dayList.isEmpty()) {
                stringBuilder.append("Never");
            } else {
                for (int i = 0; i < dayList.size(); i++) {
                    stringBuilder.append(dayArray[dayList.get(i)]);
                    if (i != dayList.size() - 1) {
                        stringBuilder.append(", ");
                    }
                }
            }

            // Cập nhật UI và Lưu
            String result = stringBuilder.toString();
            binding.txtFrequencyDisplay.setText(result);
            saveString(KEY_FREQ_DISPLAY, result);

            // Lưu danh sách index các ngày đã chọn để lần sau restore (VD: "0,2,4")
            saveDayListToPrefs();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Đã có quyền
                saveBoolean(KEY_ALLOW_NOTIFS, true);
                updateUIState(true);
            } else {
                // Chưa có quyền -> Xin
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android cũ -> Mặc định có quyền
            saveBoolean(KEY_ALLOW_NOTIFS, true);
            updateUIState(true);
        }
    }

    private void updateUIState(boolean isAllowed) {
        // Làm mờ và khóa các nút nếu tắt thông báo
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
        // Switch Tổng
        boolean isAllowed = sharedPreferences.getBoolean(KEY_ALLOW_NOTIFS, false);
        binding.switchAllowNotifs.setChecked(isAllowed);
        updateUIState(isAllowed);

        // Switch Con
        binding.switchSound.setChecked(sharedPreferences.getBoolean(KEY_SOUND, true));
        binding.switchVibration.setChecked(sharedPreferences.getBoolean(KEY_VIBRATION, true));

        // Time
        binding.txtTimeDisplay.setText(sharedPreferences.getString(KEY_TIME_DISPLAY, "08:00 AM"));

        // Frequency
        binding.txtFrequencyDisplay.setText(sharedPreferences.getString(KEY_FREQ_DISPLAY, "Every Day"));

        // Restore dayList và selectedDays array
        restoreDayListFromPrefs();
    }

    // Lưu danh sách ngày dạng chuỗi "0,1,2"
    private void saveDayListToPrefs() {
        StringBuilder sb = new StringBuilder();
        for (Integer i : dayList) {
            sb.append(i).append(",");
        }
        saveString(KEY_SELECTED_DAYS, sb.toString());
    }

    // Khôi phục danh sách ngày từ chuỗi
    private void restoreDayListFromPrefs() {
        String savedIndices = sharedPreferences.getString(KEY_SELECTED_DAYS, "0,1,2,3,4,5,6"); // Mặc định full tuần
        dayList.clear();
        if (!savedIndices.isEmpty()) {
            String[] split = savedIndices.split(",");
            for (String s : split) {
                if (!s.isEmpty()) {
                    try {
                        int index = Integer.parseInt(s);
                        if(index >=0 && index < dayArray.length) {
                            dayList.add(index);
                            selectedDays[index] = true; // Tick vào ô checkbox tương ứng
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