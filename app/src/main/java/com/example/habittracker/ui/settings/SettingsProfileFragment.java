package com.example.habittracker.ui.settings;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.RoundedBitmapDrawable; // Mới
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory; // Mới
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentSettingsProfileBinding;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SettingsProfileFragment extends Fragment {

    private FragmentSettingsProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private NavController navController;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            // [SỬA] Thay vì setImageBitmap, ta gọi hàm setCircularImage để bo tròn
                            setCircularImage(bitmap);

                            String encodedImage = encodeImage(bitmap);
                            uploadAvatarImmediately(encodedImage);

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Không tìm thấy ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        navController = NavHostFragment.findNavController(this);

        setupUI();
        loadUserData();
        setupListeners();
    }

    private void setupUI() {
        binding.inputEmail.setEnabled(false);
        binding.inputEmail.setFocusable(false);
        binding.inputEmail.setAlpha(0.6f);
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            try {
                navController.navigate(R.id.loginFragment);
            } catch (Exception e) {
                // Fallback
            }
            return;
        }

        binding.inputEmail.setText(user.getEmail());

        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        String bio = documentSnapshot.getString("bio");
                        String avatarBase64 = documentSnapshot.getString("avatarBase64");

                        binding.inputFullname.setText(fullName);
                        binding.inputBio.setText(bio);

                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(avatarBase64, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                // [SỬA] Bo tròn ảnh khi load từ DB lên
                                setCircularImage(decodedByte);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
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
        binding.btnBackProfile.setOnClickListener(v -> {
            navController.popBackStack();
        });

        binding.btnSaveProfile.setOnClickListener(v -> {
            saveUserProfile();
        });

        binding.imgProfileAvatar.setOnClickListener(v -> {
            pickImageLauncher.launch("image/*");
        });
    }

    // --- [MỚI] Hàm hỗ trợ hiển thị ảnh tròn ---
    private void setCircularImage(Bitmap bitmap) {
        if (getContext() == null) return;

        // Cắt ảnh thành hình vuông trước (lấy phần giữa) để khi bo tròn không bị méo thành hình trứng
        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap squareBitmap = Bitmap.createBitmap(bitmap,
                (bitmap.getWidth() - dimension) / 2,
                (bitmap.getHeight() - dimension) / 2,
                dimension, dimension);

        // Tạo Drawable bo tròn từ Bitmap vuông
        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
        roundedDrawable.setCircular(true);
        roundedDrawable.setAntiAlias(true);

        // Xóa background cũ đi để không bị lòi viền hình chữ nhật ở 4 góc
        binding.imgProfileAvatar.setBackground(null);

        binding.imgProfileAvatar.setImageDrawable(roundedDrawable);
    }

    private void uploadAvatarImmediately(String base64Image) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Đang cập nhật ảnh đại diện...", Toast.LENGTH_SHORT).show();

        db.collection("users").document(user.getUid())
                .update("avatarBase64", base64Image)
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Đã cập nhật ảnh đại diện!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("avatarBase64", base64Image);
                    db.collection("users").document(user.getUid())
                            .set(data, SetOptions.merge());

                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi cập nhật ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
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

        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(newName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateFirestoreTextData(user.getUid(), newName, user.getEmail(), newBio);
                    } else {
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Lỗi cập nhật Auth", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void updateFirestoreTextData(String userId, String name, String email, String bio) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("fullName", name);
        userData.put("email", email);
        userData.put("bio", bio);

        db.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Đã lưu thông tin!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 500;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();

        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}