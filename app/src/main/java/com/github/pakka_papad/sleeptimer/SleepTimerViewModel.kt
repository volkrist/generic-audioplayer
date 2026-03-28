package com.github.pakka_papad.sleeptimer

import androidx.lifecycle.ViewModel
import com.github.pakka_papad.data.services.SleepTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SleepTimerViewModel @Inject constructor(
    val sleepTimerService: SleepTimerService,
) : ViewModel() {

    fun startMinutes(minutes: Int) = sleepTimerService.begin(minutes * 60)

    fun startStopAfterCurrentTrack() = sleepTimerService.beginStopAfterCurrentTrack()

    fun cancelTimer() = sleepTimerService.cancel()
}
