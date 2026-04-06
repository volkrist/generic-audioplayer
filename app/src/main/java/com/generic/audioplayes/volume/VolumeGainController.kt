package com.generic.audioplayes.volume

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Linear gain applied in [VolumePcmGainAudioProcessor] (1.0 = 100%, 2.0 = 200% amplitude).
 * Thread-safe for reads from the audio thread and writes from main/coroutines.
 */
@Singleton
class VolumeGainController @Inject constructor() {

    @Volatile
    var linearGain: Float = 1f
        private set

    fun setFromPercent(percent: Int) {
        val p = percent.coerceIn(100, 200)
        linearGain = p / 100f
    }

    fun resetToUnity() {
        linearGain = 1f
    }
}
