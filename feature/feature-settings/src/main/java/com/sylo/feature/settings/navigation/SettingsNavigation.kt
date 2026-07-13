package com.sylo.feature.settings.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sylo.core.navigation.SettingsRoute
import com.sylo.feature.settings.SettingsRoute as SettingsScreenRoute

fun EntryProviderScope<NavKey>.settingsEntry(
    onChangePin: () -> Unit,
    onLoggedOut: () -> Unit,
) {
    entry<SettingsRoute> {
        SettingsScreenRoute(onChangePin = onChangePin, onLoggedOut = onLoggedOut)
    }
}
