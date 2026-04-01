package com.generic.audioplayes.equalizer

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.data.EqualizerSettings
import com.generic.audioplayes.data.ZenCrashReporter
import com.generic.audioplayes.data.ZenPreferenceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerManager @Inject constructor(
    private val exoPlayer: ExoPlayer,
    private val prefs: ZenPreferenceProvider,
    private val crashReporter: ZenCrashReporter,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _uiState = MutableStateFlow(EqualizerUiState.initial())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            mainHandler.post { attachEffects(audioSessionId) }
        }
    }

    init {
        mainHandler.post {
            exoPlayer.addListener(playerListener)
            attachEffects(exoPlayer.audioSessionId)
        }
        scope.launch {
            prefs.equalizerSettings.collectLatest { settings ->
                withContext(Dispatchers.Main) {
                    applyStoredSettings(settings)
                }
            }
        }
    }

    private fun attachEffects(sessionId: Int) {
        releaseEffects()
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) {
            publishDisconnected()
            return
        }
        try {
            val eq = Equalizer(0, sessionId).apply { enabled = true }
            val bass = BassBoost(0, sessionId).apply { enabled = true }
            equalizer = eq
            bassBoost = bass
            var virt: Virtualizer? = null
            try {
                val v = Virtualizer(0, sessionId)
                if (v.strengthSupported) {
                    v.enabled = true
                    virt = v
                } else {
                    v.release()
                }
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            virtualizer = virt
            applyStoredSettings(prefs.equalizerSettings.value)
        } catch (e: Exception) {
            crashReporter.logException(e)
            publishDisconnected()
        }
    }

    private fun releaseEffects() {
        virtualizer?.release()
        virtualizer = null
        bassBoost?.release()
        bassBoost = null
        equalizer?.release()
        equalizer = null
    }

    private fun applyStoredSettings(settings: EqualizerSettings) {
        val eq = equalizer
        val bass = bassBoost
        val virt = virtualizer
        val bypass = !settings.enabled
        try {
            if (bypass) {
                eq?.enabled = false
                bass?.enabled = false
                virt?.enabled = false
            } else {
                eq?.enabled = true
                bass?.enabled = true
                virt?.enabled = virt?.strengthSupported == true
            }
            if (!bypass && eq != null) {
                val n = eq.numberOfBands.toInt()
                val levels = EqualizerPresetHelper.computeLevels(settings, n)
                val range = eq.bandLevelRange
                val minL = range[0].toInt()
                val maxL = range[1].toInt()
                for (i in 0 until n) {
                    val lvl = levels.getOrElse(i) { 0 }.coerceIn(minL, maxL).toShort()
                    eq.setBandLevel(i.toShort(), lvl)
                }
            }
            if (!bypass) {
                bass?.let { b ->
                    if (b.strengthSupported) {
                        b.setStrength(settings.bassStrength.coerceIn(0, 1000).toShort())
                    }
                }
                virt?.let { v ->
                    if (v.strengthSupported) {
                        v.setStrength(settings.virtualizerStrength.coerceIn(0, 1000).toShort())
                    }
                }
            }
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
        publishUiState(settings)
    }

    private fun publishUiState(settings: EqualizerSettings) {
        val eq = equalizer
        if (eq != null) {
            val n = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minL = range[0].toInt()
            val maxL = range[1].toInt()
            val levels = (0 until n).map { eq.getBandLevel(it.toShort()).toInt() }
            val freqs = (0 until n).map { eq.getCenterFreq(it.toShort()) / 1000f }
            _uiState.value = EqualizerUiState(
                equalizerEnabled = settings.enabled,
                bandCount = n,
                centerFreqHz = freqs,
                levelsMb = levels,
                levelMinMb = minL,
                levelMaxMb = maxL,
                bassStrength = settings.bassStrength,
                virtualizerStrength = settings.virtualizerStrength,
                virtualizerSupported = virtualizer != null,
                effectsAttached = true,
            )
        } else {
            publishDisconnected(settings)
        }
    }

    private fun publishDisconnected(settings: EqualizerSettings = prefs.equalizerSettings.value) {
        val n = 5
        val levels = EqualizerPresetHelper.computeLevels(settings, n).toList()
        _uiState.value = EqualizerUiState(
            equalizerEnabled = settings.enabled,
            bandCount = n,
            centerFreqHz = EqualizerPresetHelper.defaultCenterFreqHz(n),
            levelsMb = levels,
            levelMinMb = -1500,
            levelMaxMb = 1500,
            bassStrength = settings.bassStrength,
            virtualizerStrength = settings.virtualizerStrength,
            virtualizerSupported = false,
            effectsAttached = false,
        )
    }
}
