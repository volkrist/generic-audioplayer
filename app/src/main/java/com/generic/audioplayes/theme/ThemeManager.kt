package com.generic.audioplayes.theme

import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.ThemePreference
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin facade over [AudioPlayerPreferenceProvider] theme state for UI layers (e.g. [ThemeViewModel]).
 */
@Singleton
class ThemeManager @Inject constructor(
    private val preferences: AudioPlayerPreferenceProvider,
) {
    val themePreference: StateFlow<ThemePreference> = preferences.theme

    fun updateTheme(theme: ThemePreference) {
        preferences.updateTheme(theme)
    }
}
