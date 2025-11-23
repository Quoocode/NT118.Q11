package com.example.habittracker.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habittracker.R;
import com.example.habittracker.data.repository.callback.StreakCallback;
import com.example.habittracker.databinding.FragmentHomeBinding;

// Import các thành phần chúng ta đã làm
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.ui.adapter.DashboardHabitAdapter; // Hoặc HabitAdapter tùy tên bạn đặt
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.data.repository.callback.SimpleCallback;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NavController navController;

    // Variables
    private HabitRepository habitRepository;
    private DashboardHabitAdapter habitAdapter;
    private List<HabitDailyView> todayHabitList = new ArrayList<>();
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // 1. Khởi tạo Repository
        currentUserId = FirebaseAuth.getInstance().getUid();
        habitRepository = new HabitRepository(currentUserId);

        // 2. Thiết lập RecyclerView
        setupRecyclerView();

        // 3. Sự kiện nút "Add Habit"
        // (Đảm bảo ID btnAddHabitToday khớp với file XML fragment_home.xml của bạn)
        if (binding.btnAddHabitToday != null) {
            binding.btnAddHabitToday.setOnClickListener(v -> {
                // Chuyển sang màn hình Add (không gửi ID)
                navController.navigate(com.example.habittracker.R.id.action_homeFragment_to_addEditHabitFragment);
            });
        }
        loadStreakData();
    }

    private void setupRecyclerView() {
        // Kiểm tra binding để tránh crash
        if (binding.recyclerViewHabits == null) {
            Log.e(TAG, "LỖI: Không tìm thấy RecyclerView (recyclerViewHabits) trong layout!");
            return;
        }

        binding.recyclerViewHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        // Khởi tạo Adapter
        habitAdapter = new DashboardHabitAdapter(getContext(), todayHabitList,

                // Listener 1: Click vào nút Check -> Mở Dialog Check-in
                new DashboardHabitAdapter.OnHabitCheckListener() {
                    @Override
                    public void onHabitCheckClick(HabitDailyView habit) {
                        // Gọi hàm hiển thị Dialog nhập số liệu
                        showCheckInDialog(habit);
                    }
                },

                // Listener 2: Click vào Item -> Chuyển sang màn hình Detail
                new DashboardHabitAdapter.OnHabitItemClickListener() {
                    @Override
                    public void onItemClick(HabitDailyView habit) {
                        Bundle bundle = new Bundle();
                        bundle.putString("EXTRA_HABIT_ID", habit.getHabitId());
                        // Chuyển sang màn hình Detail (như bạn yêu cầu)
                        navController.navigate(com.example.habittracker.R.id.action_homeFragment_to_habitDetailsFragment, bundle);
                    }
                }
        );

        binding.recyclerViewHabits.setAdapter(habitAdapter);
    }

    /**
     * Hàm hiển thị DialogFragment để Check-in (Nhập số liệu)
     */
    private void showCheckInDialog(HabitDailyView habit) {
        // Tạo instance của Dialog với dữ liệu hiện tại của habit
        HabitCheckInDialogFragment dialog = HabitCheckInDialogFragment.newInstance(
                habit.getHabitId(),
                habit.getTitle(),
                habit.getTargetValue(),
                habit.getCurrentValue(),
                habit.getUnit(),
                habit.getStatus()
        );

        // Lắng nghe sự kiện khi Dialog check-in xong (để reload list)
        dialog.setOnCheckInListener(new HabitCheckInDialogFragment.OnCheckInListener() {
            @Override
            public void onCheckInCompleted() {
                Log.d(TAG, "Check-in hoàn tất, tải lại danh sách...");
                loadHabitsForToday();
            }
        });

        // Hiển thị Dialog
        dialog.show(getChildFragmentManager(), "CheckInDialog");
    }

    /**
     * Tải dữ liệu từ Firebase về
     */
    private void loadHabitsForToday() {
        if (habitRepository == null) return;

        Log.d(TAG, "Đang tải habits...");

        // Gọi Repository lấy dữ liệu hôm nay
        habitRepository.getHabitsAndHistoryForDate(Calendar.getInstance(), new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                // Ép kiểu dữ liệu trả về
                List<HabitDailyView> data = (List<HabitDailyView>) result;

                // Cập nhật UI
                if (binding != null) {
                    todayHabitList.clear();
                    if (data != null) {
                        // Sắp xếp: Chưa xong lên đầu, Đã xong xuống dưới
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            data.sort((h1, h2) -> {
                                boolean isDone1 = h1.getCurrentValue() >= h1.getTargetValue() || "DONE".equals(h1.getStatus());
                                boolean isDone2 = h2.getCurrentValue() >= h2.getTargetValue() || "DONE".equals(h2.getStatus());

                                if (isDone1 == isDone2) return 0; // Cùng trạng thái thì giữ nguyên
                                return isDone1 ? 1 : -1; // Done (true) thì nằm sau (1), Pending (false) nằm trước (-1)
                            });
                        }
                        todayHabitList.addAll(data);
                    }

                    // Thông báo cho Adapter cập nhật giao diện
                    if (habitAdapter != null) {
                        habitAdapter.notifyDataSetChanged();
                    }

                    Log.d(TAG, "Đã tải xong: " + todayHabitList.size() + " habits");
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadStreakData() {
        // Gọi hàm tính User Streak (Không cần ID thói quen)
        habitRepository.calculateUserStreaks(new StreakCallback() {
            @Override
            public void onStreakCalculated(int currentStreak, int longestStreak) {
                if (binding != null) {
                    String currentText = currentStreak + " days";
                    String longestText = longestStreak + " days";
                    // Cập nhật UI
                    // Giả sử bạn có TextView hiển thị Streak của User
                    binding.tvDetailCurrentStreak.setText(String.valueOf(currentText));
                    binding.tvDetailLongestStreak.setText(String.valueOf(longestText));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("StreakError", "Lỗi: " + e.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu mỗi khi quay lại màn hình này (ví dụ từ màn hình Add/Edit hoặc Dialog tắt)
        loadHabitsForToday();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}