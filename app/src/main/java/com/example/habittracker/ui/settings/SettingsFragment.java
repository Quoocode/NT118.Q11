package com.example.habittracker.ui.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.utils.NotificationHelper;
import com.example.habittracker.R;
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.HabitRepository;
// Lưu ý: Đảm bảo import đúng DataCallback. Nếu nó nằm ở Utils thì sửa lại đường dẫn.
import com.example.habittracker.data.repository.callback.DataCallback;
import com.example.habittracker.databinding.FragmentSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private NavController navController;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

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

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // --- CÁC NÚT ĐIỀU HƯỚNG ---
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        binding.btnProfileSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_settingsFragment_to_settingsProfileFragment);
        });

        binding.btnRemindersSettings.setOnClickListener(v -> {
            navController.navigate(R.id.action_settingsFragment_to_settingsReminderFragment);
        });

        // --- KHÔI PHỤC CHỨC NĂNG LOGOUT ---
        binding.btnLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });

        // --- TẢI DỮ LIỆU PROFILE (HÌNH ẢNH/TÊN) ---
        loadUserProfile();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi màn hình này hiện lên (để cập nhật Avatar mới nếu có)
        loadUserProfile();
    }

    // =================================================================================
    // PHẦN 1: LOGIC HIỂN THỊ PROFILE (AVATAR & TÊN) - GIỮ NGUYÊN CODE MỚI
    // =================================================================================

    private void loadUserProfile() {
        // Kiểm tra an toàn
        if (binding == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // 1. Hiển thị tạm tên từ Auth
        updateNameDisplay(user.getDisplayName());

        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Kiểm tra binding lần nữa vì callback là async
                    if (binding == null) return;

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String avatarBase64 = documentSnapshot.getString("avatarBase64");

                        // 2. Cập nhật tên từ Firestore
                        if (fullName != null && !fullName.isEmpty()) {
                            updateNameDisplay(fullName);
                        }

                        // 3. Cập nhật Avatar
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(avatarBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                if (decodedByte != null) {
                                    setCircularImage(decodedByte);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }

    private void updateNameDisplay(String name) {
        if (name == null || binding == null) return;
        try {
            // Tìm container chứa text (thường là LinearLayout thứ 2 trong btnProfileSettings)
            LinearLayout parent = binding.btnProfileSettings;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                // Tìm LinearLayout con chứa các TextView
                if (child instanceof LinearLayout) {
                    LinearLayout textContainer = (LinearLayout) child;
                    // Tìm TextView đầu tiên trong container này
                    for (int j = 0; j < textContainer.getChildCount(); j++) {
                        View textChild = textContainer.getChildAt(j);
                        if (textChild instanceof TextView) {
                            ((TextView) textChild).setText(name);
                            return; // Tìm thấy thì set và thoát luôn
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCircularImage(Bitmap bitmap) {
        if (getContext() == null || binding == null) return;

        ImageView avatarView = null;
        try {
            // Tìm ImageView bằng cách duyệt children
            ViewGroup parent = binding.btnProfileSettings;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof ImageView) {
                    avatarView = (ImageView) child;
                    break;
                }
            }
        } catch (Exception e) { return; }

        if (avatarView == null) return;

        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap,
                (bitmap.getWidth() - dimension) / 2,
                (bitmap.getHeight() - dimension) / 2,
                dimension, dimension);

        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
        roundedDrawable.setCircular(true);
        roundedDrawable.setAntiAlias(true);

        // Cập nhật hiển thị
        avatarView.setImageDrawable(roundedDrawable);
        avatarView.setBackground(null); // Xóa background nếu có
        avatarView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    // =================================================================================
    // PHẦN 2: LOGIC LOGOUT - KHÔI PHỤC LẠI
    // =================================================================================

    // 1. Hiển thị hộp thoại xác nhận
    private void showLogoutConfirmationDialog() {
        if (getContext() == null) return;

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
        if (getContext() != null) {
            Toast.makeText(getContext(), "Logging out...", Toast.LENGTH_SHORT).show();
        }

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
                if (getContext() != null) {
                    // 1. Hủy báo thức riêng lẻ
                    NotificationHelper.cancelAllHabitReminders(requireContext(), habitList);

                    // 2. Hủy báo thức tổng
                    NotificationHelper.cancelDailyBriefing(requireContext());

                    // 3. Xóa cài đặt cục bộ
                    clearLocalPreferences();
                }

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
        if (getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences("HabitTrackerPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    private void navigateToLogin() {
        navController.navigate(R.id.loginFragment, null,
                new androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .build());

        if (getContext() != null) {
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}