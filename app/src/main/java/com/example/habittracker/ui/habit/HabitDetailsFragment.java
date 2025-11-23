package com.example.habittracker.ui.habit;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.habittracker.data.model.Habit;
import com.example.habittracker.data.repository.callback.HabitQueryCallback;
import com.example.habittracker.data.repository.callback.SimpleCallback;
import com.example.habittracker.data.repository.HabitRepository;
import com.example.habittracker.R;
import com.example.habittracker.data.repository.callback.StatsCallback;
import com.example.habittracker.databinding.FragmentHabitDetailsBinding;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.example.habittracker.data.model.HabitHistory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class HabitDetailsFragment extends Fragment {

    private FragmentHabitDetailsBinding binding;
    private NavController navController;
    private HabitRepository habitRepository;
    private String habitId;

    private LineChart lineChart;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHabitDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = NavHostFragment.findNavController(this);

        // Khởi tạo Repository
        String uid = FirebaseAuth.getInstance().getUid();
        habitRepository = new HabitRepository(uid);

        // Lấy Habit ID được gửi từ HomeFragment
        if (getArguments() != null) {
            habitId = getArguments().getString("EXTRA_HABIT_ID");
        }

        // Nút "Back"
        binding.btnBack.setOnClickListener(v -> {
            navController.popBackStack();
        });

        // Nút "Edit Habit"
        binding.btnEditHabit.setOnClickListener(v -> {
            if (habitId != null) {
                // Đóng gói ID để gửi sang màn hình Edit
                Bundle args = new Bundle();
                args.putString("EXTRA_HABIT_ID", habitId);

                // Điều hướng sang màn hình Add/Edit
                navController.navigate(R.id.action_habitDetailsFragment_to_addEditHabitFragment, args);
            } else {
                Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID thói quen", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút "Delete Habit"
        binding.btnDeleteHabit.setOnClickListener(v -> {
            showDeleteConfirmationDialog();
        });

        if (habitId != null) {
            loadHabitInfo();       // Tải tên, mô tả
            loadHabitStatistics(); // Tải số liệu (Tuần, Tháng, Tổng)
        } else {
            Toast.makeText(getContext(), "Lỗi: Không tìm thấy ID", Toast.LENGTH_SHORT).show();
        }

        // --- CODE BIỂU ĐỒ BẮT ĐẦU TỪ ĐÂY ---

        // 1. Ánh xạ và Cấu hình giao diện biểu đồ
        lineChart = binding.lineChart;
        setupChartStyle();

        // 2. Xử lý khi bấm nút Day/Week/Month
        binding.rgTimeFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (habitId == null) return;

            if (checkedId == R.id.rb_day) {
                loadChartData(7); // Xem 7 ngày
                binding.tvChartTitle.setText("Last 7 Days");
            } else if (checkedId == R.id.rb_week) {
                loadChartData(28); // Xem 4 tuần
                binding.tvChartTitle.setText("Last 4 Weeks");
            } else if (checkedId == R.id.rb_month) {
                loadChartData(90); // Xem 3 tháng
                binding.tvChartTitle.setText("Last 3 Months");
            }
        });

        // 3. Mặc định tải 7 ngày đầu tiên khi mở màn hình
        if (habitId != null) {
            loadChartData(7);
        }
    }

    private void loadHabitInfo() {
        habitRepository.getHabitById(habitId, new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {
                if (!result.isEmpty()) {
                    Habit habit = (Habit) result.get(0);
                    // Gán tên thói quen vào TextView title
                    binding.tvDetailTitle.setText(habit.getTitle());

                    // Nếu bạn có TextView mô tả, gán ở đây:
                    // binding.tvDescription.setText(habit.getDescription());
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Lỗi tải thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Hàm 2: Tải thống kê (Tuần, Tháng, Tổng)
    private void loadHabitStatistics() {
        habitRepository.getHabitStatistics(habitId, new StatsCallback() {
            @Override
            public void onStatsLoaded(int week, int month, int total) {
                // Đảm bảo cập nhật UI
                if (binding != null) {
                    binding.tvDetailThisWeek.setText(String.valueOf(week));
                    binding.tvDetailThisMonth.setText(String.valueOf(month));
                    binding.tvDetailTotalDone.setText(String.valueOf(total));
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải thống kê", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa thói quen?")
                .setMessage("Bạn có chắc chắn muốn xóa thói quen này không? Dữ liệu lịch sử vẫn sẽ được lưu trữ.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    performDelete();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void performDelete() {
        if (habitId == null) return;

        // Gọi hàm archiveHabit (Xóa mềm) trong Repository
        habitRepository.archiveHabit(habitId, new SimpleCallback() {
            @Override
            public void onComplete(boolean success, Exception e) {
                if (success) {
                    Toast.makeText(getContext(), "Đã xóa thói quen", Toast.LENGTH_SHORT).show();
                    navController.popBackStack(); // Quay về Home sau khi xóa thành công
                } else {
                    Toast.makeText(getContext(), "Lỗi khi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // HÀM 1: Trang trí biểu đồ cho đẹp (Tắt lưới, làm mờ...)
    private void setupChartStyle() {
        if (lineChart == null) return;

        lineChart.getDescription().setEnabled(false); // Tắt chữ Description
        lineChart.getLegend().setEnabled(false); // Tắt chú thích
        lineChart.setTouchEnabled(false); // Không cho zoom/kéo

        // Trục X (Thời gian - Ở dưới)
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(android.graphics.Color.WHITE);
        xAxis.setDrawGridLines(false); // Tắt lưới dọc

        // Trục Y (Giá trị - Bên trái)
        lineChart.getAxisLeft().setTextColor(android.graphics.Color.WHITE);
        lineChart.getAxisLeft().setGridColor(android.graphics.Color.parseColor("#33FFFFFF")); // Lưới ngang mờ
        lineChart.getAxisRight().setEnabled(false); // Tắt trục phải

        // Hiệu ứng
        lineChart.animateY(1000);
    }

    // HÀM 2: Tải dữ liệu và Vẽ
    private void loadChartData(int daysBack) {
        // Cập nhật tiêu đề biểu đồ ngay khi bấm nút
        String title = "";
        if (daysBack == 7) title = "Last 7 Days";
        else if (daysBack == 28) title = "Last 4 Weeks";
        else title = "Last 3 Months";
        binding.tvChartTitle.setText(title);
        // Tính ngày bắt đầu (Hôm nay - daysBack)
        Calendar startCal = Calendar.getInstance();
        startCal.add(Calendar.DAY_OF_YEAR, -daysBack);

        // Gọi Repository lấy dữ liệu
        habitRepository.getHistoryForChart(habitId, startCal.getTimeInMillis(), new HabitQueryCallback() {
            @Override
            public void onSuccess(List<?> result) {

                // Tổng số lần hoàn thành trong khoảng thời gian này
                int totalCount = result.size();
                binding.tvChartValue.setText(String.valueOf(totalCount));

                // Cập nhật dòng chữ xanh (Trend)
                // Vì ta chưa có dữ liệu quá khứ để so sánh (+10%), nên ta sẽ hiện câu động viên
                if (totalCount > 0) {
                    // Tính trung bình: Ví dụ 0.5 lần/ngày
                    double avg = (double) totalCount / daysBack;
                    String avgStr = String.format("%.1f times/day", avg);
                    binding.tvChartTrend.setText(avgStr);
                    binding.tvChartTrend.setTextColor(android.graphics.Color.parseColor("#FF0AD95C")); // Màu xanh
                } else {
                    binding.tvChartTrend.setText("No data yet");
                    binding.tvChartTrend.setTextColor(android.graphics.Color.GRAY);
                }
                // XỬ LÝ DỮ LIỆU ĐỂ VẼ
                ArrayList<Entry> values = new ArrayList<>();
                ArrayList<String> labels = new ArrayList<>();

                // Tạo trục X giả lập (0, 1, 2... đến daysBack)
                for (int i = 0; i < daysBack; i++) {
                    // Logic đơn giản: Nếu ngày này có trong result -> Giá trị = 1, không thì = 0
                    // (Đây là logic demo, bạn có thể nâng cấp sau để check đúng ngày tháng)
                    float val = 0;
                    if (i < result.size()) val = 1; // Demo: Cứ có bao nhiêu bản ghi thì vẽ bấy nhiêu điểm

                    values.add(new Entry(i, val));
                    labels.add("D" + (i+1));
                }

                // TẠO ĐƯỜNG KẺ (DATASET)
                LineDataSet set1 = new LineDataSet(values, "Data");
                set1.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Làm cong đường kẻ (QUAN TRỌNG)
                set1.setCubicIntensity(0.2f);
                set1.setDrawFilled(true); // Tô màu bên dưới
                set1.setFillColor(android.graphics.Color.parseColor("#2196F3")); // Màu xanh
                set1.setFillAlpha(50); // Độ trong suốt
                set1.setLineWidth(2f);
                set1.setDrawCircles(false); // Bỏ dấu chấm tròn
                set1.setDrawValues(false); // Bỏ số trên đường kẻ

                // Đổ dữ liệu vào Chart
                LineData data = new LineData(set1);
                lineChart.setData(data);
                lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels)); // Gắn nhãn D1, D2...
                lineChart.invalidate(); // Vẽ lại
            }

            @Override
            public void onFailure(Exception e) {
                // SỬA: In lỗi ra Logcat và Toast để biết nguyên nhân
                Log.e("CHART_ERROR", "Lỗi tải biểu đồ: " + e.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi Chart: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}