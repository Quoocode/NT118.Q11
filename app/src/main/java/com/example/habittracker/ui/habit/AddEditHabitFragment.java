package com.example.habittracker.ui.habit;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.ui.adapter.IconAdapter; // Adapter Icon cũ
import com.example.habittracker.R; // Resource ID
import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.callback.DataCallback;
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.databinding.FragmentAddEditHabitBinding; // Binding

import com.example.habittracker.utils.NotificationHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddEditHabitFragment extends Fragment {

    private FragmentAddEditHabitBinding binding;
    private NavController navController;
    private HabitRepository habitRepository;

    // State
    private boolean isEditMode = false;
    private String currentHabitId = null;
    private Calendar selectedStartDate;
    private String selectedReminderTime = "09:00";
    private String selectedFrequency = "DAILY"; // Mặc định
    private String selectedIconName = "ic_menu_book"; // Mặc định

    // Danh sách Icon (Tên file drawable)
    private final List<String> iconList = Arrays.asList(
            "ic_menu_bed", "ic_menu_book", "ic_menu_dinner",
            "ic_menu_shopping", "ic_menu_thinking", "ic_menu_cooking",
            "ic_menu_running", "ic_menu_gym", "ic_menu_football",
            "ic_menu_water", "ic_menu_coffee", "ic_menu_game",
            "ic_menu_hospital"

            // Thêm icon của bạn vào đây (VD: ic_gym, ic_water...)
    );

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAddEditHabitBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // 1. Init Repository
        String uid = FirebaseAuth.getInstance().getUid();
        habitRepository = new HabitRepository(uid);

        selectedStartDate = Calendar.getInstance();

        // 2. Setup UI Components
        setupIconRecyclerView();
        setupFrequencyButtons();
        setupClickListeners();

        // 3. Check Mode (Add or Edit)
        if (getArguments() != null && getArguments().getString("EXTRA_HABIT_ID") != null) {
            isEditMode = true;
            currentHabitId = getArguments().getString("EXTRA_HABIT_ID");

            // Update UI cho Edit Mode
            binding.tvScreenTitle.setText("Edit Habit");
            binding.btnConfirmAction.setText("Save Changes"); // Nút duy nhất

            loadHabitData(currentHabitId);
        } else {
            isEditMode = false;
            // Update UI cho Add Mode
            binding.tvScreenTitle.setText("New Habit");
            binding.btnConfirmAction.setText("Create Habit"); // Nút duy nhất

            updateStartDateText();
        }
    }

    // --- SETUP UI ---

    private void setupIconRecyclerView() {
        binding.recyclerIcons.setLayoutManager(new GridLayoutManager(getContext(), 5));

        // Khởi tạo Adapter
        IconAdapter adapter = new IconAdapter(getContext(), iconList, selectedIconName, iconName -> {
            selectedIconName = iconName; // Callback khi chọn icon
        });
        binding.recyclerIcons.setAdapter(adapter);
    }

    private void setupFrequencyButtons() {
        // Helper để reset và highlight nút được chọn
        View.OnClickListener freqListener = v -> {
            // 1. Reset tất cả (setSelected = false để XML selector đổi màu về xám)
            binding.btnFreqOnce.setSelected(false);
            binding.btnFreqDaily.setSelected(false);
            binding.btnFreqWeekly.setSelected(false);
            binding.btnFreqMonthly.setSelected(false);

            // 2. Highlight nút được click
            v.setSelected(true);

            // 3. Lưu giá trị chuỗi để gửi lên Firebase
            if (v == binding.btnFreqOnce) selectedFrequency = "ONCE";
            else if (v == binding.btnFreqDaily) selectedFrequency = "DAILY";
            else if (v == binding.btnFreqWeekly) selectedFrequency = "WEEKLY";
            else if (v == binding.btnFreqMonthly) selectedFrequency = "MONTHLY";
        };

        binding.btnFreqOnce.setOnClickListener(freqListener);
        binding.btnFreqDaily.setOnClickListener(freqListener);
        binding.btnFreqWeekly.setOnClickListener(freqListener);
        binding.btnFreqMonthly.setOnClickListener(freqListener);

        // Mặc định chọn Daily khi mở màn hình
        binding.btnFreqDaily.performClick();
    }

    // --- LOAD DATA (EDIT MODE) ---

    private void loadHabitData(String habitId) {
        habitRepository.getHabitById(habitId, new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (!result.isEmpty()) {
                    Habit habit = (Habit) result.get(0);
                    populateUI(habit);
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Error loading habit", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(Habit habit) {
        binding.editHabitName.setText(habit.getTitle());
        binding.editHabitDesc.setText(habit.getDescription());
        binding.editHabitValue.setText(String.valueOf(habit.getTargetValue()));
        binding.editHabitUnit.setText(habit.getUnit());

        // Time
        selectedReminderTime = habit.getReminderTime();
        binding.tvTime.setText(selectedReminderTime);

        // Date
        if (habit.getStartDate() != null) {
            selectedStartDate.setTime(habit.getStartDate().toDate());
            updateStartDateText();
        }

        // Frequency (Khôi phục trạng thái nút bấm)
        if (habit.getFrequency() != null) {
            String type = (String) habit.getFrequency().get("type");
            if ("ONCE".equals(type)) binding.btnFreqOnce.performClick();
            else if ("WEEKLY".equals(type)) binding.btnFreqWeekly.performClick();
            else if ("MONTHLY".equals(type)) binding.btnFreqMonthly.performClick();
            else binding.btnFreqDaily.performClick();
        }

        // Icon (Khôi phục icon đã chọn)
        selectedIconName = habit.getIconName();
        if (binding.recyclerIcons.getAdapter() instanceof IconAdapter) {
            ((IconAdapter) binding.recyclerIcons.getAdapter()).setSelectedIcon(selectedIconName);
        }
    }

    // --- LISTENERS ---

    private void setupClickListeners() {
        // Nút Back
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        // Date Picker
        binding.btnStartDate.setOnClickListener(v -> {
            new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
                selectedStartDate.set(year, month, dayOfMonth);
                updateStartDateText();
            }, selectedStartDate.get(Calendar.YEAR), selectedStartDate.get(Calendar.MONTH), selectedStartDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time Picker
        binding.btnTime.setOnClickListener(v -> {
            String[] parts = selectedReminderTime.split(":");
            int hour = 9;
            int minute = 0;
            try {
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception e) {}

            new TimePickerDialog(requireContext(), (view, hourOfDay, minuteOfHour) -> {
                selectedReminderTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                binding.tvTime.setText(selectedReminderTime);
            }, hour, minute, DateFormat.is24HourFormat(getContext())).show();
        });

        // Nút Hành Động Duy Nhất (Create/Save)
        binding.btnConfirmAction.setOnClickListener(v -> saveHabit());
    }

    // --- HÀM KIỂM TRA QUYỀN (MỚI) ---
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
                .setMessage("Để nhận nhắc nhở thói quen đúng giờ, bạn cần cho phép ứng dụng đặt báo thức. Nhấn OK để mở Cài đặt.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Mở màn hình cài đặt quyền
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Để sau", null)
                .show();
    }
    // --------------------------------

    private void saveHabit() {
        // 1. Validate dữ liệu (Giữ nguyên code cũ của mày)
        String title = binding.editHabitName.getText().toString().trim();
        if (title.isEmpty()) {
            binding.editHabitName.setError("Required");
            return;
        }

        double target = 0;
        try {
            target = Double.parseDouble(binding.editHabitValue.getText().toString());
        } catch (Exception e) {
            // Mặc định 0
        }

        String unit = binding.editHabitUnit.getText().toString().trim();
        String desc = binding.editHabitDesc.getText().toString().trim();

        // Map Frequency (Giữ nguyên)
        Map<String, Object> freqMap = new HashMap<>();
        freqMap.put("type", selectedFrequency);

        // --- KIỂM TRA QUYỀN TRƯỚC KHI LƯU ---
        // Chỉ kiểm tra nếu người dùng có đặt giờ nhắc
        if (selectedReminderTime != null && !selectedReminderTime.isEmpty()) {
            if (!checkExactAlarmPermission()) {
                Log.e("ALARM_PERMISSION", "Bị chặn quyền Exact Alarm. Đang yêu cầu user cấp quyền...");
                showPermissionDialog();
                return; // Dừng lại, không lưu
            }
        }
        // -------------------------------------

        // Tạo Object Habit (Giữ nguyên)
        Habit habit = new Habit(
                title,
                desc,
                selectedIconName,
                unit,
                freqMap,
                selectedReminderTime,
                new Timestamp(selectedStartDate.getTime()),
                target
        );

        // 2. Gọi Repository (CODE MỚI - TÁCH LUỒNG)
        if (isEditMode) {
            habit.setId(currentHabitId);

            // Trường hợp UPDATE: Dùng DataCallback<Boolean>
            habitRepository.updateHabit(habit, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    // Update thành công -> ID chính là currentHabitId
                    handleSaveSuccess(currentHabitId, title);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Update Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            // Trường hợp ADD: Dùng DataCallback<String> để hứng cái ID mới về
            habitRepository.addHabit(habit, new DataCallback<String>() {
                @Override
                public void onSuccess(String newHabitId) {
                    // Add thành công -> Có ID mới toanh -> Truyền vào handleSaveSuccess
                    handleSaveSuccess(newHabitId, title);
                }


                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Add Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Hàm xử lý chung sau khi Lưu thành công
    private void handleSaveSuccess(String habitId, String habitTitle) {
        String msg = isEditMode ? "Saved Changes" : "Habit Created";
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();

        Log.d("TEST_REMINDER", "--------------------------");
        Log.d("TEST_REMINDER", "GIAI ĐOẠN 3 (FINAL): Save Success!");

        if (habitId != null && !habitId.isEmpty()) {
            NotificationHelper.debugAlarmPermission(requireContext());
            Log.d("TEST_REMINDER", ">> ID OK: " + habitId);

            // Kiểm tra xem user có đặt giờ không
            if (selectedReminderTime != null && !selectedReminderTime.isEmpty()) {
                Log.d("TEST_REMINDER", ">> Calling Scheduler for: " + selectedReminderTime);

                // [QUAN TRỌNG] GỌI HÀM ĐẶT BÁO THỨC THẬT SỰ
                NotificationHelper.scheduleHabitReminder(
                        requireContext(),
                        habitId,
                        habitTitle,
                        selectedReminderTime,
                        selectedFrequency,
                        selectedStartDate.getTime() // Chuyển Calendar thành Date
                );

            } else {
                Log.d("TEST_REMINDER", ">> No Reminder Time set. Skipping alarm.");
            }
        } else {
            Log.e("TEST_REMINDER", ">> FAIL: ID is null! Cannot schedule alarm.");
        }
        Log.d("TEST_REMINDER", "--------------------------");

        navController.popBackStack();
    }

    private void updateStartDateText() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        binding.tvStartDate.setText(sdf.format(selectedStartDate.getTime()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}