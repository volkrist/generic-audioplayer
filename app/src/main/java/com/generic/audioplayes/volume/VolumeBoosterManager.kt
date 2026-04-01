package com.generic.audioplayes.volume

import android.media.audiofx.LoudnessEnhancer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.data.ZenCrashReporter
import com.generic.audioplayes.data.ZenPreferenceProvider
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
 * Real volume boost via [LoudnessEnhancer] on the player [audioSessionId].
 * [ExoPlayer.setVolume] stays at 1f; gain is 0–600 mB (100% → 0, 200% → 600 mB).
 */
@Singleton
class VolumeBoosterManager @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val prefs: ZenPreferenceProvider,
    private val crashReporter: ZenCrashReporter,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loudness: LoudnessEnhancer? = null
    private var loudnessAvailable = false

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            mainHandler.post { attachLoudness(audioSessionId) }
        }
    }

    init {
        mainHandler.post {
            exoPlayer.addListener(playerListener)
            attachLoudness(exoPlayer.audioSessionId)
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
     * Call from main thread when the player service is shutting down; releases the audio effect.
     */
    fun releaseLoudnessEffect() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            loudness?.release()
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
        loudness = null
        loudnessAvailable = false
        exoPlayer.volume = 1f
    }

    private fun attachLoudness(sessionId: Int) {
        try {
            loudness?.release()
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
        loudness = null
        loudnessAvailable = false
        exoPlayer.volume = 1f

        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) {
            applyPercent(prefs.volumeBoosterPercent.value)
            return
        }
        try {
            val l = LoudnessEnhancer(sessionId)
            l.enabled = true
            loudness = l
            loudnessAvailable = true
            applyPercent(prefs.volumeBoosterPercent.value)
            Timber.d("VolumeBoosterManager: LoudnessEnhancer attached sessionId=$sessionId")
        } catch (e: Exception) {
            crashReporter.logException(e)
            Timber.w(e, "VolumeBoosterManager: LoudnessEnhancer unavailable, using fallback")
            applyPercent(prefs.volumeBoosterPercent.value)
        }
    }

    private fun applyPercent(percent: Int) {
        val p = percent.coerceIn(100, 200)
        exoPlayer.volume = 1f

        val l = loudness
        if (p <= 100) {
            try {
                l?.setTargetGain(0)
                l?.enabled = false
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            return
        }

        val gainMb = percentToLinearGainMb(p)
        if (!loudnessAvailable || l == null) {
            fallbackWithoutLoudness(p)
            return
        }
        try {
            l.enabled = true
            l.setTargetGain(gainMb)
            Timber.d("VolumeBoosterManager: apply percent=$p gainMb=$gainMb")
        } catch (e: Exception) {
            crashReporter.logException(e)
            fallbackWithoutLoudness(p)
        }
    }

    /** 100% → 0 mB, 150% → 300 mB, 200% → 600 mB */
    private fun percentToLinearGainMb(percent: Int): Int {
        val p = percent.coerceIn(100, 200)
        return ((p - 100) * 600 / 100).coerceIn(0, 600)
    }

    /** Last resort if LoudnessEnhancer cannot be applied on this device. */
    private fun fallbackWithoutLoudness(percent: Int) {
        val p = percent.coerceIn(100, 200)
        if (p <= 100) return
        exoPlayer.volume = (p / 200f).coerceIn(0.5f, 1f)
        Timber.d("VolumeBoosterManager: fallback exoPlayer.volume=${exoPlayer.volume}")
    }
}
