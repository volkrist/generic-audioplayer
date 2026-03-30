package com.github.pakka_papad.sleeptimer

import androidx.lifecycle.ViewModel
import com.github.pakka_papad.data.services.SleepTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    val sleepTimerService: SleepTimerService,
) : ViewModel() {

    init {
        sleepTimerService.restoreIfNeeded()
    }

    fun startMinutes(minutes: Int) = sleepTimerService.begin(minutes.coerceIn(1, 24 * 60) * 60)

    fun startCustomTotalSeconds(totalSeconds: Int) =
        sleepTimerService.begin(totalSeconds.coerceIn(1, 24 * 60 * 60))

    fun startStopAfterCurrentTrack() = sleepTimerService.beginStopAfterCurrentTrack()

    fun cancelTimer() = sleepTimerService.cancel()
}
