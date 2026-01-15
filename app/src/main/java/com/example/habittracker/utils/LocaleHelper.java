package com.example.habittracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_NAME = "language_pref";
    private static final String KEY_LANG = "app_language";

    public static void setLocale(Context context, String lang) {
        saveLanguage(context, lang);
        // Đảm bảo app cũng cập nhật resource ở cấp Application, không chỉ Context trả về.
        updateResources(context.getApplicationContext(), lang);
    }

    public static Context applyLocale(Context context) {
        String lang = getSavedLanguage(context);
        return updateResources(context, lang);
    }

    private static Context updateResources(Context context, String lang) {
        if (lang == null || lang.trim().isEmpty()) lang = "en";

        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        Context localizedContext = context.createConfigurationContext(config);

        // Đồng thời cập nhật resource gốc để các thành phần vẫn dùng context ban đầu
        // (vd: một số ViewModel/singleton) cũng nhận đúng locale.
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

        return localizedContext;
    }

    private static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    private static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "en"); // mặc định tiếng Anh
    }
}