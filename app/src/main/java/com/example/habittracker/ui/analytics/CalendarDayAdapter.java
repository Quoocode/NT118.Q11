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
    private Date selectedDate;

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

    public void setSelectedDate(Date date) {
        this.selectedDate = date;
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
        boolean isToday = isSameDay(calendarDay.getDate(), today);
        boolean isSelected = isSameDay(calendarDay.getDate(), selectedDate);
        holder.bind(calendarDay, isToday, isSelected);
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
        private final View dayContainer;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayNumber = itemView.findViewById(R.id.day_number);
            dayContainer = itemView.findViewById(R.id.day_container);
        }

        void bind(CalendarDay calendarDay, boolean isToday, boolean isSelected) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(calendarDay.getDate());
            dayNumber.setText(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));

            // - default: dark background
            // - selected: blue background
            // - today: border only
            // - today + selected: blue background + border
            if (isToday && isSelected) {
                dayContainer.setBackgroundResource(R.drawable.calendar_day_selected_today);
            } else if (isSelected) {
                dayContainer.setBackgroundResource(R.drawable.container_blue);
            } else if (isToday) {
                dayContainer.setBackgroundResource(R.drawable.calendar_day_today_border);
            } else {
                // use default style
                dayContainer.setBackgroundResource(R.drawable.calendar_day_default);
            }

            float alpha = calendarDay.isInCurrentMonth() ? 1f : 0.3f;
            dayContainer.setAlpha(alpha);
            dayNumber.setAlpha(1f);
        }
    }

    private boolean isSameDay(Date first, Date second) {
        if (first == null || second == null) {
            return false;
        }
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(first);
        cal2.setTime(second);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }
}
