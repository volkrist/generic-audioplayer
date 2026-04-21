package com.generic.audioplayes.data.services

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.await
import com.generic.audioplayes.data.QueueStateProvider
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.player.AudioPlayerService
import com.generic.audioplayes.player.toMediaItem
import com.generic.audioplayes.toCorrectedParams
import com.generic.audioplayes.toExoPlayerPlaybackParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

interface PlayerService {
    suspend fun startServiceIfNotRunning(
        songs: List<Song>,
        startPlayingFromPosition: Int,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
        usePersistedShuffle: Boolean = false,
    )

    suspend fun togglePlayPauseIfRunning()

    /** Stops playback and clears Exo queue; call after [QueueService.clearQueue]. */
    suspend fun stopPlaybackAndClearQueueIfRunning()

    /**
     * Writes current queue index, position and play/pause to disk (same snapshot as the player service).
     * Call when the app goes to background or process may be killed so reopen restores here.
     */
    suspend fun persistPlaybackSnapshotToDisk()
}

class PlayerServiceImpl(
    private val context: Context,
    private val queueService: QueueService,
    private val preferenceProvider: AudioPlayerPreferenceProvider,
    private val crashReporter: AudioPlayerCrashReporter,
    private val exoPlayer: ExoPlayer,
    private val queueStateProvider: QueueStateProvider,
) : PlayerService {

    private val lastCallTime = AtomicLong(0)
    /** Serializes playback starts so two overlapping taps cannot both call [QueueService.setQueue]. */
    private val startPlaybackMutex = Mutex()

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun startServiceIfNotRunning(
        songs: List<Song>,
        startPlayingFromPosition: Int,
        startPositionMs: Long,
        autoPlay: Boolean,
        @Suppress("UNUSED_PARAMETER") usePersistedShuffle: Boolean,
    ) {
        crashReporter.logData(
            "PlayerService.startServiceIfNotRunning() pos=$startPositionMs autoPlay=$autoPlay",
        )
        startPlaybackMutex.withLock {
            // Drop only true accidental double-taps (~120 ms). Previous 450 ms window made the
            // player feel sluggish: tapping a different track quickly after the first one was
            // silently ignored.
            val now = System.currentTimeMillis()
            if (lastCallTime.get() + 120 >= now) return
            lastCallTime.set(now)
            if (songs.isEmpty()) return

            queueService.setQueue(songs, startPlayingFromPosition)

            if (AudioPlayerService.isRunning.get()) {
                // [QueueService] already notified [AudioPlayerService.onSetQueue], which loads the new
                // queue and seeks/plays on the main ExoPlayer. Do not also drive MediaController here —
                // that caused a second seek/play and felt like the track "starting twice".
                return
            }

            MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, AudioPlayerService::class.java)),
            ).buildAsync().await().apply {
                withContext(Dispatchers.Main) {
                    stop()
                    clearMediaItems()
                    addMediaItems(songs.map(Song::toMediaItem))
                    prepare()
                    seekTo(startPlayingFromPosition, startPositionMs)
                    repeatMode = queueService.repeatMode.value.toExoPlayerRepeatMode(songs.size)
                    playbackParameters = preferenceProvider.playbackParams.value
                        .toCorrectedParams()
                        .toExoPlayerPlaybackParameters()
                    if (autoPlay) play() else pause()
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun togglePlayPauseIfRunning() {
        if (!AudioPlayerService.isRunning.get()) return
        // Operate directly on the singleton ExoPlayer instead of building a new MediaController
        // for every tap — controller creation involves binding to the MediaSessionService and
        // adds 100-300 ms of perceived lag on each play/pause press.
        withContext(Dispatchers.Main) {
            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun stopPlaybackAndClearQueueIfRunning() {
        if (!AudioPlayerService.isRunning.get()) return
        withContext(Dispatchers.Main) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
    }

    override suspend fun persistPlaybackSnapshotToDisk() {
        withContext(Dispatchers.Main) {
            if (queueService.queue.isEmpty()) return@withContext
            val lastIndex = queueService.queue.lastIndex
            val rawIdx = exoPlayer.currentMediaItemIndex
            val queueIdx = queueService.currentQueueIndex().coerceIn(0, lastIndex)
            val startIndex = if (rawIdx in 0..lastIndex) rawIdx else queueIdx
            queueStateProvider.persistStateNow(
                queue = queueService.queue.map { it.location },
                queueStartIndex = startIndex,
                startPosition = exoPlayer.currentPosition.coerceAtLeast(0L),
                repeatModeOrdinal = queueService.repeatMode.value.ordinal,
                shuffleMode = 0,
                wasPlaying = exoPlayer.isPlaying,
            )
        }
    }
}
