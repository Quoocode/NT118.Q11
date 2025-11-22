package com.example.habittracker.ui.auth;
import android.util.Patterns;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private NavController navController;
    private FirebaseAuth firebaseAuth;
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
        firebaseAuth = FirebaseAuth.getInstance();

        // Nút "Back" (mũi tên)
        binding.btnBack.setOnClickListener(v -> {
            navController.popBackStack(); // Quay lại màn hình trước đó
        });

        // Nút "Sign Up" (ID: container_depth_frame15)
        binding.containerDepthFrame15.setOnClickListener(v -> {
            // TODO: Xử lý logic đăng ký
//            Toast.makeText(getContext(), "Đang đăng ký...", Toast.LENGTH_SHORT).show();
            registerUser();
            // Điều hướng về Login sau khi đăng ký thành công
//            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        });

        // Chữ "Already have an account? Sign In" (ID: text_sign_in)
        binding.textSignIn.setOnClickListener(v -> {
            // Điều hướng về Login
            navController.navigate(R.id.action_registerFragment_to_loginFragment);
        });
    }

    private void registerUser(){
        String email = binding.editEmail.getText().toString().trim();
        String password = binding.editPassword.getText().toString().trim();
        String confirmPassword = binding.editConfirmPassword.getText().toString().trim();

        if (email.isEmpty()){
            binding.editEmail.setError("Email is required");
            binding.editEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editEmail.setError("Please enter a valid email");
            binding.editEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            binding.editPassword.setError("Password is required");
            binding.editPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            binding.editPassword.setError("Password must be at least 6 characters");
            binding.editPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            binding.editConfirmPassword.setError("ConfirmPassword is required");
            binding.editConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            binding.editConfirmPassword.setError("ConfirmPasswords and PassWords do not match");
            binding.editConfirmPassword.requestFocus();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(getContext(), "Failed to send verification email: " + verifyTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        // Điều hướng về Login
                                        navController.navigate(R.id.action_registerFragment_to_loginFragment);
                                    });
                        }
                    }
                    else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(getContext(), "This email is already registered.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}