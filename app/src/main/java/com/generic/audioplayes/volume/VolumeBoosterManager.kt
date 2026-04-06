package com.generic.audioplayes.volume

import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies volume boost up to 200% via [VolumePcmGainAudioProcessor] (linear PCM amplitude ×2).
 * [ExoPlayer.setVolume] stays at 1f; actual gain is in [VolumeGainController.linearGain].
 */
@Singleton
class VolumeBoosterManager @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val prefs: AudioPlayerPreferenceProvider,
    private val gainController: VolumeGainController,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        mainHandler.post {
            gainController.setFromPercent(prefs.volumeBoosterPercent.value)
        }
        scope.launch {
            prefs.volumeBoosterPercent.collectLatest { percent ->
                withContext(Dispatchers.Main) {
                    applyPercent(percent)
                }
            }
        }
    }

    /**
     * Call from main thread when the player service is shutting down.
     */
    fun releaseLoudnessEffect() {
        mainHandler.removeCallbacksAndMessages(null)
        exoPlayer.volume = 1f
        gainController.resetToUnity()
    }

    private fun applyPercent(percent: Int) {
        val p = percent.coerceIn(100, 200)
        exoPlayer.volume = 1f
        gainController.setFromPercent(p)
        Timber.d("VolumeBoosterManager: linearGain=${gainController.linearGain} percent=$p")
    }
}
