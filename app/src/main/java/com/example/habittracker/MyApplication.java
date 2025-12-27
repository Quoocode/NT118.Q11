package com.example.habittracker;

import android.app.Application;
import com.example.habittracker.utils.ThemeHelper;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Lấy trạng thái Dark Mode đã lưu và áp dụng ngay lập tức
        boolean isDark = ThemeHelper.isDarkMode(this);
        ThemeHelper.applyTheme(isDark);
    }
}