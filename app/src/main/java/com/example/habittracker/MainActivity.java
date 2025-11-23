package com.example.habittracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.habittracker.R;
import com.example.habittracker.databinding.ActivityMainBinding; // <-- Tạo từ "activity_main.xml"
import com.example.habittracker.DatabaseStructure.HabitRepository;
import com.example.habittracker.DatabaseStructure.Habit;
import com.example.habittracker.DatabaseStructure.DataSeeder;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Sử dụng ViewBinding để liên kết layout "activity_main.xml"
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Tìm NavController từ NavHostFragment trong "content_main.xml"
        // (Chúng ta giả sử ID của NavHostFragment là 'nav_host_fragment_content_main')
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // 3. Liên kết NavController với BottomNavigationView
            // Thao tác này sẽ tự động xử lý việc "click" vào các item menu
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);

            // 4. Gọi hàm để quản lý ẩn/hiện thanh điều hướng
            setupBottomNavVisibility();
        }

        String user1_ID = "ML1NNiZM0XO2TPPtnUOoKi0nMHN2"; // <-- PASTE FULL UID VÀO ĐÂY
        DataSeeder seeder1 = new DataSeeder(user1_ID);
        seeder1.seedData();

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
                    destinationId == R.id.forgotPasswordFragment) { // <-- Thêm các ID fragment khác nếu cần

                binding.bottomNavigationView.setVisibility(View.GONE);
            } else {
                // Hiển thị ở các màn hình chính (Home, Calendar, Settings...)
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