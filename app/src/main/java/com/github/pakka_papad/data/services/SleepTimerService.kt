package com.github.pakka_papad.data.services

import android.app.PendingIntent
import androidx.datastore.core.DataStore
import com.github.pakka_papad.data.UserPreferences
import com.github.pakka_papad.data.copy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.annotations.VisibleForTesting

interface SleepTimerService {
    val isRunning: StateFlow<Boolean>
    val timeLeft: StateFlow<Int>
    val isStopAfterCurrentTrack: StateFlow<Boolean>

    fun cancel()
    fun begin(durationSeconds: Int)
    fun beginStopAfterCurrentTrack()

    fun consumeStopAfterCurrentTrack(): Boolean

    fun restoreIfNeeded()
}

class SleepTimerServiceImpl(
    private val scope: CoroutineScope,
    private val closeIntent: PendingIntent,
    private val userPreferences: DataStore<UserPreferences>,
) : SleepTimerService {

    private var timerJob: Job? = null

    @VisibleForTesting
    internal val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    @VisibleForTesting
    internal val _timeLeft = MutableStateFlow(0)
    override val timeLeft: StateFlow<Int> = _timeLeft.asStateFlow()

    private val _isStopAfterCurrentTrack = MutableStateFlow(false)
    override val isStopAfterCurrentTrack: StateFlow<Boolean> = _isStopAfterCurrentTrack.asStateFlow()

    override fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _isRunning.update { false }
        _isStopAfterCurrentTrack.update { false }
        _timeLeft.update { 0 }
        scope.launch { clearPersist() }
    }

    override fun begin(durationSeconds: Int) {
        if (durationSeconds <= 0) return
        timerJob?.cancel()
        timerJob = null
        _isStopAfterCurrentTrack.update { false }
        scope.launch {
            userPreferences.updateData {
                it.copy {
                    sleepTimerEndEpochMs = System.currentTimeMillis() + durationSeconds * 1000L
                    sleepTimerStopAfterCurrentTrack = false
                }
            }
            startDurationCountdown(durationSeconds)
        }
    }

    override fun beginStopAfterCurrentTrack() {
        timerJob?.cancel()
        timerJob = null
        scope.launch {
            userPreferences.updateData {
                it.copy {
                    sleepTimerEndEpochMs = 0L
                    sleepTimerStopAfterCurrentTrack = true
                }
            }
        }
        _isStopAfterCurrentTrack.update { true }
        _isRunning.update { true }
        _timeLeft.update { 0 }
    }

    override fun consumeStopAfterCurrentTrack(): Boolean {
        if (!_isStopAfterCurrentTrack.value) return false
        _isStopAfterCurrentTrack.update { false }
        _isRunning.update { false }
        scope.launch { clearPersist() }
        return true
    }

    override fun restoreIfNeeded() {
        scope.launch {
            if (timerJob?.isActive == true) return@launch
            val prefs = userPreferences.data.first()
            if (prefs.sleepTimerStopAfterCurrentTrack) {
                _isStopAfterCurrentTrack.update { true }
                _isRunning.update { true }
                _timeLeft.update { 0 }
                return@launch
            }
            val end = prefs.sleepTimerEndEpochMs
            if (end <= 0L) return@launch
            val now = System.currentTimeMillis()
            if (end <= now) {
                clearPersist()
                return@launch
            }
            val remaining = ((end - now) / 1000L).toInt().coerceAtLeast(1)
            startDurationCountdown(remaining)
        }
    }

    private suspend fun clearPersist() {
        userPreferences.updateData {
            it.copy {
                sleepTimerEndEpochMs = 0L
                sleepTimerStopAfterCurrentTrack = false
            }
        }
    }

    private fun startDurationCountdown(durationSeconds: Int) {
        timerJob?.cancel()
        timerJob = scope.launch {
            _isRunning.update { true }
            var left = durationSeconds
            _timeLeft.update { left }
            while (left > 0) {
                delay(1000L)
                left--
                _timeLeft.update { left }
            }
            closeIntent.send()
            _isRunning.update { false }
            clearPersist()
        }
    }
}
