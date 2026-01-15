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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.example.habittracker.ui.home.HabitCheckInDialogFragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.util.Log;

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


    private int fadeStartTopPx = 0;
    private int fadeEndTopPx = 0;

    private boolean isMonthAnimating = false;


    private TextView dayIndicatorLabel;

    private static final String TAG_FADE = "CalendarSheetFade";
    private static final boolean DEBUG_FADE = false;

    private View habitBottomSheetView;

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

        dayIndicatorLabel = binding.dayIndicatorLabel;

        updateMonthLabelText();
        updateDayIndicatorText();

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
        binding.monthLabel.setAlpha(1f - t);
        dayIndicatorLabel.setAlpha(t);
    }

    private void setupHabitSheetAndList() {
        if (binding == null) {
            return;
        }


        final int baseBottom = (int) (16f * requireContext().getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(binding.habitCompletionSection.habitCompletionRecycler, (v, insets) -> {
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            int appBottomNavHeight = 0;
            View activityBottomNav = requireActivity().findViewById(R.id.bottom_navigation_view);
            if (activityBottomNav instanceof BottomNavigationView && activityBottomNav.getVisibility() == View.VISIBLE) {
                // Ensure we have a measured height
                appBottomNavHeight = activityBottomNav.getHeight();
            }

            int left = v.getPaddingLeft();
            int top = v.getPaddingTop();
            int right = v.getPaddingRight();
            v.setPadding(left, top, right, baseBottom + navBottom + appBottomNavHeight);
            return insets;
        });

        binding.habitCompletionSection.habitCompletionRecycler.post(() ->
                ViewCompat.requestApplyInsets(binding.habitCompletionSection.habitCompletionRecycler)
        );

        habitAdapter = new HabitCompletionAdapter(
                habit -> {
                    if (habit.getHabitId() == null) return;
                    Bundle bundle = new Bundle();
                    bundle.putString("EXTRA_HABIT_ID", habit.getHabitId());
                    navController.navigate(com.example.habittracker.R.id.action_analyticsFragment_to_habitDetailsFragment, bundle);
                },
                habit -> {
                    if (habit.getHabitId() == null) return;

                    HabitCheckInDialogFragment dialog = HabitCheckInDialogFragment.newInstance(
                            habit.getHabitId(),
                            habit.getName(),
                            habit.getTargetValue(),
                            habit.getCurrentValue(),
                            habit.getUnit(),
                            habit.getRawStatus() != null ? habit.getRawStatus() : "PENDING"
                    );

                    dialog.setOnCheckInListener(() -> loadHabitsForDate(selectedDate));
                    dialog.show(getChildFragmentManager(), "CheckInDialog");
                }
        );
        binding.habitCompletionSection.habitCompletionRecycler.setAdapter(habitAdapter);

        habitBottomSheetView = binding.habitCompletionSection.getRoot();
        sheetBehavior = BottomSheetBehavior.from(habitBottomSheetView);

        sheetBehavior.setHideable(false);

        sheetBehavior.setFitToContents(false);

        binding.getRoot().post(this::configureSheetHeights);

        binding.habitCompletionSection.habitSheetHeader.setOnClickListener(v -> toggleSheet());

        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private int lastState = BottomSheetBehavior.STATE_COLLAPSED;
            private Integer dragStartTop = null;

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (sheetBehavior == null) return;

                // Some OEMs pass a different reference here; always use the real sheet view.
                final View sheet = habitBottomSheetView != null ? habitBottomSheetView : bottomSheet;

                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    cancelCalendarFadeAnimations();
                    dragStartTop = sheet.getTop();

                    if (DEBUG_FADE) {
                        Log.d(TAG_FADE, "DRAGGING startTop=" + dragStartTop + " fadeStart=" + fadeStartTopPx + " fadeEnd=" + fadeEndTopPx);
                    }
                }

                if (lastState == BottomSheetBehavior.STATE_DRAGGING
                        && newState == BottomSheetBehavior.STATE_SETTLING) {

                    int endTop = sheet.getTop();
                    int startTop = dragStartTop != null ? dragStartTop : endTop;
                    boolean movedUp = endTop < startTop;
                    boolean movedDown = endTop > startTop;

                    if (DEBUG_FADE) {
                        Log.d(TAG_FADE, "SETTLING startTop=" + startTop + " endTop=" + endTop + " movedUp=" + movedUp + " movedDown=" + movedDown);
                    }

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

                if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    int top = bottomSheet.getTop();
                    int mid = (fadeStartTopPx + fadeEndTopPx) / 2;
                    sheetBehavior.setState(top <= mid
                            ? BottomSheetBehavior.STATE_EXPANDED
                            : BottomSheetBehavior.STATE_COLLAPSED);
                }

                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    applyMonthLabelMorph(0f);
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    applyMonthLabelMorph(1f);
                }

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

                cancelCalendarFadeAnimations();

                final View sheet = habitBottomSheetView != null ? habitBottomSheetView : bottomSheet;
                int top = sheet.getTop();

                if (fadeEndTopPx <= 0 || fadeStartTopPx <= 0 || fadeEndTopPx >= fadeStartTopPx) {
                    binding.getRoot().post(AnalyticsFragment.this::configureSheetHeights);
                }

                float t = computeFadeProgress(top);
                float alpha = 1f - t;

                binding.weekdaysRow.setAlpha(alpha);
                binding.calendarDaysRecycler.setAlpha(alpha);

                if (DEBUG_FADE) {
                    Log.d(TAG_FADE, "onSlide top=" + top + " t=" + t + " alpha=" + alpha
                            + " fadeStart=" + fadeStartTopPx + " fadeEnd=" + fadeEndTopPx
                            + " slideOffset=" + slideOffset);
                }
            }
        });
    }

    private void cancelCalendarFadeAnimations() {
        if (binding == null) return;
        binding.calendarDaysRecycler.animate().cancel();
        binding.weekdaysRow.animate().cancel();
        // monthLabel/dayIndicatorLabel are animated during month switch too.
        binding.monthLabel.animate().cancel();
        if (dayIndicatorLabel != null) {
            dayIndicatorLabel.animate().cancel();
        }
    }

    private float computeFadeProgress(int sheetTop) {
        return CalendarFadeMath.computeProgress(sheetTop, fadeStartTopPx, fadeEndTopPx);
    }

    private void configureSheetHeights() {
        if (binding == null || sheetBehavior == null) {
            return;
        }

        final View root = binding.getRoot();
        final View bottomSheet = habitBottomSheetView != null
                ? habitBottomSheetView
                : binding.habitCompletionSection.getRoot();

        if (root.getHeight() == 0 || bottomSheet.getHeight() == 0) {
            root.post(this::configureSheetHeights);
            return;
        }

        int calendarBottomInRoot = getBottomInAncestorCoords(binding.calendarContainer, root);
        int navRowBottomInRoot = getBottomInAncestorCoords(binding.monthNavigationRow, root);

        int expandedOffset = Math.max(0, navRowBottomInRoot);
        sheetBehavior.setExpandedOffset(expandedOffset);

        int parentHeight = root.getHeight();
        int peekHeight = Math.max(0, parentHeight - calendarBottomInRoot);
        sheetBehavior.setPeekHeight(peekHeight, true);

        fadeStartTopPx = calendarBottomInRoot;
        fadeEndTopPx = expandedOffset;

        if (fadeEndTopPx >= fadeStartTopPx) {
            int[] rootLoc = new int[2];
            int[] calLoc = new int[2];
            int[] navLoc = new int[2];
            root.getLocationInWindow(rootLoc);
            binding.calendarContainer.getLocationInWindow(calLoc);
            binding.monthNavigationRow.getLocationInWindow(navLoc);

            int rootY = rootLoc[1];
            int calBottom = (calLoc[1] + binding.calendarContainer.getHeight()) - rootY;
            int navBottom = (navLoc[1] + binding.monthNavigationRow.getHeight()) - rootY;

            fadeStartTopPx = calBottom;
            fadeEndTopPx = navBottom;

            expandedOffset = Math.max(0, fadeEndTopPx);
            sheetBehavior.setExpandedOffset(expandedOffset);

            peekHeight = Math.max(0, parentHeight - fadeStartTopPx);
            sheetBehavior.setPeekHeight(peekHeight, true);
        }

        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        float t = computeFadeProgress(bottomSheet.getTop());
        float alpha = 1f - t;
        binding.weekdaysRow.setAlpha(alpha);
        binding.calendarDaysRecycler.setAlpha(alpha);

        if (DEBUG_FADE) {
            Log.d(TAG_FADE, "configureSheetHeights rootH=" + root.getHeight()
                    + " sheetTop=" + bottomSheet.getTop()
                    + " fadeStart=" + fadeStartTopPx
                    + " fadeEnd=" + fadeEndTopPx);
        }

        applyMonthLabelMorph(0f);
    }

    private int getBottomInAncestorCoords(@NonNull View descendant, @NonNull View ancestor) {
        int[] descLoc = new int[2];
        int[] ancLoc = new int[2];
        descendant.getLocationInWindow(descLoc);
        ancestor.getLocationInWindow(ancLoc);
        return (descLoc[1] - ancLoc[1]) + descendant.getHeight();
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

        if (sheetBehavior != null && sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            applyMonthLabelMorph(0f);

            cancelCalendarFadeAnimations();
            moveMonth(offset);

            binding.getRoot().post(this::configureSheetHeights);

            isMonthAnimating = false;
            return;
        }

        isMonthAnimating = true;

        if (sheetBehavior != null) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        applyMonthLabelMorph(0f);

        cancelCalendarFadeAnimations();

        View fadeTarget = binding.calendarDaysRecycler;
        View fadeWeekdays = binding.weekdaysRow;
        View monthLabel = binding.monthLabel;

        float slideDistance = monthLabel.getResources().getDisplayMetrics().density * 12f;
        float dir = offset > 0 ? -1f : 1f;

        fadeTarget.animate().cancel();
        fadeWeekdays.animate().cancel();
        monthLabel.animate().cancel();
        if (dayIndicatorLabel != null) {
            dayIndicatorLabel.animate().cancel();
            dayIndicatorLabel.setAlpha(0f);
        }

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

                    moveMonth(offset);

                    binding.getRoot().post(this::configureSheetHeights);

                    float targetAlpha = 1f;

                    monthLabel.setTranslationX(-dir * slideDistance);
                    monthLabel.setAlpha(0f);

                    fadeWeekdays.setAlpha(0f);
                    fadeTarget.setAlpha(0f);

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
        return new HabitCompletion(
                view.getHabitId(),
                view.getTitle(),
                status,
                view.getTargetValue(),
                view.getCurrentValue(),
                view.getUnit(),
                view.getStatus()
        );
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
            if ("PENDING".equals(normalized) && isSelectedDateInPast()) {
                return HabitCompletion.Status.MISSED;
            }
        }
        if (view.getTargetValue() > 0 && view.getCurrentValue() >= view.getTargetValue()) {
            return HabitCompletion.Status.COMPLETED;
        }
        if (isSelectedDateInPast()) {
            return HabitCompletion.Status.MISSED;
        }
        return HabitCompletion.Status.PENDING;
    }

    private boolean isSelectedDateInPast() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar selected = (Calendar) selectedDate.clone();
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        return selected.before(today);
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
