package com.example.habittracker.ui.analytics;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.example.habittracker.R;
import com.example.habittracker.databinding.FragmentCalendarBinding; // Tạo từ fragment_calendar.xml
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

        calendarAdapter = new CalendarDayAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 7);
        binding.calendarDaysRecycler.setLayoutManager(layoutManager);
        binding.calendarDaysRecycler.setAdapter(calendarAdapter);

        binding.btnPrevMonth.setOnClickListener(v -> moveMonth(-1));
        binding.btnNextMonth.setOnClickListener(v -> moveMonth(1));
        binding.btnBack.setOnClickListener(v -> navController.popBackStack());

        setupHabitList();
        seedSampleHabits();
        updateMonth();
    }

    private void setupHabitList() {
        ScrollView scrollView = binding.habitCompletionSection.habitCompletionScroll;
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (!scrollView.canScrollVertically(1)) {
                appendMoreHabits();
            }
        });
    }

    private void moveMonth(int offset) {
        currentMonth.add(Calendar.MONTH, offset);
        updateMonth();
    }

    private void updateMonth() {
        binding.monthLabel.setText(monthFormatter.format(currentMonth.getTime()));
        List<CalendarDay> days = buildMonthDays();
        calendarAdapter.submitDays(days);
        calendarAdapter.setToday(Calendar.getInstance().getTime());
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

    private void seedSampleHabits() {
        allHabits.clear();
        allHabits.add(new HabitCompletion("Exercise", HabitCompletion.Status.COMPLETED));
        allHabits.add(new HabitCompletion("Read", HabitCompletion.Status.MISSED));
        allHabits.add(new HabitCompletion("Meditate", HabitCompletion.Status.PENDING));
        allHabits.add(new HabitCompletion("Drink Water", HabitCompletion.Status.COMPLETED));
        allHabits.add(new HabitCompletion("Sleep Early", HabitCompletion.Status.MISSED));
        allHabits.add(new HabitCompletion("Journal", HabitCompletion.Status.PENDING));
        habitsLoaded = 0;
        appendMoreHabits();
    }

    private void appendMoreHabits() {
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
        binding.habitCompletionSection.habitFooterMessage.setVisibility(habitsLoaded >= allHabits.size() ? View.GONE : View.VISIBLE);
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
                icon.setImageResource(R.drawable.ic_completed);
                break;
            case MISSED:
                pendingMarker.setVisibility(View.GONE);
                icon.setVisibility(View.VISIBLE);
                icon.setImageResource(R.drawable.ic_missed);
                break;
            case PENDING:
                icon.setVisibility(View.GONE);
                pendingMarker.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onDaySelected(Date date) {
        // Hook for future detail view
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}