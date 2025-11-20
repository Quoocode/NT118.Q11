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
import com.example.habittracker.databinding.FragmentForgotPasswordNewBinding; // Tạo từ fragment_forgot_password_new.xml

public class ForgotPasswordNewFragment extends Fragment {

    private FragmentForgotPasswordNewBinding binding;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentForgotPasswordNewBinding.inflate(inflater, container, false);
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

        // Nút "Submit" (ID: container_submit)
        binding.containerSubmit.setOnClickListener(v -> {
            // TODO: Xử lý logic đổi mật khẩu
            Toast.makeText(getContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();

            // Quay về màn hình Login
            // (Chúng ta điều hướng về loginFragment, popUpTo để xóa hết các màn hình Quên mật khẩu)
            navController.popBackStack(R.id.loginFragment, false);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}