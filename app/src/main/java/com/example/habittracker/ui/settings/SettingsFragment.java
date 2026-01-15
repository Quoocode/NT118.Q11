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
import com.example.habittracker.data.repository.callback.DataCallback;
import com.example.habittracker.databinding.FragmentSettingsBinding;
// Import ThemeHelper để xử lý giao diện Sáng/Tối
import com.example.habittracker.utils.ThemeHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.Intent;
import com.example.habittracker.MainActivity;
import com.example.habittracker.utils.LocaleHelper;
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

        // --- LOGIC THEME ---
        // Cập nhật icon ban đầu
        updateThemeIcon();

        // Xử lý click nút Theme
        binding.btnThemeSettings.setOnClickListener(v -> {
            if (getContext() != null) {
                // Lấy trạng thái hiện tại
                boolean isDark = ThemeHelper.isDarkMode(getContext());

                // Lưu và áp dụng trạng thái mới (Đảo ngược)
                ThemeHelper.saveThemeChoice(getContext(), !isDark);

                // Cập nhật icon (thực tế Activity sẽ recreate nên hàm updateThemeIcon ở trên sẽ chạy lại)
                updateThemeIcon();
            }
        });

        // --- CÁC NÚT ĐIỀU HƯỚNG ---
        binding.btnLanguageSettings.setOnClickListener(v -> {
            showLanguageDialog();
        });
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

    // Hàm cập nhật icon Sun/Moon dựa trên chế độ hiện tại
    private void updateThemeIcon() {
        if (getContext() == null || binding == null) return;

        boolean isDark = ThemeHelper.isDarkMode(getContext());

        if (isDark) {
            // Nếu đang Tối -> Hiện icon Mặt Trăng
            binding.imgThemeIcon.setImageResource(R.drawable.ic_moon);
        } else {
            // Nếu đang Sáng -> Hiện icon Mặt Trời
            binding.imgThemeIcon.setImageResource(R.drawable.ic_sun);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi màn hình này hiện lên
        loadUserProfile();
        updateThemeIcon();
    }

    // LOGIC HIỂN THỊ PROFILE

    private void loadUserProfile() {
        if (binding == null) return;

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        updateNameDisplay(user.getDisplayName());

        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (binding == null) return;

                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String avatarBase64 = documentSnapshot.getString("avatarBase64");

                        if (fullName != null && !fullName.isEmpty()) {
                            updateNameDisplay(fullName);
                        }

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
            LinearLayout parent = binding.btnProfileSettings;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof LinearLayout) {
                    LinearLayout textContainer = (LinearLayout) child;
                    for (int j = 0; j < textContainer.getChildCount(); j++) {
                        View textChild = textContainer.getChildAt(j);
                        if (textChild instanceof TextView) {
                            ((TextView) textChild).setText(name);
                            return;
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

        avatarView.setImageDrawable(roundedDrawable);
        avatarView.setBackground(null);
        avatarView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    // PHẦN 2: LOGIC LOGOUT

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

        repository.getActiveHabits(new DataCallback<List<Habit>>() {
            @Override
            public void onSuccess(List<Habit> habitList) {
                if (getContext() != null) {
                    NotificationHelper.cancelAllHabitReminders(requireContext(), habitList);
                    NotificationHelper.cancelDailyBriefing(requireContext());
                    clearLocalPreferences();
                }
                FirebaseAuth.getInstance().signOut();
                navigateToLogin();
            }

            @Override
            public void onFailure(Exception e) {
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

    private void changeLanguage(String langCode) {
        // Lưu & áp dụng locale
        com.example.habittracker.utils.LocaleHelper.setLocale(requireContext(), langCode);

        // Restart app + clear toàn bộ back stack (FIX Create Habit)
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);

        // Kết thúc activity hiện tại
        requireActivity().finish();
    }





    private void showLanguageDialog() {
        if (getContext() == null) return;

        String[] languages = {
                getString(R.string.english),
                getString(R.string.vietnamese)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.choose_language))
                .setItems(languages, (dialog, which) -> {
                    if (which == 0) {
                        changeLanguage("en");
                    } else {
                        changeLanguage("vi");
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}