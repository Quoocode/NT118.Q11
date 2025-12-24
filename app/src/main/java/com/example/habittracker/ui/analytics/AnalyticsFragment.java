package com.example.habittracker.ui.analytics;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentCalendarBinding; // Tạo từ fragment_calendar.xml
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.google.firebase.auth.FirebaseAuth;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalyticsFragment extends Fragment implements CalendarDayAdapter.Listener {

    private FragmentCalendarBinding binding;
    private NavController navController;

    private CalendarDayAdapter calendarAdapter;
    private final List<HabitCompletion> allHabits = new ArrayList<>();
    private int habitsLoaded = 0;
    private static final int HABIT_BATCH_SIZE = 3;
    private final SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final Calendar currentMonth = Calendar.getInstance();
    private final Calendar selectedDate = Calendar.getInstance();
    private HabitRepository habitRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalendarBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            habitRepository = new HabitRepository(uid);
        } else {
            Toast.makeText(requireContext(), R.string.error_auth_required, Toast.LENGTH_SHORT).show();
        }

        calendarAdapter = new CalendarDayAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 7);
        binding.calendarDaysRecycler.setLayoutManager(layoutManager);
        binding.calendarDaysRecycler.setAdapter(calendarAdapter);

        binding.btnPrevMonth.setOnClickListener(v -> moveMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> moveMonth(1));
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        setupHabitList();
        updateMonth();
        loadHabitsForDate(selectedDate);
    }

    private void setupHabitList() {
        if (binding == null || binding.habitCompletionSection == null) {
            return;
        }
        ScrollView scrollView = binding.habitCompletionSection.habitCompletionScroll;
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (binding == null || binding.habitCompletionSection == null) {
                return;
            }
            if (!scrollView.canScrollVertically(1)) {
                appendMoreHabits();
            }
        });
    }

    private void moveMonth(int offset) {
        currentMonth.add(Calendar.MONTH, offset);
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate.setTime(currentMonth.getTime());
        updateMonth();
        loadHabitsForDate(selectedDate);
    }

    private void updateMonth() {
        binding.monthLabel.setText(monthFormatter.format(currentMonth.getTime()));
        List<CalendarDay> days = buildMonthDays();
        calendarAdapter.submitDays(days);
        calendarAdapter.setToday(Calendar.getInstance().getTime());
        calendarAdapter.setSelectedDate(selectedDate.getTime());
    }

    private List<CalendarDay> buildMonthDays() {
        List<CalendarDay> days = new ArrayList<>();
        Calendar temp = (Calendar) currentMonth.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);
        int leading = temp.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        if (leading < 0) leading += 7;
        temp.add(Calendar.DAY_OF_MONTH, -leading);
        for (int i = 0; i < 42; i++) {
            boolean inCurrent = temp.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
                    && temp.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR);
            days.add(new CalendarDay(temp.getTime(), inCurrent));
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private void loadHabitsForDate(Calendar date) {
        if (habitRepository == null) {
            seedSampleHabits();
            return;
        }
        Calendar target = (Calendar) date.clone();
        habitRepository.getHabitsAndHistoryForDate(target, new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                List<HabitCompletion> mapped = new ArrayList<>();
                for (Object item : result) {
                    if (item instanceof HabitDailyView) {
                        mapped.add(mapToCompletion((HabitDailyView) item));
                    }
                }
                applyHabitData(mapped);
            }

            @Override
            public void onFailure(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), getString(R.string.error_load_habits, e.getMessage()), Toast.LENGTH_SHORT).show();
                }
                seedSampleHabits();
            }
        });
    }

    private HabitCompletion mapToCompletion(HabitDailyView view) {
        HabitCompletion.Status status = resolveStatus(view);
        return new HabitCompletion(view.getTitle(), status);
    }

    private HabitCompletion.Status resolveStatus(HabitDailyView view) {
        String status = view.getStatus();
        if (status != null) {
            String normalized = status.toUpperCase(Locale.US);
            if ("DONE".equals(normalized) || "COMPLETED".equals(normalized)) {
                return HabitCompletion.Status.COMPLETED;
            }
            if ("MISSED".equals(normalized) || "SKIPPED".equals(normalized)) {
                return HabitCompletion.Status.MISSED;
            }
        }
        if (view.getTargetValue() > 0 && view.getCurrentValue() >= view.getTargetValue()) {
            return HabitCompletion.Status.COMPLETED;
        }
        return HabitCompletion.Status.PENDING;
    }

    private void applyHabitData(List<HabitCompletion> completions) {
        allHabits.clear();
        allHabits.addAll(completions);
        habitsLoaded = 0;
        if (binding == null || binding.habitCompletionSection == null) {
            return;
        }
        LinearLayout container = binding.habitCompletionSection.habitCompletionListContainer;
        container.removeAllViews();
        appendMoreHabits();
    }

    private void seedSampleHabits() {
        List<HabitCompletion> samples = new ArrayList<>();
        samples.add(new HabitCompletion("Exercise", HabitCompletion.Status.COMPLETED));
        samples.add(new HabitCompletion("Read", HabitCompletion.Status.MISSED));
        samples.add(new HabitCompletion("Meditate", HabitCompletion.Status.PENDING));
        samples.add(new HabitCompletion("Drink Water", HabitCompletion.Status.COMPLETED));
        samples.add(new HabitCompletion("Sleep Early", HabitCompletion.Status.MISSED));
        samples.add(new HabitCompletion("Journal", HabitCompletion.Status.PENDING));
        applyHabitData(samples);
    }

    private void appendMoreHabits() {
        if (binding == null || binding.habitCompletionSection == null) {
            return;
        }
        LinearLayout container = binding.habitCompletionSection.habitCompletionListContainer;
        int nextLimit = Math.min(allHabits.size(), habitsLoaded + HABIT_BATCH_SIZE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = habitsLoaded; i < nextLimit; i++) {
            HabitCompletion habit = allHabits.get(i);
            View item = inflater.inflate(R.layout.item_habit_completion, container, false);
            TextView name = item.findViewById(R.id.tv_habit_name);
            TextView statusText = item.findViewById(R.id.tv_habit_status);
            View icon = item.findViewById(R.id.img_status);
            TextView pendingMarker = item.findViewById(R.id.tv_pending_marker);
            name.setText(habit.getName());
            statusText.setText(habit.getStatus().name());
            updateStatusIcon(habit.getStatus(), icon, pendingMarker);
            container.addView(item);
        }
        habitsLoaded = nextLimit;
        binding.habitCompletionSection.habitFooterMessage.setVisibility(
                habitsLoaded >= allHabits.size() ? View.GONE : View.VISIBLE
        );
    }

    private void updateStatusIcon(HabitCompletion.Status status, View imageView, TextView pendingMarker) {
        if (!(imageView instanceof android.widget.ImageView)) {
            return;
        }
        android.widget.ImageView icon = (android.widget.ImageView) imageView;
        switch (status) {
            case COMPLETED:
                pendingMarker.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_circle_check);
                break;
            case MISSED:
                pendingMarker.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_close_circle);
                break;
            case PENDING:
                icon.setVisibility(View.GONE);
                pendingMarker.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onDaySelected(Date date) {
        selectedDate.setTime(date);
        calendarAdapter.setSelectedDate(date);
        loadHabitsForDate(selectedDate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}