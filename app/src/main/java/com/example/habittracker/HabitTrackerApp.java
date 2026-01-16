package com.example.habittracker;
import android.app.Application;
import com.example.habittracker.utils.ThemeHelper;
import android.app.Application;
import android.content.Context;
import com.example.habittracker.utils.LocaleHelper;

/**
 * Lớp Application nhằm đảm bảo locale (ngôn ngữ) đã lưu được áp dụng khi khởi động lạnh.
 * Coldstart là khi ứng dụng được mở lại sau khi bị hệ thống kill để giải phóng bộ nhớ.
 *
 * Việc này giúp tránh tình trạng các chuỗi (vd: thành tích) bị quay về tiếng Anh sau khi ứng dụng
 * bị hệ thống kill và mở lại.
 */
public class HabitTrackerApp extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Ép áp dụng locale cho cả tài nguyên cấp ứng dụng (bao phủ các thành phần dùng app context/resources).
        LocaleHelper.applyLocale(this);

        // Lấy trạng thái Dark Mode đã lưu và áp dụng ngay lập tức
        boolean isDark = ThemeHelper.isDarkMode(this);
        ThemeHelper.applyTheme(isDark);
    }
}
