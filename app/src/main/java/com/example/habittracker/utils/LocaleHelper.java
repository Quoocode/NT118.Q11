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
        // Ensure the app also updates the application resources too, not only a returned context.
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

        // Also update the base resources so components still using the original context (e.g. some ViewModels / singletons) see the right locale.
        context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());

        return localizedContext;
    }

    private static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    private static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "en"); // default English
    }
}