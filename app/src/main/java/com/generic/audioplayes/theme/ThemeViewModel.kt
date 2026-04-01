package com.generic.audioplayes.theme

import androidx.lifecycle.ViewModel
import com.generic.audioplayes.ui.theme.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeManager: ThemeManager,
) : ViewModel() {

    val themePreference = themeManager.themePreference

    fun updateTheme(theme: ThemePreference) {
        themeManager.updateTheme(theme)
    }
}
