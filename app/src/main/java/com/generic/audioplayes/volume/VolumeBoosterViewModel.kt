package com.generic.audioplayes.volume

import androidx.lifecycle.ViewModel
import com.generic.audioplayes.data.ZenPreferenceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class VolumeBoosterViewModel @Inject constructor(
    private val prefs: ZenPreferenceProvider,
) : ViewModel() {

    val percent = prefs.volumeBoosterPercent

    fun setPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        prefs.updateVolumeBoosterPercent(clamped)
    }

    fun reset() = setPercent(100)
}
