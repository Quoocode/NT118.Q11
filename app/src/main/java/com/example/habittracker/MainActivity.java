package com.example.habittracker;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph; // Import mới
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.habittracker.R;
import com.example.habittracker.databinding.ActivityMainBinding;
import com.example.habittracker.DatabaseStructure.HabitRepository;
import com.example.habittracker.DatabaseStructure.Habit;
import com.example.habittracker.DatabaseStructure.DataSeeder;
import com.google.firebase.auth.FirebaseAuth; // Import mới
import com.google.firebase.auth.FirebaseUser; // Import mới

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Sử dụng ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Tìm NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // --- [MỚI] LOGIC KIỂM TRA ĐĂNG NHẬP (AUTO LOGIN) ---

            // Lấy NavGraph hiện tại (được khai báo trong nav_graph.xml)
            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph); // Thay nav_graph bằng tên file xml điều hướng của bạn nếu khác

            // Kiểm tra user hiện tại
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                // Nếu đã đăng nhập -> Chuyển hướng thẳng vào Home
                navGraph.setStartDestination(R.id.homeFragment);
            } else {
                // Nếu chưa -> Vào Login (Mặc định)
                navGraph.setStartDestination(R.id.loginFragment);
            }

            // Gán lại đồ thị đã chỉnh sửa cho Controller
            navController.setGraph(navGraph);
            // ---------------------------------------------------

            // 3. Liên kết BottomNav
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);

            // 4. Quản lý ẩn/hiện thanh điều hướng
            setupBottomNavVisibility();
        }

        // --- ĐOẠN DATA SEEDER (Có thể bỏ hoặc comment lại sau này) ---
        // Chỉ chạy khi user đã đăng nhập để tránh lỗi null UID
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String user1_ID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // DataSeeder seeder1 = new DataSeeder(user1_ID);
            // seeder1.seedData();
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