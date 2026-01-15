package com.example.habittracker.ui.achievements;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.habittracker.R;
import com.example.habittracker.data.achievements.AchievementId;
import com.example.habittracker.data.achievements.AchievementsRepository;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.data.repository.callback.StreakCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Date;

public class AchievementDetailsDialogFragment extends DialogFragment {

    private static final String ARG_ID = "ARG_ID";
    private static final String ARG_ICON = "ARG_ICON";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_DESC = "ARG_DESC";
    private static final String ARG_UNLOCKED_AT = "ARG_UNLOCKED_AT";

    public static AchievementDetailsDialogFragment newInstance(
            @NonNull AchievementId id,
            @DrawableRes int iconRes,
            String title,
            String description,
            @Nullable Long unlockedAtMillis
    ) {
        AchievementDetailsDialogFragment f = new AchievementDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id.name());
        args.putInt(ARG_ICON, iconRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, description);
        if (unlockedAtMillis != null) args.putLong(ARG_UNLOCKED_AT, unlockedAtMillis);
        f.setArguments(args);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Prefer fragment inflater.
        LayoutInflater inflater = getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_achievement_details, null, false);

        ImageView icon = content.findViewById(R.id.achievement_detail_icon);
        TextView tvTitle = content.findViewById(R.id.achievement_detail_title);
        TextView tvDesc = content.findViewById(R.id.achievement_detail_description);
        TextView tvTime = content.findViewById(R.id.achievement_detail_time);

        View progressContainer = content.findViewById(R.id.achievement_detail_progress_container);
        TextView progressLabel = content.findViewById(R.id.achievement_detail_progress_label);
        ProgressBar progressBar = content.findViewById(R.id.achievement_detail_progress_bar);

        Bundle args = getArguments();
        int iconRes = args != null ? args.getInt(ARG_ICON, R.drawable.ic_cup) : R.drawable.ic_cup;
        String title = args != null ? args.getString(ARG_TITLE, "") : "";
        String desc = args != null ? args.getString(ARG_DESC, "") : "";

        AchievementId id = null;
        if (args != null) {
            String idStr = args.getString(ARG_ID, null);
            if (idStr != null) {
                try {
                    id = AchievementId.valueOf(idStr);
                } catch (Exception ignored) {
                    // Keep null; we'll just hide progress.
                }
            }
        }

        boolean hasUnlocked = args != null && args.containsKey(ARG_UNLOCKED_AT);
        Long unlockedAt = hasUnlocked ? args.getLong(ARG_UNLOCKED_AT) : null;

        icon.setImageResource(iconRes);
        tvTitle.setText(title);
        tvDesc.setText(desc);

        if (unlockedAt != null) {
            String formatted = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                    .format(new Date(unlockedAt));
            tvTime.setText(getString(R.string.achievement_unlocked_at, formatted));
        } else {
            tvTime.setText(getString(R.string.achievement_not_yet_unlocked));
        }

        // Requirement: hide progress UI once unlocked.
        if (progressContainer != null) {
            progressContainer.setVisibility(unlockedAt != null ? View.GONE : View.VISIBLE);
        }

        // Async fetch progress on dialog open (only for locked achievements).
        if (unlockedAt == null && id != null && progressContainer != null && progressLabel != null && progressBar != null) {
            progressBar.setIndeterminate(true);
            progressLabel.setText(R.string.achievement_progress_loading);
            loadAchievementProgressAsync(id, progressLabel, progressBar, progressContainer);
        } else {
            // If we can't compute progress for any reason, hide the container to avoid confusing UI.
            if (unlockedAt == null && progressContainer != null) {
                progressContainer.setVisibility(View.GONE);
            }
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setPositiveButton(R.string.close, (d, w) -> {})
                .create();
    }

    private void loadAchievementProgressAsync(
            @NonNull AchievementId id,
            @NonNull TextView label,
            @NonNull ProgressBar bar,
            @NonNull View container
    ) {
        // Assumption: progress is only meaningful for a subset of achievements.
        // If not supported, we hide the section.
        AchievementsRepository repo = new AchievementsRepository(requireContext().getApplicationContext());

        switch (id) {
            case THREE_HABITS_CREATED: {
                int current = repo.getHabitsCreated();
                bindProgress(current, 3, label, bar, container);
                break;
            }
            case SEVEN_HABITS_CREATED: {
                int current = repo.getHabitsCreated();
                bindProgress(current, 7, label, bar, container);
                break;
            }
            case PERFECT_3_DAYS: {
                int current = repo.getPerfectDays().size();
                bindProgress(current, 3, label, bar, container);
                break;
            }
            case CHECKIN_STREAK_3:
            case CHECKIN_STREAK_7:
            case LONGEST_STREAK_7: {
                // These depend on repo computation that currently hits Firestore.
                // Fetch async and update UI when done.
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid == null || uid.isEmpty()) {
                    container.setVisibility(View.GONE);
                    return;
                }

                HabitRepository habitRepository = new HabitRepository(uid);
                habitRepository.calculateUserStreaks(new StreakCallback() {
                    @Override
                    public void onStreakCalculated(int currentStreak, int longestStreak) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            int target;
                            int current;
                            if (id == AchievementId.CHECKIN_STREAK_3) {
                                target = 3;
                                current = currentStreak;
                            } else if (id == AchievementId.CHECKIN_STREAK_7) {
                                target = 7;
                                current = currentStreak;
                            } else {
                                target = 7;
                                current = longestStreak;
                            }
                            bindProgress(current, target, label, bar, container);
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> container.setVisibility(View.GONE));
                    }
                });
                break;
            }
            default:
                container.setVisibility(View.GONE);
                break;
        }
    }

    private void bindProgress(int current, int target, @NonNull TextView label, @NonNull ProgressBar bar, @NonNull View container) {
        if (target <= 0) {
            container.setVisibility(View.GONE);
            return;
        }

        int safeCurrent = Math.max(0, Math.min(current, target));

        bar.setIndeterminate(false);
        bar.setMax(target);
        bar.setProgress(safeCurrent);

        label.setText(getString(R.string.achievement_progress_format, safeCurrent, target));
        container.setVisibility(View.VISIBLE);
    }
}
