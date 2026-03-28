package com.github.pakka_papad.data

import androidx.datastore.core.DataStore
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.services.QueueService
import com.github.pakka_papad.data.services.SongService
import com.github.pakka_papad.nowplaying.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueStateProvider @Inject constructor(
    private val queueState: DataStore<QueueState>,
    private val coroutineScope: CoroutineScope,
) {
    val state: Flow<QueueState>
        get() = queueState.data

    fun saveState(
        queue: List<String>,
        startIndex: Int,
        startPosition: Long,
        repeatModeOrdinal: Int,
        shuffleMode: Int,
        wasPlaying: Boolean,
    ) {
        coroutineScope.launch {
            persistStateNow(
                queue = queue,
                queueStartIndex = startIndex,
                startPosition = startPosition,
                repeatModeOrdinal = repeatModeOrdinal,
                shuffleMode = shuffleMode,
                wasPlaying = wasPlaying,
            )
        }
    }

    suspend fun persistStateNow(
        queue: List<String>,
        queueStartIndex: Int,
        startPosition: Long,
        repeatModeOrdinal: Int,
        shuffleMode: Int,
        wasPlaying: Boolean,
    ) {
        queueState.updateData {
            it.copy {
                locations.apply {
                    clear()
                    addAll(queue)
                }
                startIndex = queueStartIndex
                startPositionMs = startPosition
                this.repeatModeOrdinal = repeatModeOrdinal
                this.shuffleMode = shuffleMode
                this.wasPlaying = wasPlaying
            }
        }
    }

    /**
     * Restores in-memory queue and repeat mode from disk. Does not start [ZenPlayer].
     * @return true if a non-empty queue was restored
     */
    suspend fun restoreQueueIfPossible(
        songService: SongService,
        queueService: QueueService,
    ): Boolean {
        val persisted = queueState.data.first()
        if (persisted.locationsCount == 0) return false

        val songs = songService.getSongsFromLocations(persisted.locationsList)
        val locationMap = songs.associateBy { it.location }
        val orderedSongs = buildList {
            for (location in persisted.locationsList) {
                locationMap[location]?.let { add(it) }
            }
        }
        if (orderedSongs.isEmpty()) {
            queueState.updateData { QueueState.getDefaultInstance() }
            return false
        }

        val idx = persisted.startIndex.coerceIn(0, orderedSongs.lastIndex)
        val repeatOrdinal = persisted.repeatModeOrdinal
        val repeatValues = RepeatMode.values()
        val mode = repeatValues.getOrNull(repeatOrdinal.coerceIn(0, repeatValues.lastIndex))
            ?: RepeatMode.NO_REPEAT
        queueService.updateRepeatMode(mode)
        queueService.setQueue(orderedSongs, idx)
        return true
    }

    suspend fun readStartPositionMs(): Long = queueState.data.first().startPositionMs

    suspend fun readWasPlaying(): Boolean = queueState.data.first().wasPlaying

    suspend fun clearPersistedState() {
        queueState.updateData { QueueState.getDefaultInstance() }
    }
}
