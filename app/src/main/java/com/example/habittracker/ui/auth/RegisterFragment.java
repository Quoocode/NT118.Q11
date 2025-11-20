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
import com.example.habittracker.databinding.FragmentRegisterBinding; // Tạo từ fragment_register.xml

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Nút "Back" (mũi tên)
        binding.btnBack.setOnClickListener(v -> {
            navController.popBackStack(); // Quay lại màn hình trước đó
        });

        // Nút "Sign Up" (ID: container_depth_frame15)
        binding.containerDepthFrame15.setOnClickListener(v -> {
            // TODO: Xử lý logic đăng ký
            Toast.makeText(getContext(), "Đang đăng ký...", Toast.LENGTH_SHORT).show();

            // Điều hướng về Login sau khi đăng ký thành công
            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        });

        // Chữ "Already have an account? Sign In" (ID: text_sign_in)
        binding.textSignIn.setOnClickListener(v -> {
            // Điều hướng về Login
            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}