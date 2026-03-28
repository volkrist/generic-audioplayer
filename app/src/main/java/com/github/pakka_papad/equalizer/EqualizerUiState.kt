package com.github.pakka_papad.equalizer

data class EqualizerUiState(
    val bandCount: Int,
    val centerFreqHz: List<Float>,
    val levelsMb: List<Int>,
    val levelMinMb: Int,
    val levelMaxMb: Int,
    val bassStrength: Int,
    val virtualizerStrength: Int,
    val virtualizerSupported: Boolean,
    val effectsAttached: Boolean,
) {
    companion object {
        fun initial(): EqualizerUiState {
            val n = 5
            return EqualizerUiState(
                bandCount = n,
                centerFreqHz = EqualizerPresetHelper.defaultCenterFreqHz(n),
                levelsMb = List(n) { 0 },
                levelMinMb = -1500,
                levelMaxMb = 1500,
                bassStrength = 0,
                virtualizerStrength = 0,
                virtualizerSupported = false,
                effectsAttached = false,
            )
        }
    }
}
