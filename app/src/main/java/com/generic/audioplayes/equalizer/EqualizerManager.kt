package com.generic.audioplayes.equalizer

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.data.EqualizerSettings
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
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
    private val prefs: AudioPlayerPreferenceProvider,
    private val crashReporter: AudioPlayerCrashReporter,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

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
            var reverb: PresetReverb? = null
            try {
                val r = PresetReverb(0, sessionId)
                r.enabled = true
                reverb = r
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            presetReverb = reverb
            applyStoredSettings(prefs.equalizerSettings.value)
        } catch (e: Exception) {
            crashReporter.logException(e)
            publishDisconnected()
        }
    }

    private fun releaseEffects() {
        presetReverb?.release()
        presetReverb = null
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
        val reverb = presetReverb
        val bypass = !settings.enabled
        try {
            if (bypass) {
                eq?.enabled = false
                bass?.enabled = false
                virt?.enabled = false
                reverb?.enabled = false
            } else {
                eq?.enabled = true
                bass?.enabled = true
                virt?.enabled = virt?.strengthSupported == true
                if (reverb != null) {
                    reverb.enabled = true
                    try {
                        reverb.preset = settings.reverbPreset.coerceIn(0, 6).toShort()
                    } catch (e: Exception) {
                        crashReporter.logException(e)
                    }
                }
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
        val displayBands = settings.uiBandCount.coerceIn(5, 10)
        val levels = EqualizerPresetHelper.computeLevels(settings, displayBands).toList()
        val freqs = EqualizerPresetHelper.displayCenterFreqHz(displayBands)
        val minL: Int
        val maxL: Int
        if (eq != null) {
            val range = eq.bandLevelRange
            minL = range[0].toInt()
            maxL = range[1].toInt()
        } else {
            minL = -1500
            maxL = 1500
        }
        _uiState.value = EqualizerUiState(
            equalizerEnabled = settings.enabled,
            uiBandCount = displayBands,
            centerFreqHz = freqs,
            levelsMb = levels,
            levelMinMb = minL,
            levelMaxMb = maxL,
            bassStrength = settings.bassStrength,
            virtualizerStrength = settings.virtualizerStrength,
            virtualizerSupported = virtualizer != null,
            effectsAttached = eq != null,
            reverbPreset = settings.reverbPreset,
            reverbSupported = presetReverb != null || eq == null,
        )
    }

    private fun publishDisconnected(settings: EqualizerSettings = prefs.equalizerSettings.value) {
        val displayBands = settings.uiBandCount.coerceIn(5, 10)
        val levels = EqualizerPresetHelper.computeLevels(settings, displayBands).toList()
        _uiState.value = EqualizerUiState(
            equalizerEnabled = settings.enabled,
            uiBandCount = displayBands,
            centerFreqHz = EqualizerPresetHelper.displayCenterFreqHz(displayBands),
            levelsMb = levels,
            levelMinMb = -1500,
            levelMaxMb = 1500,
            bassStrength = settings.bassStrength,
            virtualizerStrength = settings.virtualizerStrength,
            virtualizerSupported = false,
            effectsAttached = false,
            reverbPreset = settings.reverbPreset,
            reverbSupported = true,
        )
    }
}
