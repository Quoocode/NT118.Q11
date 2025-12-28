package com.example.habittracker.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// Hãy đảm bảo import đúng file R của dự án bạn
import com.example.habittracker.R;
import com.example.habittracker.data.model.HabitDailyView;

import java.util.List;

public class DashboardHabitAdapter extends RecyclerView.Adapter<DashboardHabitAdapter.HabitViewHolder> {

    private Context context;
    private List<HabitDailyView> habitList;
    private OnHabitCheckListener checkListener;
    private OnHabitItemClickListener itemClickListener;

    // --- INTERFACES ---

    public interface OnHabitCheckListener {
        // Chỉ báo là đã click, không truyền boolean isChecked vì sẽ mở Dialog nhập số
        void onHabitCheckClick(HabitDailyView habit);
    }

    public interface OnHabitItemClickListener {
        void onItemClick(HabitDailyView habit);
    }

    // --- CONSTRUCTOR ---
    public DashboardHabitAdapter(Context context, List<HabitDailyView> habitList,
                                 OnHabitCheckListener checkListener,
                                 OnHabitItemClickListener itemClickListener) {
        this.context = context;
        this.habitList = habitList;
        this.checkListener = checkListener;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dùng tên layout xml chứa giao diện item habit của bạn (VD: item_today_habit)
        View view = LayoutInflater.from(context).inflate(R.layout.item_today_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        HabitDailyView habit = habitList.get(position);

        // 1. Hiển thị Tên
        holder.tvTitle.setText(habit.getTitle());

        // 2. Hiển thị Tiến độ bằng string resource để dịch được
        Double currentValue = habit.getCurrentValue();
        Double targetValue = habit.getTargetValue();

        double current = currentValue != null ? currentValue.doubleValue() : 0.0;
        double target = targetValue != null ? targetValue.doubleValue() : 0.0;
        String unit = habit.getUnit() != null ? habit.getUnit() : "";

        // Lấy chữ Progress theo locale
        String progressLabel = context.getString(R.string.home_progress); // "Progress" hoặc "Tiến độ"
        String progressText = String.format("%s: %.1f / %.1f %s", progressLabel, current, target, unit);
        holder.tvTime.setText(progressText);

        // 3. Hiển thị Icon
        String iconName = habit.getIconName();
        int resId = context.getResources().getIdentifier(iconName, "drawable", context.getPackageName());
        if (resId != 0) {
            holder.imgIcon.setImageResource(resId);
        } else {
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_report_image);
        }

        // 4. Xử lý trạng thái hoàn thành
        boolean isCompleted = current >= target || "DONE".equals(habit.getStatus());
        if (isCompleted) {
            holder.imgCheck.setImageResource(R.drawable.ic_tick_done);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setAlpha(0.5f);
            holder.tvTime.setAlpha(0.5f);
        } else {
            holder.imgCheck.setImageResource(R.drawable.ic_ellipsis);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setAlpha(1f);
            holder.tvTime.setAlpha(1f);
        }

        // 5. Click events
        holder.imgCheck.setOnClickListener(v -> {
            if (checkListener != null) checkListener.onHabitCheckClick(habit);
        });
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null) itemClickListener.onItemClick(habit);
        });
    }


    @Override
    public int getItemCount() {
        return habitList != null ? habitList.size() : 0;
    }

    public void updateData(List<HabitDailyView> newHabitList) {
        this.habitList = newHabitList;
        notifyDataSetChanged();
    }

    // ViewHolder khớp với XML item_today_habit
    public static class HabitViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle;
        TextView tvTime;
        ImageView imgCheck; // Đây là ImageView (để hiện dấu tích hoặc dấu 3 chấm)
        View layoutIconBg;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvTime = itemView.findViewById(R.id.tv_time);
            imgCheck = itemView.findViewById(R.id.img_check);
            layoutIconBg = itemView.findViewById(R.id.layout_icon_bg);
        }
    }
}