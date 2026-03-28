package com.github.pakka_papad.theme

import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.ui.theme.ThemePreference
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin facade over [ZenPreferenceProvider] theme state for UI layers (e.g. [ThemeViewModel]).
 */
@Singleton
class ThemeManager @Inject constructor(
    private val preferences: ZenPreferenceProvider,
) {
    val themePreference: StateFlow<ThemePreference> = preferences.theme

    fun updateTheme(theme: ThemePreference) {
        preferences.updateTheme(theme)
    }
}
