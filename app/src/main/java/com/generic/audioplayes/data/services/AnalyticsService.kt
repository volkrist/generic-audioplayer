package com.generic.audioplayes.data.services

import com.generic.audioplayes.data.analytics.PlayHistoryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface AnalyticsService {
    fun logSongPlay(songLocation: String, playDuration: Long)
}

class AnalyticsServiceImpl(
    private val playHistoryDao: PlayHistoryDao,
    private val scope: CoroutineScope,
): AnalyticsService {
    override fun logSongPlay(songLocation: String, playDuration: Long) {
        scope.launch {
            playHistoryDao.addRecord(songLocation, playDuration)
        }
    }
}