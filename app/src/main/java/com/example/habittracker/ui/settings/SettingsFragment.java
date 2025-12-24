package com.example.habittracker.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentSettingsBinding;
import com.example.habittracker.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;

// --- [SỬA LẠI IMPORT ĐÚNG PACKAGE DATA] ---
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.DataCallback;
// Lưu ý: Nếu DataCallback chưa có trong package này, mày nhớ tạo file DataCallback.java
// vào trong com.example.habittracker.data.repository.callback nhé.
// ------------------------------------------

import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Nút Back
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        // Chuyển sang Profile
        binding.btnProfileSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_settingsFragment_to_settingsProfileFragment);
        });

        // Chuyển sang Reminders
        binding.btnRemindersSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_settingsFragment_to_settingsReminderFragment);
        });

        // Nút Logout
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    // 1. Hiển thị hộp thoại xác nhận
    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Log Out", (dialog, which) -> {
                    performSafeLogout();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // 2. Logic Logout an toàn
    private void performSafeLogout() {
        Toast.makeText(getContext(), "Logging out...", Toast.LENGTH_SHORT).show();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            navigateToLogin();
            return;
        }

        HabitRepository repository = new HabitRepository(uid);

        // Gọi hàm getActiveHabits từ Repository (dùng DataCallback chuẩn)
        repository.getActiveHabits(new DataCallback<List<Habit>>() {
            @Override
            public void onSuccess(List<Habit> habitList) {
                // 1. Hủy báo thức riêng lẻ
                NotificationHelper.cancelAllHabitReminders(requireContext(), habitList);

                // 2. Hủy báo thức tổng
                NotificationHelper.cancelDailyBriefing(requireContext());

                // 3. Xóa cài đặt cục bộ
                clearLocalPreferences();

                // 4. Đăng xuất Firebase
                FirebaseAuth.getInstance().signOut();

                // 5. Chuyển màn hình
                navigateToLogin();
            }

            @Override
            public void onFailure(Exception e) {
                // Lỗi mạng vẫn cho đăng xuất để tránh kẹt user
                FirebaseAuth.getInstance().signOut();
                navigateToLogin();
            }
        });
    }

    private void clearLocalPreferences() {
        SharedPreferences prefs = requireContext().getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void navigateToLogin() {
        navController.navigate(R.id.loginFragment, null,
                new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .build());

        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}