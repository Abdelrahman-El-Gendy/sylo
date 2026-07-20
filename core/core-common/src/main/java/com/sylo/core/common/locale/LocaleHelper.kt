package com.sylo.core.common.locale

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Per-app language switching that works on every supported API level (28+) without
 * AppCompat. The selected BCP-47 tag is kept in a tiny synchronous SharedPreferences
 * (so it can be read in [android.app.Activity.attachBaseContext], which runs before
 * any async store is available) and applied by wrapping the base context in a
 * configuration that overrides the locale + layout direction (RTL for Arabic).
 *
 * "" (empty) means "follow the system language". Lives in :core-common (rather than
 * :app) so both the Activity/Application (attachBaseContext) and the Settings feature
 * (the language picker) can read/write the same preference without a reverse
 * feature -> app dependency.
 */
object LocaleHelper {

    private const val PREFS = "sylo_locale"
    private const val KEY_LANGUAGE = "app_language"

    /**
     * Persisted language tag: "" (system), "en", or "ar".
     *
     * Deliberately reads via [context] directly, NOT `context.applicationContext`:
     * inside [android.app.Application.attachBaseContext], `getApplicationContext()`
     * is still null (the framework only wires it up after attachBaseContext returns),
     * so using it here crashed the app at startup with a NullPointerException. Any
     * Context — including a raw base context — can call getSharedPreferences directly.
     */
    fun getLanguageTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, "")
            .orEmpty()

    fun setLanguageTag(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, tag)
            .apply()
    }

    /** Returns [context] re-based on the persisted language, or unchanged for "system". */
    fun wrap(context: Context): Context {
        val tag = getLanguageTag(context)
        if (tag.isEmpty()) return context
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
