package com.citizenai.app.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * LanguageManager — central place for locale management.
 *
 * Strategy:
 *  - The selected language code is stored in BOTH DataStore (for reactive UI) AND a plain
 *    SharedPreferences file named [LANG_PREFS] (for reading in [attachBaseContext] before
 *    Hilt/DataStore is ready).
 *  - [applyLanguage] calls [AppCompatDelegate.setApplicationLocales] which handles
 *    Activity recreation on all API levels via the AndroidX AppCompat library.
 *  - [applyLocale] wraps a Context with the correct locale for [attachBaseContext].
 *
 * Supported codes: "en", "mr", "hi"
 */
object LanguageManager {

    /** SharedPreferences file name used as a fast locale bootstrap. */
    const val LANG_PREFS = "lang_bootstrap"

    /** Key inside [LANG_PREFS] for the saved language code. */
    const val LANG_KEY = "app_language"

    /** Codes that the app supports. */
    val SUPPORTED_LANGUAGES = listOf("en", "mr", "hi")

    /** Human-readable display names. These are intentionally NOT translated —
     *  they always appear in their own script so the user can recognise them. */
    val LANGUAGE_DISPLAY = mapOf(
        "en" to "English",
        "mr" to "मराठी",
        "hi" to "हिंदी"
    )

    /**
     * Apply [languageCode] app-wide immediately and trigger Activity recreation.
     * Also writes to [LANG_PREFS] so the code survives process death without DataStore.
     * Must be called on the Main thread.
     */
    fun applyLanguage(context: Context, languageCode: String) {
        // 1. Persist to the lightweight SharedPreferences bootstrap file.
        context.applicationContext
            .getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(LANG_KEY, languageCode)
            .apply()

        // 2. Set JVM default locale immediately.
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        // 3. Tell AppCompatDelegate — this triggers Activity recreation automatically.
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Read the bootstrapped language code synchronously.
     * Safe to call from [attachBaseContext] before Hilt/DataStore is initialized.
     */
    fun getSavedLanguageCode(context: Context): String =
        context.applicationContext
            .getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
            .getString(LANG_KEY, "en") ?: "en"

    /**
     * Wraps [base] with the [languageCode] locale.
     * Called from [MainActivity.attachBaseContext] for correct resource resolution.
     */
    fun applyLocale(base: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
            base.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            base.resources.updateConfiguration(config, base.resources.displayMetrics)
            base
        }
    }
}
