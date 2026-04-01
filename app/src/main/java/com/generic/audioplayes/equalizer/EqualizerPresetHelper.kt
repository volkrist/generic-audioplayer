package com.generic.audioplayes.equalizer

import com.generic.audioplayes.data.UserPreferences
import kotlin.math.pow
import kotlin.math.roundToInt

internal object EqualizerPresetHelper {

    private val REF_NORMAL = intArrayOf(0, 0, 0, 0, 0)
    private val REF_BASS = intArrayOf(420, 280, 100, -40, -100)
    private val REF_ROCK = intArrayOf(300, 140, -100, 120, 300)
    private val REF_POP = intArrayOf(-80, 20, 200, 160, 120)
    private val REF_CLASSICAL = intArrayOf(-100, -50, 0, 100, 200)

    fun millibelsForPreset(
        preset: UserPreferences.EqualizerPreset,
        bandCount: Int,
    ): IntArray {
        val ref = when (preset) {
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL,
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM,
            -> REF_NORMAL
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_BASS -> REF_BASS
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_ROCK -> REF_ROCK
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_POP -> REF_POP
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CLASSICAL -> REF_CLASSICAL
            UserPreferences.EqualizerPreset.UNRECOGNIZED -> REF_NORMAL
        }
        return expandToBands(ref, bandCount)
    }

    fun expandToBands(ref: IntArray, bandCount: Int): IntArray {
        if (bandCount <= 0) return intArrayOf()
        if (ref.isEmpty()) return IntArray(bandCount) { 0 }
        if (bandCount == 1) return intArrayOf(ref[ref.size / 2])
        return IntArray(bandCount) { i ->
            val pos = i.toFloat() / (bandCount - 1) * (ref.size - 1)
            val lo = pos.toInt().coerceIn(0, ref.size - 2)
            val hi = (lo + 1).coerceAtMost(ref.size - 1)
            val frac = pos - lo
            (ref[lo] * (1 - frac) + ref[hi] * frac).roundToInt()
        }
    }

    fun resizeBands(levels: List<Int>, newCount: Int): IntArray {
        if (newCount <= 0) return intArrayOf()
        if (levels.isEmpty()) return IntArray(newCount) { 0 }
        if (levels.size == newCount) return levels.toIntArray()
        val ref = levels.toIntArray()
        return IntArray(newCount) { i ->
            val pos = i.toFloat() / (newCount - 1) * (ref.size - 1)
            val lo = pos.toInt().coerceIn(0, ref.size - 2)
            val hi = (lo + 1).coerceAtMost(ref.size - 1)
            val frac = pos - lo
            (ref[lo] * (1 - frac) + ref[hi] * frac).roundToInt()
        }
    }

    fun defaultCenterFreqHz(bandCount: Int): List<Float> {
        if (bandCount <= 0) return emptyList()
        if (bandCount == 1) return listOf(1000f)
        val minHz = 60f
        val maxHz = 14000f
        return List(bandCount) { i ->
            val t = i.toFloat() / (bandCount - 1)
            minHz * (maxHz / minHz).pow(t)
        }
    }

    fun computeLevels(
        settings: com.generic.audioplayes.data.EqualizerSettings,
        bandCount: Int,
    ): IntArray {
        return when (settings.preset) {
            UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM -> {
                if (settings.customBandsMb.isEmpty()) {
                    millibelsForPreset(
                        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL,
                        bandCount,
                    )
                } else {
                    resizeBands(settings.customBandsMb, bandCount)
                }
            }
            UserPreferences.EqualizerPreset.UNRECOGNIZED ->
                millibelsForPreset(UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL, bandCount)
            else -> millibelsForPreset(settings.preset, bandCount)
        }
    }
}
