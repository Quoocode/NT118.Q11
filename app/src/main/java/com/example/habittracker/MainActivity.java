package com.example.habittracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph; // Import mới
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.habittracker.R;
import com.example.habittracker.databinding.ActivityMainBinding;
import com.example.habittracker.data.model.Habit; // Mới
import com.example.habittracker.ui.ViewModel.HabitViewModel; // Mới: Đảm bảo package đúng với file bạn vừa tạo
import com.example.habittracker.utils.NotificationHelper; // Mới: Đảm bảo package đúng

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private HabitViewModel habitViewModel; // Khai báo ViewModel

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // 1. Sử dụng ViewBinding để liên kết layout "activity_main.xml"
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Tìm NavController từ NavHostFragment trong "content_main.xml"
        // (Chúng ta giả sử ID của NavHostFragment là 'nav_host_fragment_content_main')
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // --- [MỚI] LOGIC KIỂM TRA ĐĂNG NHẬP (AUTO LOGIN) ---

            // Lấy NavGraph hiện tại
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);

            // Kiểm tra user hiện tại
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                navGraph.setStartDestination(R.id.homeFragment);
            } else {
                navGraph.setStartDestination(R.id.loginFragment);
            }

            // Gán lại đồ thị đã chỉnh sửa cho Controller
            navController.setGraph(navGraph);
            // ---------------------------------------------------

            // 3. Liên kết BottomNav
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);

            // 4. Gọi hàm để quản lý ẩn/hiện thanh điều hướng
            setupBottomNavVisibility();
        }

        // --- ĐOẠN DATA SEEDER (Có thể bỏ hoặc comment lại sau này) ---
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String user1_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // DataSeeder seeder1 = new DataSeeder(user1_ID);
            // seeder1.seedData();
        }

        // 5. [QUAN TRỌNG] Kích hoạt quan sát dữ liệu để đặt báo thức
        setupHabitObserver();
    }

    private void setupHabitObserver() {
        try {
            // Khởi tạo ViewModel
            habitViewModel = new ViewModelProvider(this).get(HabitViewModel.class);

            // A. Lắng nghe dữ liệu (Observe)
            // Khi nào loadActiveHabits chạy xong, nó sẽ bắn dữ liệu vào đây
            habitViewModel.getHabits().observe(this, new Observer<List<Habit>>() {
                @Override
                public void onChanged(List<Habit> habits) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                    if (user != null && habits != null && !habits.isEmpty()) {
                        Log.d("ALARM_DEBUG", "MainActivity: Đã tải được " + habits.size() + " thói quen. Tiến hành đặt báo thức...");

                        // Gọi NotificationHelper để đặt báo thức
                        NotificationHelper.scheduleAllHabitReminders(MainActivity.this, habits);
                    } else {
                        Log.d("ALARM_DEBUG", "MainActivity: Dữ liệu thói quen trống hoặc User chưa đăng nhập.");
                    }
                }
            });

            // B. Kích hoạt tải dữ liệu ngay nếu đã đăng nhập
            // Nếu không có dòng này, Observer ở trên sẽ ngồi chơi xơi nước mãi mãi
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                Log.d("ALARM_DEBUG", "MainActivity: Đã đăng nhập, gọi ViewModel tải dữ liệu ngay.");
                habitViewModel.loadActiveHabits();
            }

        } catch (Exception e) {
            Log.e("ALARM_DEBUG", "Lỗi setup Observer trong MainActivity: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupBottomNavVisibility() {
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {

            // Lấy ID của màn hình (Fragment) hiện tại
            int destinationId = destination.getId();

            // Ẩn BottomNav ở các màn hình Xác thực và màn hình Tạo/Sửa
            if (destinationId == R.id.loginFragment ||
                    destinationId == R.id.registerFragment ||
                    destinationId == R.id.addEditHabitFragment ||
                    destinationId == R.id.habitDetailsFragment ||
                    destinationId == R.id.forgotPasswordFragment ||
                    destinationId == R.id.forgotPasswordNewFragment) {
                binding.bottomNavigationView.setVisibility(View.GONE);
            } else {
                // Hiển thị ở các màn hình chính (Home, Calendar, Achievements, Settings...)
                binding.bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });
    }

    // Hỗ trợ nút back của hệ thống để điều hướng
    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }
}