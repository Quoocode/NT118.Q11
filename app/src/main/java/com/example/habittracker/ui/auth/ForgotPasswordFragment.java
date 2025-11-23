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

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentForgotPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;


public class ForgotPasswordFragment extends Fragment {

    private FragmentForgotPasswordBinding binding;
    private NavController navController;
    private FirebaseAuth mAuth;


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
            String email = binding.editEmail.getText().toString().trim();
            mAuth = FirebaseAuth.getInstance();
            if(email.isEmpty()){
                Toast.makeText(getContext(), "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                return;
            }
            // Điều hướng đến màn hình nhập mật khẩu mới
//            navController.navigate(R.id.action_forgotPasswordFragment_to_forgotPasswordNewFragment);
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            Toast.makeText(getContext(),
                                    "Email đặt lại mật khẩu đã được gửi! Vui lòng kiểm tra hộp thư đến.",
                                    Toast.LENGTH_LONG).show();

                            // Chuyển đến màn hình nhập mật khẩu mới (nếu cần)
                            navController.navigate(R.id.action_forgotPasswordFragment_to_forgotPasswordNewFragment);
                        } else {
                            Toast.makeText(getContext(),
                                    "Lỗi: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}