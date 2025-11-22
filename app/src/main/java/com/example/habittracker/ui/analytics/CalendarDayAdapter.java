package com.example.habittracker.ui.analytics;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habittracker.R;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.DayViewHolder> {

    public interface Listener {
        void onDaySelected(Date date);
    }

    private final List<CalendarDay> days = new ArrayList<>();
    private final Listener listener;
    private Date today = new Date();

    public CalendarDayAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitDays(List<CalendarDay> calendarDays) {
        days.clear();
        days.addAll(calendarDays);
        notifyDataSetChanged();
    }

    public void setToday(Date date) {
        this.today = date;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        CalendarDay calendarDay = days.get(position);
        holder.bind(calendarDay, calendarDay.getDate().equals(today));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDaySelected(calendarDay.getDate());
            }
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class DayViewHolder extends RecyclerView.ViewHolder {
        private final TextView dayNumber;
        private final View todayIndicator;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.day_number);
            todayIndicator = itemView.findViewById(R.id.today_indicator);
        }

        void bind(CalendarDay calendarDay, boolean isToday) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(calendarDay.getDate());
            dayNumber.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            todayIndicator.setVisibility(isToday ? View.VISIBLE : View.GONE);
            float alpha = calendarDay.isInCurrentMonth() ? 1f : 0.3f;
            dayNumber.setAlpha(alpha);
            todayIndicator.setAlpha(alpha);
        }
    }
}
