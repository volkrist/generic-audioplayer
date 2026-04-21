package com.generic.audioplayes.data

import androidx.datastore.core.DataStore
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SongService
import com.generic.audioplayes.nowplaying.RepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QueueStateProvider @Inject constructor(
    private val queueState: DataStore<QueueState>,
    private val coroutineScope: CoroutineScope,
    private val crashReporter: AudioPlayerCrashReporter,
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
        val safePos = startPosition.coerceAtLeast(0L)
        val safeQueueStartIndex = if (queue.isEmpty()) {
            0
        } else {
            queueStartIndex.coerceIn(0, queue.lastIndex)
        }
        queueState.updateData {
            it.copy {
                locations.apply {
                    clear()
                    addAll(queue)
                }
                startIndex = safeQueueStartIndex
                startPositionMs = safePos
                this.repeatModeOrdinal = repeatModeOrdinal
                this.shuffleMode = shuffleMode
                this.wasPlaying = wasPlaying
            }
        }
    }

    /**
     * Restores in-memory queue and repeat mode from disk. Does not start [AudioPlayerService].
     * @return true if a non-empty queue was restored
     */
    suspend fun restoreQueueIfPossible(
        songService: SongService,
        queueService: QueueService,
    ): Boolean {
        val tag = "QueueStateProvider.restore"
        try {
            val persisted = queueState.data.first()
            val savedLocationsCount = persisted.locationsCount
            val rawIndex = persisted.startIndex
            val rawPosition = persisted.startPositionMs
            crashReporter.logData(
                "$tag savedLocations=$savedLocationsCount startIndex=$rawIndex startPositionMs=$rawPosition",
            )
            Timber.d(
                "$tag savedLocations=$savedLocationsCount startIndex=$rawIndex startPositionMs=$rawPosition",
            )

            if (savedLocationsCount == 0 || persisted.locationsList.isEmpty()) {
                Timber.d("$tag skip (empty locations)")
                return false
            }

            val songs = songService.getSongsFromLocations(persisted.locationsList)
            val locationMap = songs.associateBy { it.location }
            val orderedSongs = buildList {
                for (location in persisted.locationsList) {
                    locationMap[location]?.let { add(it) }
                }
            }

            Timber.d(
                "$tag after filter restoredCount=${orderedSongs.size} (missing files dropped silently)",
            )
            crashReporter.logData("$tag restoredCount=${orderedSongs.size}")

            if (orderedSongs.isEmpty()) {
                // Library DB may not be ready yet (cold start before MediaStore sync). Keep the
                // persisted snapshot intact so the next restoration attempt (after sync) can use
                // it; otherwise the user's last-played track silently disappears across restarts.
                Timber.w("$tag no songs resolved — keeping persisted state for later retry")
                return false
            }

            var safeIndex = rawIndex
            if (safeIndex < 0) safeIndex = 0
            if (safeIndex >= orderedSongs.size) safeIndex = 0

            var safePositionMs = rawPosition.coerceAtLeast(0L)
            if (rawIndex != safeIndex || rawIndex >= orderedSongs.size) {
                safePositionMs = 0L
            }

            val repeatOrdinal = persisted.repeatModeOrdinal
            val repeatValues = RepeatMode.values()
            val mode = repeatValues.getOrNull(repeatOrdinal.coerceIn(0, repeatValues.lastIndex))
                ?: RepeatMode.NO_REPEAT

            queueService.updateRepeatMode(mode)
            queueService.setQueue(orderedSongs, safeIndex)

            Timber.d(
                "$tag applied queueSize=${orderedSongs.size} startIndex=$safeIndex startPositionMs=$safePositionMs",
            )
            crashReporter.logData(
                "$tag applied queueSize=${orderedSongs.size} startIndex=$safeIndex startPositionMs=$safePositionMs",
            )

            try {
                persistStateNow(
                    queue = orderedSongs.map(Song::location),
                    queueStartIndex = safeIndex,
                    startPosition = safePositionMs,
                    repeatModeOrdinal = repeatOrdinal,
                    shuffleMode = persisted.shuffleMode,
                    wasPlaying = persisted.wasPlaying,
                )
            } catch (e: Exception) {
                crashReporter.logException(e)
            }

            return true
        } catch (e: Exception) {
            crashReporter.logException(e)
            Timber.e(e, "$tag failed")
            try {
                queueState.updateData { QueueState.getDefaultInstance() }
            } catch (e2: Exception) {
                crashReporter.logException(e2)
            }
            return false
        }
    }

    suspend fun readStartPositionMs(): Long = queueState.data.first().startPositionMs.coerceAtLeast(0L)

    suspend fun readWasPlaying(): Boolean = queueState.data.first().wasPlaying

    suspend fun clearPersistedState() {
        queueState.updateData { QueueState.getDefaultInstance() }
    }
}
