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

import com.example.habittracker.ui.home.HabitOptionsBottomSheetDialogFragment;

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

    // Biên ánh xạ fade theo toạ độ của parent
    private int fadeStartTopPx = 0; // top của sheet khi lịch hiển thị đầy đủ
    private int fadeEndTopPx = 0;   // top của sheet khi chạm đến month_label

    private boolean isMonthAnimating = false;

    // Overlay TextView khai báo trong XML (xếp chồng trong FrameLayout lên monthLabel)
    private TextView dayIndicatorLabel;

    private static final String TAG_FADE = "CalendarSheetFade";
    private static final boolean DEBUG_FADE = false;

    // Cache view thật sự mà BottomSheetBehavior đang điều khiển.
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

        // Dùng label overlay từ XML (không cần tự canh vị trí thủ công)
        dayIndicatorLabel = binding.dayIndicatorLabel;

        updateMonthLabelText();
        updateDayIndicatorText();

        // Đảm bảo trạng thái ban đầu là collapsed
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
        // Crossfade giữa month label (collapsed) và day indicator (expanded)
        binding.monthLabel.setAlpha(1f - t);
        dayIndicatorLabel.setAlpha(t);
    }

    private void setupHabitSheetAndList() {
        if (binding == null) {
            return;
        }

        // Đảm bảo RecyclerView trong bottom sheet không bị đè bởi:
        // 1) thanh điều hướng hệ thống (gesture bar)
        // 2) BottomNavigationView của app (home/calendar/achievements/settings)
        final int baseBottom = (int) (16f * requireContext().getResources().getDisplayMetrics().density);

        ViewCompat.setOnApplyWindowInsetsListener(binding.habitCompletionSection.habitCompletionRecycler, (v, insets) -> {
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;

            int appBottomNavHeight = 0;
            View activityBottomNav = requireActivity().findViewById(R.id.bottom_navigation_view);
            if (activityBottomNav instanceof BottomNavigationView && activityBottomNav.getVisibility() == View.VISIBLE) {
                // Đảm bảo đã đo được chiều cao (measured)
                appBottomNavHeight = activityBottomNav.getHeight();
            }

            int left = v.getPaddingLeft();
            int top = v.getPaddingTop();
            int right = v.getPaddingRight();
            v.setPadding(left, top, right, baseBottom + navBottom + appBottomNavHeight);
            return insets;
        });

        // Apply lại sau khi layout xong (vì chiều cao bottom nav có thể = 0 trước lần đo đầu)
        binding.habitCompletionSection.habitCompletionRecycler.post(() ->
                ViewCompat.requestApplyInsets(binding.habitCompletionSection.habitCompletionRecycler)
        );

        // Adapter cho RecyclerView
        habitAdapter = new HabitCompletionAdapter(
                habit -> {
                    if (habit.getHabitId() == null) return;
                    Bundle bundle = new Bundle();
                    bundle.putString("EXTRA_HABIT_ID", habit.getHabitId());
                    navController.navigate(com.example.habittracker.R.id.action_analyticsFragment_to_habitDetailsFragment, bundle);
                },
                habit -> {
                    if (habit.getHabitId() == null) return;

                    HabitOptionsBottomSheetDialogFragment sheet = HabitOptionsBottomSheetDialogFragment.newInstance(
                            habit.getHabitId(),
                            habit.getName() != null ? habit.getName() : "",
                            habit.getTargetValue(),
                            habit.getCurrentValue(),
                            habit.getUnit(),
                            habit.getRawStatus() != null ? habit.getRawStatus() : "PENDING"
                    );

                    sheet.setOnCheckInListener(() -> loadHabitsForDate(selectedDate));
                    sheet.show(getChildFragmentManager(), "HabitOptionsSheet");
                }
        );
        binding.habitCompletionSection.habitCompletionRecycler.setAdapter(habitAdapter);

        // Thiết lập BottomSheetBehavior
        habitBottomSheetView = binding.habitCompletionSection.getRoot();
        sheetBehavior = BottomSheetBehavior.from(habitBottomSheetView);

        // Chỉ muốn 2 vị trí: COLLAPSED (chạm đáy lịch) và EXPANDED (chạm đáy month_label)
        sheetBehavior.setHideable(false);

        // Quan trọng:
        // expandedOffset chỉ có hiệu lực khi fitToContents=false.
        // (fitToContents=true thì trạng thái EXPANDED phụ thuộc vào content height và có thể bỏ qua expandedOffset.)
        sheetBehavior.setFitToContents(false);

        // Tính expandedOffset + peekHeight sau khi layout xong.
        binding.getRoot().post(this::configureSheetHeights);

        // Bấm header để toggle trạng thái cho tiện
        binding.habitCompletionSection.habitSheetHeader.setOnClickListener(v -> toggleSheet());

        // Fade các phần tử của lịch khi sheet được kéo lên (fade range: đáy lịch -> month_label).
        sheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            private int lastState = BottomSheetBehavior.STATE_COLLAPSED;
            private Integer dragStartTop = null;
            private Integer dragStartStableState = null;

            // If the user drags even a little from a stable end state, we want to
            // "commit" in that drag direction (collapsed -> expanded, expanded -> collapsed).
            // This is expressed as a tiny % of the total travel between our two anchors.
            private static final float COMMIT_FRACTION = 0.02f; // 2% travel

            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (sheetBehavior == null) return;

                // Một số OEM truyền vào reference khác; luôn ưu tiên dùng view sheet thật.
                final View sheet = habitBottomSheetView != null ? habitBottomSheetView : bottomSheet;

                // Capture start position when drag begins.
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    dragStartTop = sheet.getTop();
                    dragStartStableState = lastStableState;
                }

                // Decide snap target when the user releases (DRAGGING -> SETTLING).
                if (lastState == BottomSheetBehavior.STATE_DRAGGING
                        && newState == BottomSheetBehavior.STATE_SETTLING) {

                    final int endTop = sheet.getTop();
                    final int startTop = dragStartTop != null ? dragStartTop : endTop;

                    // Our anchors (in root coords):
                    // - fadeStartTopPx == collapsed top (calendar bottom)
                    // - fadeEndTopPx   == expanded top  (month nav bottom)
                    final int anchorCollapsedTop = fadeStartTopPx;
                    final int anchorExpandedTop = fadeEndTopPx;
                    final int travel = Math.max(1, anchorCollapsedTop - anchorExpandedTop);

                    final int delta = startTop - endTop; // + = moved up, - = moved down
                    final float absDeltaFraction = Math.abs(delta) / (float) travel;

                    final boolean movedUp = delta > 0;
                    final boolean movedDown = delta < 0;

                    if (DEBUG_FADE) {
                        Log.d(TAG_FADE, "SETTLING startTop=" + startTop + " endTop=" + endTop
                                + " delta=" + delta + " frac=" + absDeltaFraction
                                + " anchors(expanded=" + anchorExpandedTop + ", collapsed=" + anchorCollapsedTop + ")"
                                + " stableStart=" + dragStartStableState);
                    }

                    // If user moved even a little from a stable state, snap in that direction.
                    // (Otherwise fall back to nearest anchor.)
                    if (absDeltaFraction >= COMMIT_FRACTION && dragStartStableState != null) {
                        if (dragStartStableState == BottomSheetBehavior.STATE_COLLAPSED && movedUp) {
                            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                        } else if (dragStartStableState == BottomSheetBehavior.STATE_EXPANDED && movedDown) {
                            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                        } else {
                            // Dragged "the wrong way" for the starting state; snap to nearest.
                            int mid = (anchorCollapsedTop + anchorExpandedTop) / 2;
                            sheetBehavior.setState(endTop <= mid
                                    ? BottomSheetBehavior.STATE_EXPANDED
                                    : BottomSheetBehavior.STATE_COLLAPSED);
                        }
                    } else {
                        // Very tiny/no movement: snap to nearest anchor.
                        int mid = (anchorCollapsedTop + anchorExpandedTop) / 2;
                        sheetBehavior.setState(endTop <= mid
                                ? BottomSheetBehavior.STATE_EXPANDED
                                : BottomSheetBehavior.STATE_COLLAPSED);
                    }

                    dragStartTop = null;
                    dragStartStableState = null;
                }

                // Nếu vì lý do nào đó vẫn rơi vào HALF_EXPANDED, ép về đầu gần nhất.
                if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED) {
                    int top = sheet.getTop();
                    int mid = (fadeStartTopPx + fadeEndTopPx) / 2;
                    sheetBehavior.setState(top <= mid
                            ? BottomSheetBehavior.STATE_EXPANDED
                            : BottomSheetBehavior.STATE_COLLAPSED);
                }

                // Quan trọng: chuyển đổi hiển thị tháng/ngày chỉ thực hiện ở trạng thái ổn định (end states).
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    applyMonthLabelMorph(0f);
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    applyMonthLabelMorph(1f);
                }

                // Theo dõi trạng thái ổn định gần nhất để toggle theo hướng kéo.
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

                // Luôn đọc top từ sheet thật sự đang được điều khiển.
                final View sheet = habitBottomSheetView != null ? habitBottomSheetView : bottomSheet;
                int top = sheet.getTop();

                // Nếu biên bị stale (xoay màn hình / đổi kích thước header), tính lại 1 lần.
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
        // monthLabel/dayIndicatorLabel cũng có animation khi chuyển tháng.
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

        // Nếu chạy quá sớm, kích thước có thể = 0 làm hỏng phép tính.
        if (root.getHeight() == 0 || bottomSheet.getHeight() == 0) {
            root.post(this::configureSheetHeights);
            return;
        }

        int calendarBottomInRoot = getBottomInAncestorCoords(binding.calendarContainer, root);
        int navRowBottomInRoot = getBottomInAncestorCoords(binding.monthNavigationRow, root);

        // Expanded offset: top của sheet dừng ở đáy của hàng điều hướng tháng.
        // Clamp >= 0.
        int expandedOffset = Math.max(0, navRowBottomInRoot);
        sheetBehavior.setExpandedOffset(expandedOffset);

        // Collapsed (thấp nhất) phải đúng bằng đáy của lịch.
        int parentHeight = root.getHeight();
        int peekHeight = Math.max(0, parentHeight - calendarBottomInRoot);
        sheetBehavior.setPeekHeight(peekHeight, true);

        // Lưu lại biên fade (sheet.getTop() dùng chung hệ toạ độ root)
        fadeStartTopPx = calendarBottomInRoot;
        fadeEndTopPx = expandedOffset;

        // An toàn: nếu layout bất thường dẫn tới biên bị đảo, tránh bị khoá alpha.
        if (fadeEndTopPx >= fadeStartTopPx) {
            // Thử tính lại theo toạ độ cửa sổ (window) cho nhất quán.
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

        // Ép về collapsed sau khi (re)configure để UI ổn định.
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Đảm bảo alpha + label phản ánh vị trí hiện tại sau khi thiết lập trạng thái.
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

    /**
     * Trả về toạ độ Y (đáy) của view con theo hệ toạ độ của view ancestor.
     * Hoạt động kể cả khi không phải quan hệ cha/con trực tiếp.
     */
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

        // Chỉ animate chuyển tháng khi sheet đang ở trạng thái collapsed.
        // Nếu không thì callback fade theo kéo sheet sẽ "giành quyền" alpha.
        if (sheetBehavior != null && sheetBehavior.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            // Vẫn giữ rule UX: chuyển tháng sẽ collapse sheet.
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            applyMonthLabelMorph(0f);

            // Huỷ animation đang chạy và chuyển tháng ngay lập tức.
            cancelCalendarFadeAnimations();
            moveMonth(offset);

            // Tính lại anchor vì chiều cao header có thể đổi.
            binding.getRoot().post(this::configureSheetHeights);

            isMonthAnimating = false;
            return;
        }

        isMonthAnimating = true;

        // Sheet luôn collapse khi chuyển tháng để tránh chồng label.
        if (sheetBehavior != null) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        // Ép label về trạng thái collapsed ngay.
        applyMonthLabelMorph(0f);

        // Đảm bảo animation cũ không tiếp tục chi phối alpha.
        cancelCalendarFadeAnimations();

        // Fade lưới lịch/ hàng ngày; giữ label tháng vẫn đọc được nhưng animate riêng
        View fadeTarget = binding.calendarDaysRecycler;
        View fadeWeekdays = binding.weekdaysRow;
        View monthLabel = binding.monthLabel;

        // Hướng trượt: tháng sau trượt sang trái, tháng trước trượt sang phải
        float slideDistance = monthLabel.getResources().getDisplayMetrics().density * 12f;
        float dir = offset > 0 ? -1f : 1f;

        // Dừng animation đang chạy để tránh chồng nếu user bấm liên tục
        fadeTarget.animate().cancel();
        fadeWeekdays.animate().cancel();
        monthLabel.animate().cancel();
        if (dayIndicatorLabel != null) {
            dayIndicatorLabel.animate().cancel();
            // Đảm bảo overlay bị ẩn trong lúc đổi tháng
            dayIndicatorLabel.setAlpha(0f);
        }

        // Pha 1: fade/slide out label tháng hiện tại + fade out grid
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

                    // Áp dụng thay đổi tháng sau khi tháng cũ đã fade out.
                    moveMonth(offset);

                    // Tính lại anchor vì chiều cao header có thể đổi.
                    binding.getRoot().post(this::configureSheetHeights);

                    // Chuyển tháng luôn collapse sheet, nên khôi phục alpha về hiển thị đầy đủ.
                    float targetAlpha = 1f;

                    // Reset label về phía ngược lại trước khi slide vào
                    monthLabel.setTranslationX(-dir * slideDistance);
                    monthLabel.setAlpha(0f);

                    // Reset grid elements về 0 trước khi fade in
                    fadeWeekdays.setAlpha(0f);
                    fadeTarget.setAlpha(0f);

                    // Pha 2: fade/slide in label tháng mới + fade in grid
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

        // Cập nhật day indicator vì ngày đang chọn đã thay đổi theo tháng.
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
            // Nếu đang xem ngày trong quá khứ, thói quen còn pending coi như missed.
            if ("PENDING".equals(normalized) && isSelectedDateInPast()) {
                return HabitCompletion.Status.MISSED;
            }
        }
        if (view.getTargetValue() > 0 && view.getCurrentValue() >= view.getTargetValue()) {
            return HabitCompletion.Status.COMPLETED;
        }
        // Nếu không có status rõ ràng, vẫn áp dụng quy tắc "pending ở quá khứ -> missed".
        if (isSelectedDateInPast()) {
            return HabitCompletion.Status.MISSED;
        }
        return HabitCompletion.Status.PENDING;
    }

    private boolean isSelectedDateInPast() {
        Calendar today = Calendar.getInstance();
        // Chuẩn hoá về đầu ngày
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

        // Cập nhật day indicator ngay khi user chọn ngày mới.
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
