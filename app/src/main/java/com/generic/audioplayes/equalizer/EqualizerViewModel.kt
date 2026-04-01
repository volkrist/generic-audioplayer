package com.generic.audioplayes.equalizer

import androidx.lifecycle.ViewModel
import com.generic.audioplayes.data.UserPreferences
import com.generic.audioplayes.data.ZenPreferenceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val prefs: ZenPreferenceProvider,
    private val equalizerManager: EqualizerManager,
) : ViewModel() {

    val uiState = equalizerManager.uiState
    val equalizerSettings = prefs.equalizerSettings

    fun onPresetSelected(preset: UserPreferences.EqualizerPreset) {
        prefs.updateEqualizerPreset(preset)
    }

    fun onBandLevelChange(index: Int, mb: Int) {
        val state = equalizerManager.uiState.value
        val clamped = mb.coerceIn(state.levelMinMb, state.levelMaxMb)
        val newLevels = state.levelsMb.toMutableList()
        if (index in newLevels.indices) {
            newLevels[index] = clamped
            prefs.updateEqualizerCustomBands(newLevels)
        }
    }

    fun onBassChange(strength: Int) {
        prefs.updateBassBoostStrength(strength)
    }

    fun onVirtualizerChange(strength: Int) {
        prefs.updateVirtualizerStrength(strength)
    }

    fun reset() {
        prefs.resetEqualizerToDefaults()
    }

    fun onEqualizerMasterEnabled(enabled: Boolean) {
        prefs.updateEqualizerEnabled(enabled)
    }
}
