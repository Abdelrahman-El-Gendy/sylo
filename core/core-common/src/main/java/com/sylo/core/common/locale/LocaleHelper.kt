package com.sylo.core.common.locale

import android.app.Activity
import android.content.Context
import android.content.Intent
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

    /**
     * Applies the persisted language to an [Activity]'s own configuration.
     *
     * [wrap] only re-bases the *base* context, but an Activity resolves its resources —
     * including the strings Compose reads through `stringResource()` — against its **own**
     * configuration, which the framework keeps pinned to the system locale. Wrapping alone
     * therefore left the UI flipped to RTL (from [Locale.setDefault], which drives layout
     * direction) while every string stayed in the default language. [applyOverrideConfiguration]
     * pushes the locale onto the Activity's resources so text is localized too.
     *
     * Passing a bare [Configuration] with only the locale set means every other field
     * (density, screen size, …) is left "undefined" and inherited from the base config, so
     * this overrides the locale and nothing else. Must be called from
     * [Activity.attachBaseContext] (before `onCreate`); a no-op for "system".
     */
    fun applyToActivity(activity: Activity, base: Context) {
        val tag = getLanguageTag(base)
        if (tag.isEmpty()) return
        val locale = Locale.forLanguageTag(tag)
        val override = Configuration().apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        activity.applyOverrideConfiguration(override)
    }

    /**
     * Relaunches the app's task so the new language takes effect app-wide.
     *
     * [FLAG_ACTIVITY_CLEAR_TASK] finishes every activity and starts a *fresh* root
     * activity, whose [android.app.Activity.attachBaseContext] re-runs [wrap] with the
     * newly-persisted tag — so a plain `recreate()` (which some OEM ROMs, e.g. ColorOS,
     * don't reliably re-locale) isn't relied on.
     *
     * The process is deliberately NOT killed. An earlier version called
     * `Runtime.getRuntime().exit(0)` right after [Context.startActivity]; because the
     * relaunched activity is created in the still-alive process, the exit raced with it
     * and tore the new activity down, leaving a blank white window. Keeping the process
     * alive relaunches instantly with no flash. (Application-context string lookups keep
     * the previous language until the next cold start — an acceptable trade-off versus a
     * broken switch; the visible UI, which reads through the Activity, is correct.)
     */
    fun restartApp(context: Context) {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            ?: return
        context.startActivity(intent)
    }
}
