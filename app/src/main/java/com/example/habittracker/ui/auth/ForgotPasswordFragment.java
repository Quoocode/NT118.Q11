package com.example.habittracker.ui.auth;

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
import com.example.habittracker.databinding.FragmentForgotPasswordBinding; // Tạo từ fragment_forgot_password.xml

public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordBinding.inflate(inflater, container, false);
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

        // Nút "Send verification code" (ID: container_send_code)
        binding.containerSendCode.setOnClickListener(v -> {
            // TODO: Xử lý logic gửi email

            // Điều hướng đến màn hình nhập mật khẩu mới
            navController.navigate(R.id.action_forgotPasswordFragment_to_forgotPasswordNewFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}