package com.example.habittracker.ui.habit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentHabitDetailsBinding; // Tạo từ fragment_habit_details.xml

public class HabitDetailsFragment extends Fragment {

    private FragmentHabitDetailsBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHabitDetailsBinding.inflate(inflater, container, false);
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

        // Nút "Edit Habit"
        binding.btnEditHabit.setOnClickListener(v -> {
            // TODO: Lấy ID thói quen và gửi sang màn hình Edit
            // Bundle args = new Bundle();
            // args.putString("HABIT_ID_TO_EDIT", "...");

            // Điều hướng đến màn hình Add/Edit
            // Chú ý: Chúng ta dùng chung action từ Home
            navController.navigate(R.id.action_homeFragment_to_addEditHabitFragment);
        });

        // Nút "Delete Habit"
        binding.btnDeleteHabit.setOnClickListener(v -> {
            // TODO: Xử lý logic xóa
            navController.popBackStack(); // Quay về Home sau khi xóa
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}