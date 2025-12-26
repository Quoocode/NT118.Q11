package com.example.habittracker.ui.achievements;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.habittracker.R;
import com.example.habittracker.data.achievements.AchievementUiModel;
import com.example.habittracker.databinding.FragmentAchievementsBinding;

import java.util.ArrayList;
import java.util.List;

public class AchievementsFragment extends Fragment {

    private FragmentAchievementsBinding binding;

    private AchievementsViewModel viewModel;

    private boolean hasAnimatedOnce = false;
    private boolean isExpanded = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAchievementsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AchievementsViewModel.class);

        setupExpandCollapse();

        viewModel.getAchievements().observe(getViewLifecycleOwner(), list -> {
            if (binding == null) return;

            renderProgress(list);
            renderBadges(list);
        });

        // Optional: quick seed so the UI is not empty on a fresh install.
        // Comment this out once you start wiring real unlock events.
        // viewModel.debugUnlockFirstThree();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    private void setupExpandCollapse() {
        View root = binding.getRoot();
        View horizontalContainer = root.findViewById(R.id.badges_list_container);
        View gridContainer = root.findViewById(R.id.badge_grid_container);
        ImageButton expandButton = root.findViewById(R.id.btn_badge_expand);
        Button collapseButton = root.findViewById(R.id.btn_badge_collapse);

        if (horizontalContainer == null || gridContainer == null || expandButton == null || collapseButton == null) {
            return;
        }

        collapseButton.setOnClickListener(v -> collapseBadges(horizontalContainer, gridContainer, expandButton));
        expandButton.setOnClickListener(v -> expandBadges(horizontalContainer, gridContainer, expandButton));
    }

    private void expandBadges(View horizontalContainer, View gridContainer, ImageButton expandButton) {
        if (isExpanded) return;
        isExpanded = true;
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
        if (!isExpanded) return;
        isExpanded = false;
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

    private void renderProgress(List<AchievementUiModel> list) {
        TextView count = binding.getRoot().findViewById(R.id.tv_achievement_progress_count);
        ProgressBar bar = binding.getRoot().findViewById(R.id.progress_achievement_total);
        if (count == null || bar == null) return;

        int total = list.size();
        int unlocked = 0;
        for (AchievementUiModel m : list) {
            if (m.isUnlocked()) unlocked++;
        }

        count.setText(getString(R.string.achievements_progress_count, unlocked, total));

        bar.setMax(Math.max(total, 1));

        // Mirror Home: animate progress bar movement.
        int from = bar.getProgress();
        int to = unlocked;

        if (!hasAnimatedOnce) {
            // First time on this tab: animate from 0.
            from = 0;
            bar.setProgress(0);
            hasAnimatedOnce = true;
        }

        if (from != to) {
            ObjectAnimator.ofInt(bar, "progress", from, to)
                    .setDuration(500)
                    .start();
        }
    }

    private void renderBadges(List<AchievementUiModel> list) {
        View root = binding.getRoot();
        LinearLayout horizontal = root.findViewById(R.id.badges_horizontal_list);
        GridLayout grid = root.findViewById(R.id.badges_grid);
        if (horizontal == null || grid == null || getContext() == null) return;

        horizontal.removeAllViews();
        grid.removeAllViews();

        // Requirement: show 3 achieved ones first on the collapsed view.
        List<AchievementUiModel> unlocked = new ArrayList<>();
        List<AchievementUiModel> locked = new ArrayList<>();
        for (AchievementUiModel m : list) {
            if (m.isUnlocked()) unlocked.add(m);
            else locked.add(m);
        }

        List<AchievementUiModel> collapsedList = new ArrayList<>();
        for (int i = 0; i < Math.min(3, unlocked.size()); i++) collapsedList.add(unlocked.get(i));
        collapsedList.addAll(locked);

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (AchievementUiModel m : collapsedList) {
            View item = inflater.inflate(R.layout.item_badge_placeholder, horizontal, false);
            bindBadgeItem(item, m);
            horizontal.addView(item);
        }

        List<AchievementUiModel> gridList = new ArrayList<>();
        gridList.addAll(unlocked);
        gridList.addAll(locked);

        for (AchievementUiModel m : gridList) {
            View item = inflater.inflate(R.layout.item_badge_placeholder, grid, false);
            bindBadgeItem(item, m);

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(8, 8, 8, 8);
            item.setLayoutParams(lp);

            grid.addView(item);
        }
    }

    private void bindBadgeItem(View item, AchievementUiModel m) {
        ImageView icon = item.findViewById(R.id.badge_icon);
        TextView title = item.findViewById(R.id.badge_title);

        if (icon != null) icon.setImageResource(m.getIconRes());
        if (title != null) title.setText(m.getTitle());

        // Locked styling: dim.
        float alpha = m.isUnlocked() ? 1f : 0.35f;
        item.setAlpha(alpha);

        ViewCompat.setTooltipText(item, m.isUnlocked() ? "Unlocked" : "Locked");

        item.setOnClickListener(v -> {
            AchievementDetailsDialogFragment dialog = AchievementDetailsDialogFragment.newInstance(
                    m.getIconRes(),
                    m.getTitle(),
                    m.getDescription(),
                    m.getUnlockedAtMillis()
            );
            dialog.show(getChildFragmentManager(), "AchievementDetails");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}