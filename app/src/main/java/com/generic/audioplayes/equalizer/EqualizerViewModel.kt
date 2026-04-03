package com.generic.audioplayes.equalizer

import androidx.lifecycle.ViewModel
import com.generic.audioplayes.data.UserPreferences
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val prefs: AudioPlayerPreferenceProvider,
    private val equalizerManager: EqualizerManager,
) : ViewModel() {

    val uiState = equalizerManager.uiState
    val equalizerSettings = prefs.equalizerSettings

    fun onPresetSelected(preset: UserPreferences.EqualizerPreset) {
        prefs.updateEqualizerPreset(preset)
    }

    fun onBandLevelChange(index: Int, mb: Int) {
        val settings = prefs.equalizerSettings.value
        val ui = equalizerManager.uiState.value
        val count = settings.uiBandCount.coerceIn(5, 10)
        val levels = EqualizerPresetHelper.computeLevels(settings, count).toMutableList()
        if (index !in levels.indices) return
        val clamped = mb.coerceIn(ui.levelMinMb, ui.levelMaxMb)
        levels[index] = clamped
        prefs.updateEqualizerCustomBands(levels)
    }

    fun onBassChange(strength: Int) {
        prefs.updateBassBoostStrength(strength)
    }

    fun onVirtualizerChange(strength: Int) {
        prefs.updateVirtualizerStrength(strength)
    }

    fun onReverbPreset(presetId: Int) {
        prefs.updateReverbPreset(presetId)
    }

    fun onUiBandCountChange(count: Int) {
        prefs.updateEqualizerUiBandCount(count)
    }

    fun onEqualizerMasterEnabled(enabled: Boolean) {
        prefs.updateEqualizerEnabled(enabled)
    }
}
