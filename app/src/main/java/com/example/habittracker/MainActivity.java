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
import android.content.Context;
import com.example.habittracker.utils.LocaleHelper;
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private HabitViewModel habitViewModel;

    // [BỔ SUNG] Biến lắng nghe sự kiện đăng nhập/đăng xuất
    private FirebaseAuth.AuthStateListener authListener;

    // Bộ chặn (guard) để tránh gọi "record open" bị trùng trong cùng một process cho cùng một tài khoản.
    // Ở cold start, onCreate() có thể ghi nhận 1 lần rồi AuthStateListener lại callback ngay;
    // lưu UID đã ghi nhận gần nhất giúp tránh ghi nhận lặp và không làm sai điều kiện "Welcome Back".
    private String lastRecordedOpenUid = null;

    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Thành tựu cục bộ (local): ghi nhận sự kiện mở app để xét "Welcome Back".
        // Việc này phải chạy theo vòng đời ứng dụng, không phụ thuộc vào tab/màn hình Achievements.
        try {
            new com.example.habittracker.data.achievements.AchievementsRepository(this)
                    .recordAppOpenAndMaybeWelcomeBack();

            // Nếu đã đăng nhập sẵn ở cold start, lưu lại UID đã được ghi nhận,
            // để callback tức thì của AuthStateListener không ghi nhận thêm lần nữa.
            FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
            lastRecordedOpenUid = (current != null) ? current.getUid() : null;
        } catch (Exception ignored) {
            // Tránh crash loop nếu SharedPreferences bị lỗi/corrupt
        }

        // 1. Sử dụng ViewBinding để liên kết layout "activity_main.xml"
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Tìm NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // --- THIẾT LẬP GRAPH KHI KHỞI ĐỘNG LẦN ĐẦU ---
            // Nếu savedInstanceState != null, nghĩa là Activity đang được tạo lại (do xoay màn hình hoặc đổi theme)
            // Lúc này NavController sẽ tự động khôi phục trạng thái cũ, ta KHÔNG NÊN set lại graph.
            if (savedInstanceState == null) {
                // Lấy NavGraph hiện tại
                NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph);

                // Kiểm tra user hiện tại (Auto Login)
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

                if (currentUser != null) {
                    navGraph.setStartDestination(R.id.homeFragment);
                } else {
                    navGraph.setStartDestination(R.id.loginFragment);
                }

                // Gán lại đồ thị đã chỉnh sửa cho Controller
                navController.setGraph(navGraph);
            }
            // -----------------------------------------------------------

            // 3. Liên kết BottomNav (Luôn phải làm, kể cả khi recreate)
            NavigationUI.setupWithNavController(binding.bottomNavigationView, navController);

            // 4. Gọi hàm để quản lý ẩn/hiện thanh điều hướng
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

                        // Nếu bị chặn exact alarm (Android 12+), hỏi quyền 1 lần để nhắc nhở có thể đúng giờ.
                        if (!NotificationHelper.isExactAlarmAllowed(MainActivity.this)) {
                            NotificationHelper.showExactAlarmPermissionDialog(MainActivity.this);
                        }

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

                // Đồng thời ghi nhận sự kiện mở app theo từng user (để "Welcome Back" đúng theo tài khoản).
                // Có guard để tránh trùng ở cold start: onCreate() có thể đã ghi nhận trước đó.
                String uid = user.getUid();
                if (lastRecordedOpenUid == null || !lastRecordedOpenUid.equals(uid)) {
                    try {
                        new com.example.habittracker.data.achievements.AchievementsRepository(MainActivity.this)
                                .recordAppOpenAndMaybeWelcomeBack();
                        lastRecordedOpenUid = uid;
                    } catch (Exception ignored) {
                        // Bỏ qua lỗi
                    }
                }

                // Gọi ViewModel tải lại dữ liệu ngay lập tức
                if (habitViewModel != null) {
                    habitViewModel.loadActiveHabits();
                }
            } else {
                Log.d("ALARM_DEBUG", "AuthStateListener: User đã đăng xuất.");
                // Reset để lần đăng nhập tiếp theo (hot swap) vẫn ghi nhận lại.
                lastRecordedOpenUid = null;
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