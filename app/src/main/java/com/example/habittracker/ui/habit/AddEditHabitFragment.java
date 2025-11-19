package com.example.habittracker.ui.habit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.habittracker.databinding.FragmentAddEditHabitBinding; // Tạo từ fragment_add_edit_habit.xml

public class AddEditHabitFragment extends Fragment {

    private FragmentAddEditHabitBinding binding;
    private NavController navController;

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

        // Nút "Back" (mũi tên)
        binding.btnBack.setOnClickListener(v -> {
            navController.popBackStack();
        });

        // Nút "Create Habit"
        binding.btnCreateHabit.setOnClickListener(v -> {
            // TODO: Xử lý logic lưu thói quen
            Toast.makeText(getContext(), "Đã tạo thói quen", Toast.LENGTH_SHORT).show();
            navController.popBackStack(); // Quay về màn hình Home
        });

        // Các nút Daily, Weekly, Monthly...
        binding.btnDaily.setOnClickListener(v -> { /* TODO: Xử lý chọn */ });
        binding.btnWeekly.setOnClickListener(v -> { /* TODO: Xử lý chọn */ });
        binding.btnMonthly.setOnClickListener(v -> { /* TODO: Xử lý chọn */ });
        binding.btnTime.setOnClickListener(v -> { /* TODO: Mở Time Picker */ });
        binding.btnDays.setOnClickListener(v -> { /* TODO: Mở chọn ngày */ });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}