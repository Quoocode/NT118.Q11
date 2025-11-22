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

import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentHabitDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;

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

        // TODO: Bạn có thể gọi thêm hàm loadData() ở đây để hiển thị thông tin chi tiết lên giao diện
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