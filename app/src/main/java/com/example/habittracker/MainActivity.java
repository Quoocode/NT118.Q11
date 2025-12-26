package com.example.habittracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.habittracker.databinding.ActivityMainBinding;
import com.example.habittracker.data.model.Habit;
import com.example.habittracker.ui.ViewModel.HabitViewModel;
import com.example.habittracker.utils.NotificationHelper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private HabitViewModel habitViewModel;

    // [BỔ SUNG] Biến lắng nghe sự kiện đăng nhập/đăng xuất
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // --- LOGIC KIỂM TRA ĐĂNG NHẬP (AUTO LOGIN) ---
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                navGraph.setStartDestination(R.id.homeFragment);
            } else {
                navGraph.setStartDestination(R.id.loginFragment);
            }
            navController.setGraph(navGraph);
            // ---------------------------------------------------

            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);
            setupBottomNavVisibility();
        }

        // --- ĐOẠN DATA SEEDER ---
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String user1_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // DataSeeder seeder1 = new DataSeeder(user1_ID);
            // seeder1.seedData();
        }

        // 5. Kích hoạt quan sát dữ liệu
        setupHabitObserver();

        // [BỔ SUNG] Khởi tạo Auth Listener (Chuẩn bị cho onStart)
        setupAuthStateListener();
    }

    private void setupHabitObserver() {
        try {
            habitViewModel = new ViewModelProvider(this).get(HabitViewModel.class);

            habitViewModel.getHabits().observe(this, new Observer<List<Habit>>() {
                @Override
                public void onChanged(List<Habit> habits) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if (user != null && habits != null && !habits.isEmpty()) {
                        Log.d("ALARM_DEBUG", "MainActivity: Đã tải được " + habits.size() + " thói quen. Tiến hành đặt báo thức...");
                        NotificationHelper.scheduleAllHabitReminders(MainActivity.this, habits);
                    } else {
                        Log.d("ALARM_DEBUG", "MainActivity: Dữ liệu thói quen trống hoặc User chưa đăng nhập.");
                    }
                }
            });

            // Logic cũ vẫn giữ nguyên để đảm bảo Cold Start chạy ổn định
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                Log.d("ALARM_DEBUG", "MainActivity: Đã đăng nhập (Cold Start), gọi ViewModel tải dữ liệu.");
                habitViewModel.loadActiveHabits();
            }

        } catch (Exception e) {
            Log.e("ALARM_DEBUG", "Lỗi setup Observer trong MainActivity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // [BỔ SUNG] Hàm định nghĩa hành động khi trạng thái Auth thay đổi
    private void setupAuthStateListener() {
        authListener = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // Đây là "Cái Chuông" reo khi Login (kể cả Hot Swap)
                Log.d("ALARM_DEBUG", "AuthStateListener: Phát hiện User mới đăng nhập: " + user.getUid());

                // Gọi ViewModel tải lại dữ liệu ngay lập tức
                if (habitViewModel != null) {
                    habitViewModel.loadActiveHabits();
                }
            } else {
                Log.d("ALARM_DEBUG", "AuthStateListener: User đã đăng xuất.");
            }
        };
    }

    // [BỔ SUNG] Bật lắng nghe khi App hiện lên
    @Override
    protected void onStart() {
        super.onStart();
        if (authListener != null) {
            FirebaseAuth.getInstance().addAuthStateListener(authListener);
        }
    }

    // [BỔ SUNG] Tắt lắng nghe khi App ẩn đi/thoát
    @Override
    protected void onStop() {
        super.onStop();
        if (authListener != null) {
            FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        }
    }

    private void setupBottomNavVisibility() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destinationId = destination.getId();
            if (destinationId == R.id.loginFragment ||
                    destinationId == R.id.registerFragment ||
                    destinationId == R.id.addEditHabitFragment ||
                    destinationId == R.id.habitDetailsFragment ||
                    destinationId == R.id.forgotPasswordFragment) {
                binding.bottomNavigationView.setVisibility(View.GONE);
            } else {
                binding.bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}