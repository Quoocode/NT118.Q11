package com.example.habittracker.ui.auth;

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

import com.example.habittracker.R;
// 1. Đảm bảo bạn đã import đúng file ViewBinding (tạo từ 'fragment_login.xml')
import com.example.habittracker.databinding.FragmentLoginBinding;

public class LoginFragment extends Fragment {

    // 2. Khai báo biến binding và navController
    private FragmentLoginBinding binding;
    private NavController navController;
    // private AuthViewModel authViewModel; // Sẽ dùng ở bước sau

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 3. Inflate layout bằng ViewBinding
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 4. Khởi tạo NavController
        // (Rất quan trọng! Nếu 'navController' bị null, app sẽ crash)
        navController = NavHostFragment.findNavController(this);

        // TODO: Khởi tạo AuthViewModel
        // authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 5. GÁN SỰ KIỆN CLICK CHO TẤT CẢ CÁC NÚT

        // Nút "Log In" (ID: container_login_20)
        binding.containerLogin20.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString();
            String password = binding.editPassword.getText().toString();

            // TODO: Gọi authViewModel.login(email, password)
            Toast.makeText(getContext(), "Đang đăng nhập...", Toast.LENGTH_SHORT).show();

            // Chuyển đến trang chính
            navController.navigate(R.id.action_loginFragment_to_homeFragment);
        });

        // Nút "Sign Up" (ID: container_login_24)
        binding.containerLogin24.setOnClickListener(v -> {
            // Chuyển đến trang Đăng ký
            navController.navigate(R.id.action_loginFragment_to_registerFragment);
        });

        // Chữ "Forgot your password" (ID: container_login_28)
        binding.containerLogin28.setOnClickListener(v -> {
            // Chuyển đến trang Quên mật khẩu
            navController.navigate(R.id.action_loginFragment_to_forgotPasswordFragment);
        });

        // Nút "Google" (ID: container_login_32_34_35)
        binding.containerLogin323435.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng đăng nhập Google", Toast.LENGTH_SHORT).show();
            // TODO: Xử lý logic đăng nhập Google
        });

        // Nút "Facebook" (ID: container_login_32_38)
        binding.containerLogin3238.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng đăng nhập Facebook", Toast.LENGTH_SHORT).show();
            // TODO: Xử lý logic đăng nhập Facebook
        });

        // === ĐÂY LÀ PHẦN SỬA LỖI CỦA BẠN ===
        // Chữ "Continue as Guest" (ID: container_login_40)
        binding.containerLogin40.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Tiếp tục với tư cách Khách", Toast.LENGTH_SHORT).show();

            // Chuyển đến trang chính (HomeFragment)
            // (ID 'action_loginFragment_to_homeFragment' phải tồn tại trong nav_graph.xml)
            navController.navigate(R.id.action_loginFragment_to_homeFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 6. Luôn giải phóng binding ở đây
        binding = null;
    }
}