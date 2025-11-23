package com.example.habittracker.ui.habit;

import android.app.AlertDialog;
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

import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.R;
import com.example.habittracker.data.repository.callback.StatsCallback;
import com.example.habittracker.databinding.FragmentHabitDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class HabitDetailsFragment extends Fragment {

    private FragmentHabitDetailsBinding binding;
    private NavController navController;
    private HabitRepository habitRepository;
    private String habitId;

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

        // Khởi tạo Repository
        String uid = FirebaseAuth.getInstance().getUid();
        habitRepository = new HabitRepository(uid);

        // Lấy Habit ID được gửi từ HomeFragment
        if (getArguments() != null) {
            habitId = getArguments().getString("EXTRA_HABIT_ID");
        }

        // Nút "Back"
        binding.btnBack.setOnClickListener(v -> {
            navController.popBackStack();
        });

        // Nút "Edit Habit"
        binding.btnEditHabit.setOnClickListener(v -> {
            if (habitId != null) {
                // Đóng gói ID để gửi sang màn hình Edit
                Bundle args = new Bundle();
                args.putString("EXTRA_HABIT_ID", habitId);

                // Điều hướng sang màn hình Add/Edit
                navController.navigate(R.id.action_habitDetailsFragment_to_addEditHabitFragment, args);
            } else {
                Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID thói quen", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút "Delete Habit"
        binding.btnDeleteHabit.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        if (habitId != null) {
            loadHabitInfo();       // Tải tên, mô tả
            loadHabitStatistics(); // Tải số liệu (Tuần, Tháng, Tổng)
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadHabitInfo() {
        habitRepository.getHabitById(habitId, new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (!result.isEmpty()) {
                    Habit habit = (Habit) result.get(0);
                    // Gán tên thói quen vào TextView title
                    binding.tvDetailTitle.setText(habit.getTitle());

                    // Nếu bạn có TextView mô tả, gán ở đây:
                    // binding.tvDescription.setText(habit.getDescription());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm 2: Tải thống kê (Tuần, Tháng, Tổng)
    private void loadHabitStatistics() {
        habitRepository.getHabitStatistics(habitId, new StatsCallback() {
            @Override
            public void onStatsLoaded(int week, int month, int total) {
                // Đảm bảo cập nhật UI
                if (binding != null) {
                    binding.tvDetailThisWeek.setText(String.valueOf(week));
                    binding.tvDetailThisMonth.setText(String.valueOf(month));
                    binding.tvDetailTotalDone.setText(String.valueOf(total));
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải thống kê", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa thói quen?")
                .setMessage("Bạn có chắc chắn muốn xóa thói quen này không? Dữ liệu lịch sử vẫn sẽ được lưu trữ.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performDelete();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performDelete() {
        if (habitId == null) return;

        // Gọi hàm archiveHabit (Xóa mềm) trong Repository
        habitRepository.archiveHabit(habitId, new SimpleCallback() {
            @Override
            public void onComplete(boolean success, Exception e) {
                if (success) {
                    Toast.makeText(getContext(), "Đã xóa thói quen", Toast.LENGTH_SHORT).show();
                    navController.popBackStack(); // Quay về Home sau khi xóa thành công
                } else {
                    Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}