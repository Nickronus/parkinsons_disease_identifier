package com.parkinsons_disease_identifier;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREFS_NAME = "SettingsPrefs";
    private static final String KEY_LANGUAGE = "language_en";
    /** По умолчанию английский выключен (используется русский). */
    private static final boolean DEFAULT_LANGUAGE_EN = false;

    public static Context applyLocale(Context context) {
        return updateResources(context, getLanguage(context));
    }

    private static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LANGUAGE, DEFAULT_LANGUAGE_EN) ? "en" : "ru";
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
            context = context.createConfigurationContext(config);
        } else {
            config.locale = locale;
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }
}
