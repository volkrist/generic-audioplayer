package com.github.pakka_papad.data.services

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.await
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.ZenCrashReporter
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.player.ZenPlayer
import com.github.pakka_papad.player.toMediaItem
import com.github.pakka_papad.toCorrectedParams
import com.github.pakka_papad.toExoPlayerPlaybackParameters
import kotlinx.coroutines.Dispatchers
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
}

class PlayerServiceImpl(
    private val context: Context,
    private val queueService: QueueService,
    private val preferenceProvider: ZenPreferenceProvider,
    private val crashReporter: ZenCrashReporter,
) : PlayerService {

    private val lastCallTime = AtomicLong(0)

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
        synchronized(lastCallTime) {
            if (lastCallTime.get() + 1000 >= System.currentTimeMillis()) return
            lastCallTime.set(System.currentTimeMillis())
        }
        if (songs.isEmpty()) return

        queueService.setQueue(songs, startPlayingFromPosition)

        if (ZenPlayer.isRunning.get()) {
            MediaController.Builder(
                context,
                SessionToken(context, ComponentName(context, ZenPlayer::class.java)),
            ).buildAsync().await().apply {
                withContext(Dispatchers.Main) {
                    seekTo(startPlayingFromPosition, startPositionMs)
                    if (autoPlay) play() else pause()
                }
            }
            return
        }

        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, ZenPlayer::class.java)),
        ).buildAsync().await().apply {
            withContext(Dispatchers.Main) {
                stop()
                clearMediaItems()
                addMediaItems(songs.map(Song::toMediaItem))
                prepare()
                seekTo(startPlayingFromPosition, startPositionMs)
                repeatMode = queueService.repeatMode.value.toExoPlayerRepeatMode()
                playbackParameters = preferenceProvider.playbackParams.value
                    .toCorrectedParams()
                    .toExoPlayerPlaybackParameters()
                if (autoPlay) play() else pause()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun togglePlayPauseIfRunning() {
        if (!ZenPlayer.isRunning.get()) return
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, ZenPlayer::class.java)),
        ).buildAsync().await().apply {
            withContext(Dispatchers.Main) {
                if (isPlaying) pause() else play()
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override suspend fun stopPlaybackAndClearQueueIfRunning() {
        if (!ZenPlayer.isRunning.get()) return
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, ZenPlayer::class.java)),
        ).buildAsync().await().apply {
            withContext(Dispatchers.Main) {
                stop()
                clearMediaItems()
            }
        }
    }
}
