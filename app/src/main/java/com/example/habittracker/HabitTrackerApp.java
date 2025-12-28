package com.example.habittracker;
import android.app.Application;
import com.example.habittracker.utils.ThemeHelper;
import android.app.Application;
import android.content.Context;
import com.example.habittracker.utils.LocaleHelper;

/**
 * Application class to ensure saved locale is applied on cold start.
 *
 * This prevents strings (e.g., achievements) from reverting to English after the app is killed
 * and opened again.
 */
public class HabitTrackerApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Force-apply to application resources too (covers components using app context/resources).
        LocaleHelper.applyLocale(this);

        // Lấy trạng thái Dark Mode đã lưu và áp dụng ngay lập tức
        boolean isDark = ThemeHelper.isDarkMode(this);
        ThemeHelper.applyTheme(isDark);
    }
}

