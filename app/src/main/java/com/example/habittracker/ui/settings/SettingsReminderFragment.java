package com.example.habittracker.ui.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.MainActivity;
import com.example.habittracker.databinding.FragmentSettingsReminderBinding;
import com.example.habittracker.utils.NotificationHelper;

public class SettingsReminderFragment extends Fragment {

    private FragmentSettingsReminderBinding binding;

    // Bộ lắng nghe kết quả xin quyền (Dành cho Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(getContext(), "Đã cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
                    // Quyền đã cấp -> Bắn thử cái thông báo cho ngầu
                    NotificationHelper.showTestNotification(requireContext(), MainActivity.class);
                } else {
                    Toast.makeText(getContext(), "Bạn đã từ chối quyền thông báo.", Toast.LENGTH_SHORT).show();
                    binding.switchAllowNotifs.setChecked(false); // Tắt switch nếu không cho quyền
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

        // 1. Tạo kênh thông báo ngay khi vào màn hình (Quan trọng)
        NotificationHelper.createNotificationChannel(requireContext());

        NavController navController = NavHostFragment.findNavController(this);

        // Nút Back (Logic cũ của mày)
        binding.btnBackReminders.setOnClickListener(v -> {
            navController.popBackStack();
        });

        // 2. Xử lý Switch "Allow Notifications"
        binding.switchAllowNotifs.setOnClickListener(v -> {
            boolean isChecked = binding.switchAllowNotifs.isChecked();
            if (isChecked) {
                // Nếu bật -> Kiểm tra quyền
                checkAndRequestPermission();
            } else {
                Toast.makeText(getContext(), "Đã tắt thông báo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Nếu là Android 13 trở lên -> Phải xin quyền
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Đã có quyền -> Bắn test
                NotificationHelper.showTestNotification(requireContext(), MainActivity.class);
            } else {
                // Chưa có quyền -> Hiện popup xin
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Android cũ hơn -> Không cần xin, bắn luôn
            NotificationHelper.showTestNotification(requireContext(), MainActivity.class);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}