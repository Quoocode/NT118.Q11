package com.example.habittracker.ui.analytics;

import java.util.Date;

public class CalendarDay {
    private final Date date;
    private final boolean inCurrentMonth;

    public CalendarDay(Date date, boolean inCurrentMonth) {
        this.date = date;
        this.inCurrentMonth = inCurrentMonth;
    }

    public Date getDate() {
        return date;
    }

    public boolean isInCurrentMonth() {
        return inCurrentMonth;
    }
}
