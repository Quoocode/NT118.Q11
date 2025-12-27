package com.example.habittracker.ui.analytics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.habittracker.R;
import com.example.habittracker.data.model.HabitDailyView;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.databinding.FragmentCalendarBinding; // Tạo từ fragment_calendar.xml
import com.google.android.material.bottomsheet.BottomSheetBehavior;
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
    private final SimpleDateFormat monthFormatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault());
    private final Calendar currentMonth = Calendar.getInstance();
    private final Calendar selectedDate = Calendar.getInstance();
    private HabitRepository habitRepository;

    private HabitCompletionAdapter habitAdapter;
    private BottomSheetBehavior<View> sheetBehavior;

    // Fade mapping bounds in parent coordinates
    private int fadeStartTopPx = 0; // sheet top when calendar is fully visible
    private int fadeEndTopPx = 0;   // sheet top when it reaches month_label

    private boolean isMonthAnimating = false;

    // Overlay TextView defined in XML (stacked in a FrameLayout over monthLabel)
    private TextView dayIndicatorLabel;

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

        binding.btnPrevMonth.setOnClickListener(v -> moveMonthAnimated(-1));
        binding.btnNextMonth.setOnClickListener(v -> moveMonthAnimated(1));

        setupMonthLabelMorph();
        setupHabitSheetAndList();

        updateMonth();
        loadHabitsForDate(selectedDate);
    }

    private void setupMonthLabelMorph() {
        if (binding == null) return;

        // Use the overlay label from XML (no manual positioning needed)
        dayIndicatorLabel = binding.dayIndicatorLabel;

        updateMonthLabelText();
        updateDayIndicatorText();

        // Make sure initial state is collapsed
        applyMonthLabelMorph(0f);
    }

    private void updateMonthLabelText() {
        if (binding == null) return;
        binding.monthLabel.setText(monthFormatter.format(currentMonth.getTime()));
    }

    private void updateDayIndicatorText() {
        if (binding == null || dayIndicatorLabel == null) return;
        dayIndicatorLabel.setText(dayFormatter.format(selectedDate.getTime()));
    }

    private void applyMonthLabelMorph(float expandedProgress) {
        if (binding == null || dayIndicatorLabel == null) return;
        float t = Math.max(0f, Math.min(1f, expandedProgress));
        // Crossfade between month label (collapsed) and day indicator (expanded)
        binding.monthLabel.setAlpha(1f - t);
        dayIndicatorLabel.setAlpha(t);
    }

    private void setupHabitSheetAndList() {
        if (binding == null) {
            return;
        }

        // RecyclerView adapter
        habitAdapter = new HabitCompletionAdapter();
        binding.habitCompletionSection.habitCompletionRecycler.setAdapter(habitAdapter);

        // Bottom sheet behavior
        View bottomSheet = binding.habitCompletionSection.getRoot();
        sheetBehavior = BottomSheetBehavior.from(bottomSheet);

        // We only want 2 positions: COLLAPSED (calendar bottom) and EXPANDED (month_label bottom)
        sheetBehavior.setHideable(false);

        // IMPORTANT:
        // expandedOffset is only respected when fitToContents=false.
        // (With fitToContents=true, EXPANDED is computed from content height and may ignore expandedOffset.)
        sheetBehavior.setFitToContents(false);

        // Compute expandedOffset + peekHeight after first layout.
        binding.getRoot().post(this::configureSheetHeights);

        // Drag handle / header can toggle state for convenience
        binding.habitCompletionSection.habitSheetHeader.setOnClickListener(v -> toggleSheet());

        // Fade calendar elements as sheet expands - fade range: calendar bottom -> month_label.
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private int lastState = BottomSheetBehavior.STATE_COLLAPSED;
            private Integer dragStartTop = null;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (sheetBehavior == null) return;

                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    // Record where the drag started so we can infer direction on release.
                    dragStartTop = bottomSheet.getTop();
                }

                // Direction based toggle on release:
                // - dragging up from collapsed => EXPANDED
                // - dragging down from expanded => COLLAPSED
                if (lastState == BottomSheetBehavior.STATE_DRAGGING
                        && newState == BottomSheetBehavior.STATE_SETTLING) {

                    int endTop = bottomSheet.getTop();
                    int startTop = dragStartTop != null ? dragStartTop : endTop;
                    boolean movedUp = endTop < startTop;
                    boolean movedDown = endTop > startTop;

                    if (lastStateStableWasCollapsed() && movedUp) {
                        sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    } else if (lastStateStableWasExpanded() && movedDown) {
                        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else {
                        // Fallback: if direction isn't clear, snap to nearest end.
                        int top = bottomSheet.getTop();
                        int mid = (fadeStartTopPx + fadeEndTopPx) / 2;
                        sheetBehavior.setState(top <= mid
                                ? BottomSheetBehavior.STATE_EXPANDED
                                : BottomSheetBehavior.STATE_COLLAPSED);
                    }

                    dragStartTop = null;
                }

                // If some other path still results in half-expanded, force to nearest end.
                if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    int top = bottomSheet.getTop();
                    int mid = (fadeStartTopPx + fadeEndTopPx) / 2;
                    sheetBehavior.setState(top <= mid
                            ? BottomSheetBehavior.STATE_EXPANDED
                            : BottomSheetBehavior.STATE_COLLAPSED);
                }

                // IMPORTANT: Month/day indicator switching only occurs in stable end states.
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    applyMonthLabelMorph(0f);
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    applyMonthLabelMorph(1f);
                }

                // Track last stable state for direction-based toggling.
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_EXPANDED) {
                    lastStableState = newState;
                }

                lastState = newState;
            }

            private int lastStableState = BottomSheetBehavior.STATE_COLLAPSED;

            private boolean lastStateStableWasCollapsed() {
                return lastStableState == BottomSheetBehavior.STATE_COLLAPSED;
            }

            private boolean lastStateStableWasExpanded() {
                return lastStableState == BottomSheetBehavior.STATE_EXPANDED;
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (binding == null) return;

                // Fade calendar grid based on current sheet top.
                int top = bottomSheet.getTop();
                float t = computeFadeProgress(top);
                float alpha = 1f - t;

                binding.weekdaysRow.setAlpha(alpha);
                binding.calendarDaysRecycler.setAlpha(alpha);

                // DO NOT morph the month/day label during drag; only switch when stable.
            }
        });
    }

    private float computeFadeProgress(int sheetTop) {
        if (fadeEndTopPx <= fadeStartTopPx) {
            // Fallback if layout not ready.
            return 0f;
        }

        // When collapsed (sheetTop ~= fadeStartTopPx): t = 0.
        // When expanded (sheetTop ~= fadeEndTopPx): t = 1.
        float t = (fadeStartTopPx - sheetTop) / (float) (fadeStartTopPx - fadeEndTopPx);
        if (t < 0f) return 0f;
        if (t > 1f) return 1f;
        return t;
    }

    private void configureSheetHeights() {
        if (binding == null || sheetBehavior == null) {
            return;
        }

        // Expanded offset: top of sheet should stop at month navigation row bottom.
        // IMPORTANT: expandedOffset is in the parent CoordinatorLayout's coordinate system.
        // We use getLocationInWindow for consistent coordinates.
        int[] rootLoc = new int[2];
        int[] navRowLoc = new int[2];
        binding.getRoot().getLocationInWindow(rootLoc);
        binding.monthNavigationRow.getLocationInWindow(navRowLoc);

        int rootY = rootLoc[1];
        int navRowBottomY = navRowLoc[1] + binding.monthNavigationRow.getHeight();
        int expandedOffset = Math.max(0, navRowBottomY - rootY);
        sheetBehavior.setExpandedOffset(expandedOffset);

        // Collapsed (lowest) position should be exactly at the bottom of calendar.
        int calendarBottom = binding.calendarContainer.getBottom();
        int parentHeight = binding.getRoot().getHeight();
        int peekHeight = Math.max(0, parentHeight - calendarBottom);
        sheetBehavior.setPeekHeight(peekHeight, true);

        // Save fade bounds
        fadeStartTopPx = calendarBottom;
        fadeEndTopPx = expandedOffset;

        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Ensure alpha + label reflect current position after state set
        View bottomSheet = binding.habitCompletionSection.getRoot();
        float t = computeFadeProgress(bottomSheet.getTop());
        float alpha = 1f - t;
        binding.weekdaysRow.setAlpha(alpha);
        binding.calendarDaysRecycler.setAlpha(alpha);

        // Since we force collapsed here, make sure label is in collapsed state.
        applyMonthLabelMorph(0f);
    }

    private void toggleSheet() {
        if (sheetBehavior == null) return;
        int state = sheetBehavior.getState();
        if (state == BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void moveMonthAnimated(int offset) {
        if (binding == null || isMonthAnimating) {
            return;
        }
        isMonthAnimating = true;

        // Always collapse the habit sheet when switching months to avoid header label overlap.
        if (sheetBehavior != null) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        // Force label to collapsed state immediately.
        applyMonthLabelMorph(0f);

        // Fade the calendar grid/weekday row; keep month label readable but animate it separately
        View fadeTarget = binding.calendarDaysRecycler;
        View fadeWeekdays = binding.weekdaysRow;
        View monthLabel = binding.monthLabel;

        // Directional slide: next month slides left, prev month slides right
        float slideDistance = monthLabel.getResources().getDisplayMetrics().density * 12f;
        float dir = offset > 0 ? -1f : 1f;

        // Stop any in-flight animations to avoid stacking if the user taps quickly
        fadeTarget.animate().cancel();
        fadeWeekdays.animate().cancel();
        monthLabel.animate().cancel();
        if (dayIndicatorLabel != null) {
            dayIndicatorLabel.animate().cancel();
            // Ensure overlay stays hidden during month change
            dayIndicatorLabel.setAlpha(0f);
        }

        // Phase 1: fade/slide out current month label and fade out grid
        monthLabel.animate()
                .alpha(0f)
                .translationX(dir * slideDistance)
                .setDuration(140)
                .start();

        fadeWeekdays.animate()
                .alpha(0f)
                .setDuration(140)
                .start();

        fadeTarget.animate()
                .alpha(0f)
                .setDuration(140)
                .withEndAction(() -> {
                    if (binding == null) {
                        return;
                    }

                    // Apply the month change once the old month is faded out.
                    moveMonth(offset);

                    // Month switching always collapses the sheet, so restore to fully visible.
                    float targetAlpha = 1f;

                    // Reset label to opposite side before sliding it in
                    monthLabel.setTranslationX(-dir * slideDistance);
                    monthLabel.setAlpha(0f);

                    // Reset grid elements to 0 before fading in
                    fadeWeekdays.setAlpha(0f);
                    fadeTarget.setAlpha(0f);

                    // Phase 2: fade/slide in new month label and fade in grid
                    monthLabel.animate()
                            .alpha(1f)
                            .translationX(0f)
                            .setDuration(180)
                            .start();

                    fadeWeekdays.animate()
                            .alpha(targetAlpha)
                            .setDuration(180)
                            .start();

                    fadeTarget.animate()
                            .alpha(targetAlpha)
                            .setDuration(180)
                            .withEndAction(() -> isMonthAnimating = false)
                            .start();
                })
                .start();
    }

    private void moveMonth(int offset) {
        currentMonth.add(Calendar.MONTH, offset);
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedDate.setTime(currentMonth.getTime());

        // Update day indicator too since selected day changed
        updateDayIndicatorText();

        updateMonth();
        loadHabitsForDate(selectedDate);
    }

    private void updateMonth() {
        updateMonthLabelText();
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

        if (habitAdapter != null) {
            habitAdapter.submitList(new ArrayList<>(allHabits));
        }
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


    @Override
    public void onDaySelected(Date date) {
        selectedDate.setTime(date);
        calendarAdapter.setSelectedDate(date);

        // Update the day indicator immediately when a new day is selected.
        updateDayIndicatorText();

        loadHabitsForDate(selectedDate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        dayIndicatorLabel = null;
    }
}

