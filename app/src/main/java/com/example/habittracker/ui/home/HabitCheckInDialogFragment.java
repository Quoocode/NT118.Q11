package com.example.habittracker.ui.home;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider; // Mới

import com.example.habittracker.R;
import com.example.habittracker.data.repository.callback.DataCallback; // Mới
import com.example.habittracker.ui.ViewModel.HabitViewModel; // Mới
import com.example.habittracker.data.achievements.AchievementService;
import com.example.habittracker.data.repository.HabitRepository; // Sửa lại package import cho đúng với dự án của bạn
import com.google.firebase.auth.FirebaseAuth;
import com.example.habittracker.data.repository.callback.SimpleCallback;

import java.util.Calendar;

public class HabitCheckInDialogFragment extends DialogFragment {

    private static final String ARG_HABIT_ID = "ARG_HABIT_ID";
    private static final String ARG_TITLE = "ARG_TITLE";
    private static final String ARG_TARGET = "ARG_TARGET";
    private static final String ARG_CURRENT = "ARG_CURRENT";
    private static final String ARG_UNIT = "ARG_UNIT";
    private static final String ARG_STATUS = "ARG_STATUS";

    private AchievementService achievementService;
    // Thay Repository bằng ViewModel
    private HabitViewModel habitViewModel;

    private OnCheckInListener listener;

    // Biến cờ để tránh vòng lặp vô tận giữa TextWatcher và RadioListener
    private boolean isProgrammaticChange = false;

    public interface OnCheckInListener {
        void onCheckInCompleted();
    }

    public void setOnCheckInListener(OnCheckInListener listener) {
        this.listener = listener;
    }

    public static HabitCheckInDialogFragment newInstance(String habitId, String title, double target, double current, String unit, String status) {
        HabitCheckInDialogFragment fragment = new HabitCheckInDialogFragment();
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Đảm bảo bạn có file layout này (tên cũ là activity_habit_check_in hoặc fragment_habit_check_in)
        return inflater.inflate(R.layout.fragment_habit_check_in, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userId = FirebaseAuth.getInstance().getUid();
        achievementService = new AchievementService(requireContext());
        // [MỚI] Khởi tạo ViewModel (Liên kết với Activity cha để đồng bộ)
        habitViewModel = new ViewModelProvider(requireActivity()).get(HabitViewModel.class);

        TextView tvTitle = view.findViewById(R.id.tv_habit_title);
        TextView tvTarget = view.findViewById(R.id.tv_habit_target);
        TextView tvUnit = view.findViewById(R.id.tv_unit);
        EditText edtCurrentValue = view.findViewById(R.id.edt_current_value);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_status); // Cần ID cho Group
        RadioButton radioPending = view.findViewById(R.id.radio_pending);
        RadioButton radioDone = view.findViewById(R.id.radio_done);
        Button btnConfirm = view.findViewById(R.id.btn_confirm);



        // Lấy dữ liệu
        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            double target = getArguments().getDouble(ARG_TARGET);
            double current = getArguments().getDouble(ARG_CURRENT);
            String unit = getArguments().getString(ARG_UNIT);
            String status = getArguments().getString(ARG_STATUS);

            tvTitle.setText(title);

            String targetStr = (target == (long) target) ? String.valueOf((long) target) : String.valueOf(target);

            tvTarget.setText(getString(R.string.target_format, targetStr, unit != null ? unit : ""));
            tvUnit.setText(unit != null ? unit : "");

            // Hiển thị giá trị hiện tại (nếu là số nguyên thì bỏ .0)
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
        }

        // --- LOGIC TỰ ĐỘNG 1: NHẬP SỐ -> TỰ CHỌN DONE ---
        edtCurrentValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isProgrammaticChange) return;

                Bundle args = getArguments();
                if (args == null) return;

                String valStr = s.toString();
                if (!valStr.isEmpty()) {
                    try {
                        double val = Double.parseDouble(valStr);
                        double target = args.getDouble(ARG_TARGET);

                        // Nếu nhập >= target -> Tự động chọn DONE
                        if (val >= target) {
                            if (!radioDone.isChecked()) {
                                isProgrammaticChange = true;
                                radioDone.setChecked(true);
                                isProgrammaticChange = false;
                            }
                        } else {
                            // Nếu nhập < target -> Tự động về PENDING (Tùy chọn, có thể bỏ nếu không thích)
                            if (!radioPending.isChecked()) {
                                isProgrammaticChange = true;
                                radioPending.setChecked(true);
                                isProgrammaticChange = false;
                            }
                        }
                    } catch (NumberFormatException ignored) {
                        // ignore
                    }
                }
            }
        });

        // --- LOGIC TỰ ĐỘNG 2: CHỌN DONE -> TỰ ĐIỀN MAX ---
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (isProgrammaticChange) return;

            Bundle args = getArguments();
            if (args == null) return;

            if (checkedId == R.id.radio_done) {
                // Nếu chọn DONE -> Điền giá trị bằng Target
                double target = args.getDouble(ARG_TARGET);
                String targetStr = (target == (long) target) ? String.valueOf((long) target) : String.valueOf(target);

                isProgrammaticChange = true;
                edtCurrentValue.setText(targetStr);
                // Di chuyển con trỏ về cuối
                edtCurrentValue.setSelection(edtCurrentValue.getText().length());
                isProgrammaticChange = false;
            }
        });

        // [MỚI] Xử lý Lưu thông qua ViewModel
        btnConfirm.setOnClickListener(v -> {
            Bundle args = getArguments();
            if (args == null) return;

            String habitId = args.getString(ARG_HABIT_ID);
            String valueStr = edtCurrentValue.getText().toString();
            if (valueStr.isEmpty()) return;

            double newValue = Double.parseDouble(valueStr);

            // Lấy trạng thái cuối cùng từ RadioButton
            String newStatus = radioDone.isChecked() ? "DONE" : "PENDING";

            // Gọi ViewModel để xử lý cả DB và Alarm
            habitViewModel.performCheckIn(habitId, newValue, newStatus, new DataCallback<Boolean>() {
                @Override
                public void onSuccess(Boolean data) {
                    Toast.makeText(getContext(), "Updated!", Toast.LENGTH_SHORT).show();
                    double targetValue = args.getDouble(ARG_TARGET);

                    if (achievementService != null) {
                        achievementService.onCheckInCommitted(newStatus, newValue, targetValue);
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
    }



    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }
}