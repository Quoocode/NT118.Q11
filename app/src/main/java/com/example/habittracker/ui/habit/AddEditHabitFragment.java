package com.example.habittracker.ui.habit;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
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
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.databinding.FragmentAddEditHabitBinding; // Binding

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

    private void saveHabit() {
        String title = binding.editHabitName.getText().toString().trim();
        if (title.isEmpty()) {
            binding.editHabitName.setError("Required");
            return;
        }

        double target = 0;
        try {
            target = Double.parseDouble(binding.editHabitValue.getText().toString());
        } catch (Exception e) {
            // Nếu nhập sai số hoặc để trống, mặc định là 0 (hoặc bạn có thể báo lỗi)
        }

        String unit = binding.editHabitUnit.getText().toString().trim();
        String desc = binding.editHabitDesc.getText().toString().trim();

        // Tạo Map Frequency để lưu vào DB
        Map<String, Object> freqMap = new HashMap<>();
        freqMap.put("type", selectedFrequency);
        // (Nếu sau này làm Weekly nâng cao thì thêm "daysOfWeek" vào đây)

        // Tạo đối tượng Habit mới
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

        // Callback xử lý kết quả
        SimpleCallback callback = (success, e) -> {
            if (success) {
                String msg = isEditMode ? "Saved Changes" : "Habit Created";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                navController.popBackStack(); // Quay về màn hình trước
            } else {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Gọi Repository
        if (isEditMode) {
            habit.setId(currentHabitId); // Quan trọng: Gán ID cũ để update đúng doc
            habitRepository.updateHabit(habit, callback);
        } else {
            habitRepository.addHabit(habit, callback);
        }
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