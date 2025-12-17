package com.example.habittracker.ui.auth;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.Credential;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.habittracker.R;
// 1. Đảm bảo bạn đã import đúng file ViewBinding (tạo từ 'fragment_login.xml')
import com.example.habittracker.databinding.FragmentLoginBinding;
import com.example.habittracker.databinding.FragmentRegisterBinding;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import android.os.CancellationSignal;
import android.util.Log;

import java.util.concurrent.Executors;

import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.CredentialManagerCallback;

public class LoginFragment extends Fragment {

    // 2. Khai báo biến binding và navController
    private FragmentLoginBinding binding;
    private NavController navController;
    // private AuthViewModel authViewModel; // Sẽ dùng ở bước sau
    private FirebaseAuth mAuth;
    private static final String TAG = "GoogleAuth";
    private CredentialManager credentialManager; // KHAI BÁO MỚI
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 3. Inflate layout bằng ViewBinding
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(requireContext());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 4. Khởi tạo NavController
        // (Rất quan trọng! Nếu 'navController' bị null, app sẽ crash)
        navController = NavHostFragment.findNavController(this);
        // AUTO LOGIN
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            navController.navigate(R.id.action_loginFragment_to_homeFragment);
//            return;
//        }
        // TODO: Khởi tạo AuthViewModel
        // authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 5. GÁN SỰ KIỆN CLICK CHO TẤT CẢ CÁC NÚT

        // Nút "Log In" (ID: container_login_20)
        binding.containerLogin20.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString();
            String password = binding.editPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập Email và Mật khẩu", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();

                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(getContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                navController.navigate(R.id.action_loginFragment_to_homeFragment);
                            } else {
                                Toast.makeText(
                                        getContext(),
                                        "Vui lòng xác thực email trước khi đăng nhập!",
                                        Toast.LENGTH_LONG
                                ).show();
                                mAuth.signOut();
                            }
                        } else {
                            Toast.makeText(getContext(), "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
                        }
                    });

//            // TODO: Gọi authViewModel.login(email, password)
//            Toast.makeText(getContext(), "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
//
//            // Chuyển đến trang chính
//            navController.navigate(R.id.action_loginFragment_to_homeFragment);
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
//            Toast.makeText(getContext(), "Chức năng đăng nhập Google", Toast.LENGTH_SHORT).show();
//            // TODO: Xử lý logic đăng nhập Google
            signInWithGoogle();
        });

        // Nút "Facebook" (ID: container_login_32_38)
        binding.containerLogin3238.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Chức năng đăng nhập Facebook", Toast.LENGTH_SHORT).show();
            // TODO: Xử lý logic đăng nhập Facebook
        });

        // === ĐÂY LÀ PHẦN SỬA LỖI CỦA BẠN ===
        // Chữ "Continue as Guest" (ID: container_login_40)
        binding.containerLogin40.setOnClickListener(v -> {
            mAuth.signInAnonymously()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(getContext(), "Đăng nhập với tư cách Khách", Toast.LENGTH_SHORT).show();
                            navController.navigate(R.id.action_loginFragment_to_homeFragment);
                        } else {
                            Toast.makeText(getContext(), "Guest login thất bại!", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

    }


    private void signInWithGoogle() {
        String serverClientId = getString(R.string.default_web_client_id);

        // 🌟 SỬ DỤNG API CHO NÚT SIGN-IN (dialog)
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(serverClientId)
                        .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();

        credentialManager.getCredentialAsync(
                requireActivity(),
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(@NonNull GetCredentialResponse result) {
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.e(TAG, "Error: " + e.getLocalizedMessage());
                        Toast.makeText(getContext(), "Google Sign-In thất bại!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleSignIn(Credential credential) {
        if (credential instanceof CustomCredential) {

            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());

                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                return;
            }
        }

        Toast.makeText(getContext(), "Không phải Google credential!", Toast.LENGTH_SHORT).show();
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        // Đăng nhập thành công, chuyển hướng người dùng
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getContext(), "Đăng nhập Google thành công!", Toast.LENGTH_SHORT).show();

                        // Chuyển đến trang chính
                        navController.navigate(R.id.action_loginFragment_to_homeFragment);
                    } else {
                        // Đăng nhập thất bại
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(getContext(), "Xác thực Firebase thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 6. Luôn giải phóng binding ở đây
        binding = null;
    }
}