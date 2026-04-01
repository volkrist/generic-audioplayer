package com.generic.audioplayes.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.generic.audioplayes.data.music.ScanStatus
import com.generic.audioplayes.data.music.SongExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OnBoardingViewModel @Inject constructor(
    private val songExtractor: SongExtractor,
) : ViewModel() {

    val scanStatus = songExtractor.scanStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300,
                replayExpirationMillis = 0
            ),
            initialValue = ScanStatus.ScanNotRunning
        )

    fun scanForMusic() {
        songExtractor.scanForMusic()
    }

}