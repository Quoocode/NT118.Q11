package com.example.habittracker.ui.home;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habittracker.R;
import com.example.habittracker.data.achievements.AchievementService;
import com.example.habittracker.data.repository.callback.StreakCallback;
import com.example.habittracker.databinding.FragmentHomeBinding;

// Import các thành phần chúng ta đã làm
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.ui.adapter.DashboardHabitAdapter; // Hoặc HabitAdapter tùy tên bạn đặt
import com.example.habittracker.data.repository.callback.HabitQueryCallback;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.content.Context;
import com.example.habittracker.utils.LocaleHelper;
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NavController navController;

    // Variables
    private HabitRepository habitRepository;
    private DashboardHabitAdapter habitAdapter;
    private final List<HabitDailyView> todayHabitList = new ArrayList<>();
    private String currentUserId;
    private AchievementService achievementService;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(LocaleHelper.applyLocale(context));
    }



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

        achievementService = new AchievementService(requireContext());

        // 2. Thiết lập RecyclerView
        setupRecyclerView();

        // 3. Sự kiện nút "Add Habit"
        // (Đảm bảo ID btnAddHabitToday khớp với file XML fragment_home.xml của bạn)
        binding.btnAddHabitToday.setOnClickListener(v ->
                navController.navigate(com.example.habittracker.R.id.action_homeFragment_to_addEditHabitFragment)
        );
        loadStreakData();
        loadDailyProgress();
    }

    private void setupRecyclerView() {
        binding.recyclerViewHabits.setLayoutManager(new LinearLayoutManager(getContext()));

        habitAdapter = new DashboardHabitAdapter(getContext(), todayHabitList,
                habit -> showCheckInDialog(habit),
                habit -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("EXTRA_HABIT_ID", habit.getHabitId());
                    navController.navigate(com.example.habittracker.R.id.action_homeFragment_to_habitDetailsFragment, bundle);
                }
        );

        binding.recyclerViewHabits.setAdapter(habitAdapter);
    }

    /**
     * Hàm hiển thị DialogFragment để Check-in (Nhập số liệu)
     */
    private void showCheckInDialog(HabitDailyView habit) {
        HabitCheckInDialogFragment dialog = HabitCheckInDialogFragment.newInstance(
                habit.getHabitId(),
                habit.getTitle(),
                habit.getTargetValue(),
                habit.getCurrentValue(),
                habit.getUnit(),
                habit.getStatus()
        );

        dialog.setOnCheckInListener(() -> {
            Log.d(TAG, "Check-in hoàn tất, tải lại danh sách...");
            loadHabitsForToday();
            loadDailyProgress();
            loadStreakData();
        });

        dialog.show(getChildFragmentManager(), "CheckInDialog");
    }

    /**
     * Tải dữ liệu từ Firebase về
     */
    private void loadHabitsForToday() {
        if (habitRepository == null) return;

        Log.d(TAG, "Đang tải habits...");

        habitRepository.getHabitsAndHistoryForDate(Calendar.getInstance(), new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (binding == null) return;

                todayHabitList.clear();
                for (Object item : result) {
                    if (item instanceof HabitDailyView) {
                        todayHabitList.add((HabitDailyView) item);
                    }
                }

                // Achievement: evaluate today's snapshot (all done, perfect day tracking, streak thresholds)
                if (achievementService != null) {
                    achievementService.onDaySnapshot(Calendar.getInstance(), todayHabitList);
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    todayHabitList.sort((h1, h2) -> {
                        boolean isDone1 = h1.getCurrentValue() >= h1.getTargetValue() || "DONE".equals(h1.getStatus());
                        boolean isDone2 = h2.getCurrentValue() >= h2.getTargetValue() || "DONE".equals(h2.getStatus());
                        if (isDone1 == isDone2) return 0;
                        return isDone1 ? 1 : -1;
                    });
                }

                if (habitAdapter != null) {
                    habitAdapter.notifyDataSetChanged();
                }

                Log.d(TAG, "Đã tải xong: " + todayHabitList.size() + " habits");
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
        habitRepository.calculateUserStreaks(new StreakCallback() {
            @Override
            public void onStreakCalculated(int currentStreak, int longestStreak) {
                if (binding != null) {
                    String currentText = getString(R.string.days_format, currentStreak);
                    String longestText = getString(R.string.days_format, longestStreak);
                    binding.tvDetailCurrentStreak.setText(currentText);
                    binding.tvDetailLongestStreak.setText(longestText);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("StreakError", "Lỗi: " + e.getMessage());
            }
        });
    }

    private void loadDailyProgress() {
        Calendar today = Calendar.getInstance();

        // Lấy context đã apply locale
        Context localeContext = LocaleHelper.applyLocale(requireContext());

        habitRepository.getHabitsAndHistoryForDate(today, new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (binding == null) return;

                int totalHabits = result.size();
                int completedHabits = 0;

                for (Object item : result) {
                    if (item instanceof HabitDailyView) {
                        HabitDailyView habit = (HabitDailyView) item;
                        if ("DONE".equalsIgnoreCase(habit.getStatus()) ||
                                "COMPLETED".equalsIgnoreCase(habit.getStatus())) {
                            completedHabits++;
                        }
                    }
                }

                // Set label Progress / Tiến độ
                binding.tvProgressLabel.setText(localeContext.getString(R.string.home_progress));

                // Set số đã hoàn thành / tổng
                String progressText = localeContext.getString(
                        R.string.progress_count_format, completedHabits, totalHabits
                );
                binding.tvProgressCount.setText(progressText);

                // Animate progressBar
                binding.progressBarDaily.setMax(totalHabits);
                ObjectAnimator.ofInt(binding.progressBarDaily, "progress", completedHabits)
                        .setDuration(500)
                        .start();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("ProgressError", "Lỗi tính progress: " + e.getMessage());
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        loadHabitsForToday();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
