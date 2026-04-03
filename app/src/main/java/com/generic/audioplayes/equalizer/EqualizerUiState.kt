package com.generic.audioplayes.equalizer

data class EqualizerUiState(
    val equalizerEnabled: Boolean,
    /** 5 or 10 — number of vertical sliders shown */
    val uiBandCount: Int,
    val centerFreqHz: List<Float>,
    /** Display / logical band levels in millibels (size = uiBandCount) */
    val levelsMb: List<Int>,
    val levelMinMb: Int,
    val levelMaxMb: Int,
    val bassStrength: Int,
    val virtualizerStrength: Int,
    val virtualizerSupported: Boolean,
    val effectsAttached: Boolean,
    val reverbPreset: Int,
    val reverbSupported: Boolean,
) {
    companion object {
        fun initial(): EqualizerUiState {
            val n = 5
            return EqualizerUiState(
                equalizerEnabled = true,
                uiBandCount = n,
                centerFreqHz = EqualizerPresetHelper.displayCenterFreqHz(n),
                levelsMb = List(n) { 0 },
                levelMinMb = -1500,
                levelMaxMb = 1500,
                bassStrength = 0,
                virtualizerStrength = 0,
                virtualizerSupported = false,
                effectsAttached = false,
                reverbPreset = 0,
                reverbSupported = true,
            )
        }
    }
}
