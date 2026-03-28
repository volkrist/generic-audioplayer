package com.github.pakka_papad.volume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.ExoPlayer
import com.github.pakka_papad.data.ZenPreferenceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class VolumeBoosterViewModel @Inject constructor(
    private val prefs: ZenPreferenceProvider,
    private val exoPlayer: ExoPlayer,
) : ViewModel() {

    val percent = prefs.volumeBoosterPercent

    init {
        viewModelScope.launch {
            val p = prefs.volumeBoosterPercent.first()
            withContext(Dispatchers.Main) {
                exoPlayer.volume = p / 100f
            }
        }
    }

    fun setPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        prefs.updateVolumeBoosterPercent(clamped)
        viewModelScope.launch(Dispatchers.Main) {
            exoPlayer.volume = clamped / 100f
        }
    }

    fun reset() = setPercent(100)
}
