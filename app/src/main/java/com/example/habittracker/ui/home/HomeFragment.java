package com.example.habittracker.ui.home;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import android.widget.GridLayout;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private NavController navController;

    // Variables
    private HabitRepository habitRepository;
    private DashboardHabitAdapter habitAdapter;
    private final List<HabitDailyView> todayHabitList = new ArrayList<>();
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void setupAchievementsToggle() {
        if (binding == null) return;
        View achievementsRoot = binding.getRoot().findViewById(R.id.include_achievements);
        if (achievementsRoot == null) return;

        // dummy placeholder
        DummyBadgeForTesting(achievementsRoot);

        View horizontalContainer = achievementsRoot.findViewById(R.id.badges_list_container);
        View gridContainer = achievementsRoot.findViewById(R.id.badge_grid_container);
        ImageButton expandButton = achievementsRoot.findViewById(R.id.btn_badge_expand);
        Button collapseButton = achievementsRoot.findViewById(R.id.btn_badge_collapse);

        if (horizontalContainer == null || gridContainer == null || expandButton == null || collapseButton == null) {
            return;
        }

        collapseButton.setOnClickListener(v -> collapseBadges(horizontalContainer, gridContainer, expandButton));
        expandButton.setOnClickListener(v -> expandBadges(horizontalContainer, gridContainer, expandButton));
    }

    private void expandBadges(View horizontalContainer, View gridContainer, ImageButton expandButton) {
        horizontalContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            horizontalContainer.setVisibility(View.GONE);
            gridContainer.setAlpha(0f);
            gridContainer.setTranslationY(16f);
            gridContainer.setVisibility(View.VISIBLE);
            gridContainer.animate().alpha(1f).translationY(0f).setDuration(200).start();
        }).start();
        rotateChevron(expandButton, 180f);
    }

    private void collapseBadges(View horizontalContainer, View gridContainer, ImageButton expandButton) {
        gridContainer.animate().alpha(0f).translationY(16f).setDuration(150).withEndAction(() -> {
            gridContainer.setVisibility(View.GONE);
            horizontalContainer.setAlpha(0f);
            horizontalContainer.setVisibility(View.VISIBLE);
            horizontalContainer.animate().alpha(1f).setDuration(200).start();
        }).start();
        rotateChevron(expandButton, 0f);
    }

    private void rotateChevron(ImageButton button, float toDegrees) {
        if (button == null) return;
        button.animate().rotation(toDegrees).setDuration(150).start();
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
        binding.btnAddHabitToday.setOnClickListener(v ->
                navController.navigate(com.example.habittracker.R.id.action_homeFragment_to_addEditHabitFragment)
        );
        loadStreakData();
        loadDailyProgress();
        setupAchievementsToggle();
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
                    String currentText = currentStreak + " days";
                    String longestText = longestStreak + " days";
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

                String progressText = completedHabits + "/" + totalHabits;
                binding.tvProgressCount.setText(progressText);

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

    private void DummyBadgeForTesting(View achievementsRoot) {
        LinearLayout list = achievementsRoot.findViewById(R.id.badges_horizontal_list);
        if (list == null || getContext() == null) return;

        list.removeAllViews();

        int[] icons = new int[]{
                R.drawable.ic_cup,
                R.drawable.ic_streak,
                R.drawable.ic_plus,
                R.drawable.ic_cup,
                R.drawable.ic_streak,
                R.drawable.ic_plus,
                R.drawable.ic_cup,
                R.drawable.ic_streak,
                R.drawable.ic_plus
        };
        String[] titles = new String[]{
                "Starter",
                "3-day",
                "Creator",
                "Consistency",
                "7-day",
                "Upgrader",
                "Milestone",
                "Streaker",
                "Collector"
        };

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < titles.length; i++) {
            View item = inflater.inflate(R.layout.item_badge_placeholder, list, false);

            ImageView icon = item.findViewById(R.id.badge_icon);
            TextView title = item.findViewById(R.id.badge_title);

            if (icon != null) icon.setImageResource(icons[i % icons.length]);
            if (title != null) title.setText(titles[i]);

            list.addView(item);
        }

        GridLayout grid = achievementsRoot.findViewById(R.id.badges_grid);
        if (grid != null) {
            grid.removeAllViews();

            for (int i = 0; i < titles.length; i++) {
                View gridItem = inflater.inflate(R.layout.item_badge_placeholder, grid, false);

                ImageView icon = gridItem.findViewById(R.id.badge_icon);
                TextView title = gridItem.findViewById(R.id.badge_title);

                if (icon != null) icon.setImageResource(icons[i % icons.length]);
                if (title != null) title.setText(titles[i]);

                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                lp.setMargins(8, 8, 8, 8);
                gridItem.setLayoutParams(lp);

                grid.addView(gridItem);
            }
        }
    }
}
