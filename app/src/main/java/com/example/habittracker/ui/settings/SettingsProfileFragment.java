package com.example.habittracker.ui.settings;

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

// Import R để sử dụng resource ID (vì package hiện tại là ui.settings)
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentSettingsProfileBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SettingsProfileFragment extends Fragment {

    private FragmentSettingsProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo các thành phần cốt lõi
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        navController = NavHostFragment.findNavController(this);

        // 1. Cấu hình giao diện ban đầu
        setupUI();

        // 2. Tải dữ liệu User hiện tại
        loadUserData();

        // 3. Xử lý sự kiện nút bấm
        setupListeners();
    }

    private void setupUI() {
        // Yêu cầu: Email chỉ được xem, không được sửa
        binding.inputEmail.setEnabled(false);
        binding.inputEmail.setFocusable(false);
        binding.inputEmail.setAlpha(0.6f); // Làm mờ nhẹ để user hiểu là disable
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Nếu mất session, đá về màn hình login (cần đảm bảo ID action đúng trong nav_graph)
            // Nếu không có action global, có thể navigate bằng ID destination
            try {
                navController.navigate(R.id.loginFragment);
            } catch (Exception e) {
                // Fallback nếu ID không tìm thấy hoặc lỗi nav
                Toast.makeText(getContext(), "Phiên đăng nhập hết hạn", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        // Hiển thị Email từ Auth (chính xác nhất)
        binding.inputEmail.setText(user.getEmail());

        // Thử lấy dữ liệu chi tiết từ Firestore (Bio, Name mới nhất)
        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Nếu đã có dữ liệu trong Firestore -> Ưu tiên hiển thị
                        String fullName = documentSnapshot.getString("fullName");
                        String bio = documentSnapshot.getString("bio");

                        binding.inputFullname.setText(fullName);
                        binding.inputBio.setText(bio);
                    } else {
                        // Nếu chưa có trong Firestore -> Lấy tên từ Auth hiển thị tạm
                        binding.inputFullname.setText(user.getDisplayName());
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupListeners() {
        // Nút Back
        binding.btnBackProfile.setOnClickListener(v -> {
            navController.popBackStack();
        });

        // Nút Save Changes
        binding.btnSaveProfile.setOnClickListener(v -> {
            saveUserProfile();
        });
    }

    private void saveUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String newName = binding.inputFullname.getText().toString().trim();
        String newBio = binding.inputBio.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.inputFullname.setError("Tên không được để trống");
            return;
        }

        // Hiển thị loading hoặc disable nút để tránh spam click (tuỳ chọn nâng cao)
        // binding.btnSaveProfile.setEnabled(false);

        // --- BƯỚC 1: Cập nhật Firebase Auth (DisplayName) ---
        // Để các phần khác của app hiển thị đúng tên ngay lập tức
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // --- BƯỚC 2: Cập nhật Firestore (Bio + Name + Email) ---
                        updateFirestoreData(user.getUid(), newName, user.getEmail(), newBio);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi cập nhật Profile Auth", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateFirestoreData(String userId, String name, String email, String bio) {
        // Tạo Map dữ liệu để đẩy lên
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", name);
        userData.put("email", email);
        userData.put("bio", bio);

        // Dùng SetOptions.merge() để không ghi đè mất các trường khác (như avatar nếu đã có)
        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                        // Có thể popBackStack() nếu muốn thoát sau khi lưu
                        // navController.popBackStack();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}