package com.sylo.com

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sylo.com.navigation.SyloNavHost
import com.sylo.core.common.locale.LocaleHelper
import com.sylo.core.database.UserPreferencesRepository
import com.sylo.core.ui.theme.SyloTheme
import com.sylo.core.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// FragmentActivity (not ComponentActivity) so androidx BiometricPrompt can attach.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var userPreferences: UserPreferencesRepository

    // Apply the user's chosen app language before any resources are resolved. wrap() covers
    // base-context lookups, but the Activity resolves its own resources (the strings Compose
    // reads via stringResource) against its own configuration, so the locale must also be
    // pushed there via applyToActivity — otherwise the UI flips to RTL but text stays English.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
        LocaleHelper.applyToActivity(this, newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Resolve the user's theme preference (System/Light/Dark) reactively so a
            // change in Settings recolors the whole app without a restart.
            val themeKey by userPreferences.themeMode
                .collectAsStateWithLifecycle(initialValue = UserPreferencesRepository.DEFAULT_THEME_MODE)
            val darkTheme = ThemeMode.fromKeyOrDefault(themeKey).resolveDark()

            SyloTheme(darkTheme = darkTheme) {
                // Root Surface paints the theme background behind every screen —
                // including the auth flow and first-run gates, which render outside the
                // main Scaffold and would otherwise fall through to the window default.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    SyloNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
