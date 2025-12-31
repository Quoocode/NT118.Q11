package com.example.habittracker.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.habittracker.R;
import com.example.habittracker.data.achievements.AchievementService;
import com.example.habittracker.data.repository.callback.DataCallback;
import com.example.habittracker.ui.ViewModel.HabitViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Replaces the old check-in popup with a bottom sheet that:
 * - matches Calendar tab sheet styling
 * - shows: Habit options + habit name
 * - contains the full old check-in fields (target/current/status/update)
 */
public class HabitOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_HABIT_ID = "ARG_HABIT_ID";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_TARGET = "ARG_TARGET";
    private static final String ARG_CURRENT = "ARG_CURRENT";
    private static final String ARG_UNIT = "ARG_UNIT";
    private static final String ARG_STATUS = "ARG_STATUS";

    private AchievementService achievementService;
    private HabitViewModel habitViewModel;

    private HabitCheckInDialogFragment.OnCheckInListener listener;

    private boolean isProgrammaticChange = false;

    public static HabitOptionsBottomSheetDialogFragment newInstance(
            @NonNull String habitId,
            @NonNull String title,
            double target,
            double current,
            @Nullable String unit,
            @Nullable String status
    ) {
        HabitOptionsBottomSheetDialogFragment fragment = new HabitOptionsBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HABIT_ID, habitId);
        args.putString(ARG_TITLE, title);
        args.putDouble(ARG_TARGET, target);
        args.putDouble(ARG_CURRENT, current);
        args.putString(ARG_UNIT, unit);
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    /** Keep same listener contract as the old dialog so callers don't change much. */
    public void setOnCheckInListener(@Nullable HabitCheckInDialogFragment.OnCheckInListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_habit_options_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        achievementService = new AchievementService(requireContext());
        habitViewModel = new ViewModelProvider(requireActivity()).get(HabitViewModel.class);

        TextView tvHabitName = view.findViewById(R.id.tv_habit_name);
        TextView tvTarget = view.findViewById(R.id.tv_habit_target);
        TextView tvUnit = view.findViewById(R.id.tv_unit);
        EditText edtCurrentValue = view.findViewById(R.id.edt_current_value);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_status);
        RadioButton radioPending = view.findViewById(R.id.radio_pending);
        RadioButton radioDone = view.findViewById(R.id.radio_done);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        View btnCancel = view.findViewById(R.id.btn_cancel);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        String habitId = args.getString(ARG_HABIT_ID);
        String title = args.getString(ARG_TITLE);
        double target = args.getDouble(ARG_TARGET);
        double current = args.getDouble(ARG_CURRENT);
        String unit = args.getString(ARG_UNIT);
        String status = args.getString(ARG_STATUS);

        tvHabitName.setText(title != null ? title : "");

        String targetStr = (target == (long) target) ? String.valueOf((long) target) : String.valueOf(target);
        tvTarget.setText(getString(R.string.target_format, targetStr, unit != null ? unit : ""));
        tvUnit.setText(unit != null ? unit : "");

        if (current == (long) current) {
            edtCurrentValue.setText(String.valueOf((long) current));
        } else {
            edtCurrentValue.setText(String.valueOf(current));
        }

        if ("DONE".equals(status)) {
            radioDone.setChecked(true);
        } else {
            radioPending.setChecked(true);
        }

        // --- same auto behaviors as old dialog ---
        edtCurrentValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isProgrammaticChange) return;
                String valStr = s.toString();
                if (valStr.isEmpty()) return;

                try {
                    double val = Double.parseDouble(valStr);
                    if (val >= target) {
                        if (!radioDone.isChecked()) {
                            isProgrammaticChange = true;
                            radioDone.setChecked(true);
                            isProgrammaticChange = false;
                        }
                    } else {
                        if (!radioPending.isChecked()) {
                            isProgrammaticChange = true;
                            radioPending.setChecked(true);
                            isProgrammaticChange = false;
                        }
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        });

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (isProgrammaticChange) return;

            if (checkedId == R.id.radio_done) {
                isProgrammaticChange = true;
                edtCurrentValue.setText(targetStr);
                edtCurrentValue.setSelection(edtCurrentValue.getText().length());
                isProgrammaticChange = false;
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String valueStr = edtCurrentValue.getText() != null ? edtCurrentValue.getText().toString() : "";
            if (valueStr.isEmpty()) return;
            if (habitId == null) return;

            double newValue;
            try {
                newValue = Double.parseDouble(valueStr);
            } catch (NumberFormatException e) {
                return;
            }

            String newStatus = radioDone.isChecked() ? "DONE" : "PENDING";

            habitViewModel.performCheckIn(habitId, newValue, newStatus, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();

                    if (achievementService != null) {
                        achievementService.onCheckInCommitted(newStatus, newValue, target);
                    }
                    if (listener != null) {
                        listener.onCheckInCompleted();
                    }
                    dismiss();
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(v -> dismiss());
    }
}

