package com.example.habittracker.ui.achievements;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.habittracker.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;

public class AchievementDetailsDialogFragment extends DialogFragment {

    private static final String ARG_ICON = "ARG_ICON";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_DESC = "ARG_DESC";
    private static final String ARG_UNLOCKED_AT = "ARG_UNLOCKED_AT";

    public static AchievementDetailsDialogFragment newInstance(
            @DrawableRes int iconRes,
            String title,
            String description,
            @Nullable Long unlockedAtMillis
    ) {
        AchievementDetailsDialogFragment f = new AchievementDetailsDialogFragment();
        Bundle args = new Bundle();
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
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View content = inflater.inflate(R.layout.dialog_achievement_details, null, false);

        ImageView icon = content.findViewById(R.id.achievement_detail_icon);
        TextView tvTitle = content.findViewById(R.id.achievement_detail_title);
        TextView tvDesc = content.findViewById(R.id.achievement_detail_description);
        TextView tvTime = content.findViewById(R.id.achievement_detail_time);

        Bundle args = getArguments();
        int iconRes = args != null ? args.getInt(ARG_ICON, R.drawable.ic_cup) : R.drawable.ic_cup;
        String title = args != null ? args.getString(ARG_TITLE, "") : "";
        String desc = args != null ? args.getString(ARG_DESC, "") : "";

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

        return new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .setPositiveButton(R.string.close, (d, w) -> {})
                .create();
    }
}
