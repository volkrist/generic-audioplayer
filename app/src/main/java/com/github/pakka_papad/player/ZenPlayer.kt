package com.github.pakka_papad.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.github.pakka_papad.Constants
import com.github.pakka_papad.data.QueueStateProvider
import com.github.pakka_papad.data.ZenCrashReporter
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.music.SongExtractor
import com.github.pakka_papad.data.services.AnalyticsService
import com.github.pakka_papad.data.services.QueueService
import com.github.pakka_papad.data.services.SleepTimerService
import com.github.pakka_papad.data.services.SongService
import com.github.pakka_papad.toCorrectedParams
import com.github.pakka_papad.toExoPlayerPlaybackParameters
import com.github.pakka_papad.widgets.PlayerWidgetManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@UnstableApi
@AndroidEntryPoint
class ZenPlayer : MediaSessionService(), QueueService.Listener, ZenBroadcastReceiver.Callback {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    @Inject lateinit var songExtractor: SongExtractor
    @Inject lateinit var queueService: QueueService
    @Inject lateinit var songService: SongService
    @Inject lateinit var sleepTimerService: SleepTimerService
    @Inject lateinit var analyticsService: AnalyticsService
    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var crashReporter: ZenCrashReporter
    @Inject lateinit var preferencesProvider: ZenPreferenceProvider
    @Inject lateinit var queueStateProvider: QueueStateProvider
    @Inject lateinit var sessionCallback: SessionCallback
    @Inject lateinit var playerNotificationManager: PlayerNotificationManager
    @Inject lateinit var playerWidgetManager: PlayerWidgetManager

    private var broadcastReceiver: ZenBroadcastReceiver? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    private val mainHandler = Handler(Looper.getMainLooper())

    private val debouncedSaveRunnable = Runnable {
        scope.launch(Dispatchers.Main) {
            try {
                persistSnapshotIfPossible()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
        }
    }

    private val periodicSaveRunnable = object : Runnable {
        override fun run() {
            scope.launch(Dispatchers.Main) {
                try {
                    persistSnapshotIfPossible()
                } catch (e: Exception) {
                    crashReporter.logException(e)
                }
            }
            if (exoPlayer.isPlaying) {
                mainHandler.postDelayed(this, 12_000L)
            }
        }
    }

    private suspend fun persistSnapshotIfPossible() {
        if (queueService.queue.isEmpty()) return
        queueStateProvider.persistStateNow(
            queue = queueService.queue.map { it.location },
            queueStartIndex = exoPlayer.currentMediaItemIndex.coerceAtLeast(0),
            startPosition = exoPlayer.currentPosition,
            repeatModeOrdinal = queueService.repeatMode.value.ordinal,
            shuffleMode = 0,
            wasPlaying = exoPlayer.isPlaying,
        )
    }

    private fun schedulePersistQueueState() {
        mainHandler.removeCallbacks(debouncedSaveRunnable)
        mainHandler.postDelayed(debouncedSaveRunnable, 400)
    }

    private val playTimeThresholdMs = 10.seconds.inWholeMilliseconds

    private val playbackStatsListener = PlaybackStatsListener(false) { eventTime, playbackStats ->
        if (playbackStats.totalPlayTimeMs < playTimeThresholdMs) return@PlaybackStatsListener
        val window = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window())
        try {
            analyticsService.logSongPlay(window.mediaItem.mediaId, playbackStats.totalPlayTimeMs)
        } catch (e : Exception){
            crashReporter.logException(e)
        }
    }

    companion object {
        const val MEDIA_SESSION = "media_session"
        val isRunning = AtomicBoolean(false)
    }

    private lateinit var mediaSession: MediaSession

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        crashReporter.logData("ZenPlayer.onCreate() isRunning:${isRunning.get()}")
        isRunning.set(true)
        broadcastReceiver = ZenBroadcastReceiver()
        mediaSession = MediaSession.Builder(applicationContext, exoPlayer)
            .setCallback(sessionCallback)
            .setId(System.currentTimeMillis().toString())
            .build()

        queueService.addListener(this)

        IntentFilter(Constants.PACKAGE_NAME).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(broadcastReceiver, it, RECEIVER_EXPORTED)
            } else {
                registerReceiver(broadcastReceiver, it)
            }
        }
        broadcastReceiver?.startListening(this)
        exoPlayer.addListener(exoPlayerListener)
        exoPlayer.addAnalyticsListener(playbackStatsListener)

        scope.launch {
            preferencesProvider.playbackParams.collect {
                val params = it.toCorrectedParams().toExoPlayerPlaybackParameters()
                withContext(Dispatchers.Main){
                    exoPlayer.playbackParameters = params
                }
            }
        }
        scope.launch {
            queueService.repeatMode.collect {
                withContext(Dispatchers.Main) { exoPlayer.repeatMode = it.toExoPlayerRepeatMode() }
            }
        }
        scope.launch {
            preferencesProvider.volumeBoosterPercent.collect { percent ->
                withContext(Dispatchers.Main) {
                    exoPlayer.volume = percent / 100f
                }
            }
        }

        setMediaNotificationProvider(playerNotificationManager)
    }

    private val exoPlayerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
                sleepTimerService.consumeStopAfterCurrentTrack()
            ) {
                scope.launch(Dispatchers.Main) { exoPlayer.pause() }
            }
            super.onMediaItemTransition(mediaItem, reason)

            try {
                queueService.setCurrentSong(exoPlayer.currentMediaItemIndex)
                queueService.getSongAtIndex(exoPlayer.currentMediaItemIndex)?.let { song ->
                    updateNotification(song.favourite)
                    playerWidgetManager.notifySongChanged(song)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            schedulePersistQueueState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            if (playbackState == Player.STATE_ENDED && sleepTimerService.consumeStopAfterCurrentTrack()) {
                scope.launch(Dispatchers.Main) { exoPlayer.pause() }
            }
            schedulePersistQueueState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            try {
                queueService.getSongAtIndex(exoPlayer.currentMediaItemIndex)?.let { song ->
                    updateNotification(song.favourite)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
            playerWidgetManager.notifyIsPlayingChanged(isPlaying)
            schedulePersistQueueState()
            if (isPlaying) {
                mainHandler.removeCallbacks(periodicSaveRunnable)
                mainHandler.postDelayed(periodicSaveRunnable, 12_000L)
            } else {
                mainHandler.removeCallbacks(periodicSaveRunnable)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            crashReporter.logException(error)
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND){
                songExtractor.cleanData()
                onBroadcastCancel()
            }
        }

        override fun onPlayerErrorChanged(error: PlaybackException?) {
            super.onPlayerErrorChanged(error)
            crashReporter.logException(error)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
        crashReporter.logData("ZenPlayer.onDestroy() isRunning:${isRunning.get()}")
        stopService()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun stopService() {
        isRunning.set(false)
        mainHandler.removeCallbacks(debouncedSaveRunnable)
        mainHandler.removeCallbacks(periodicSaveRunnable)
        runBlocking {
            try {
                persistSnapshotIfPossible()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
        }
        with(queueService) {
            clearQueue()
            removeListener(this@ZenPlayer)
        }
        with(exoPlayer) {
            stop()
            clearMediaItems()
            removeAnalyticsListener(playbackStatsListener)
            removeListener(exoPlayerListener)
        }
        scope.cancel()
        job.cancel()

        sleepTimerService.cancel()

        broadcastReceiver?.let { unregisterReceiver(it) }
        broadcastReceiver?.stopListening()
        broadcastReceiver = null
    }

    private fun updateNotification(isLiked: Boolean) {
        mediaSession.setCustomLayout(
            listOf(
                if (isLiked) ZenCommandButtons.liked else ZenCommandButtons.unliked,
                ZenCommandButtons.previous,
                ZenCommandButtons.playPause,
                ZenCommandButtons.next,
                ZenCommandButtons.cancel
            )
        )
    }

    private fun setQueue(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
    ) {
        crashReporter.logData("ZenPlayer.setQueue(List<MediaItem>,Int,Long,Boolean)")
        scope.launch {
            val repeatMode = queueService.repeatMode.first()
            withContext(Dispatchers.Main) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.addMediaItems(mediaItems)
                exoPlayer.prepare()
                exoPlayer.seekTo(startIndex, startPositionMs)
                exoPlayer.repeatMode = repeatMode.toExoPlayerRepeatMode()
                exoPlayer.playbackParameters = preferencesProvider.playbackParams.value
                    .toCorrectedParams()
                    .toExoPlayerPlaybackParameters()
                if (autoPlay) exoPlayer.play() else exoPlayer.pause()
            }
        }
    }

    override fun onAppend(song: Song) {
        exoPlayer.addMediaItem(song.toMediaItem())
    }

    override fun onAppend(songs: List<Song>) {
        exoPlayer.addMediaItems(
            songs.map(Song::toMediaItem)
        )
    }

    override fun onUpdate(updatedSong: Song, position: Int) {
        scope.launch {
            val performUpdate = withContext(Dispatchers.Main) {
                exoPlayer.currentMediaItemIndex == position
            }
            if (!performUpdate) return@launch
            updateNotification(updatedSong.favourite)
        }
    }

    override fun onMove(from: Int, to: Int) {
        exoPlayer.moveMediaItem(from, to)
    }

    override fun onClear() {
        crashReporter.logData("ZenPlayer.onClear()")
    }

    override fun onSetQueue(songs: List<Song>, startPlayingFromPosition: Int) {
        crashReporter.logData("ZenPlayer.onSetQueue(List<Song>,Int)")
        val mediaItems = songs.map(Song::toMediaItem)
        setQueue(mediaItems, startPlayingFromPosition, startPositionMs = 0L, autoPlay = true)
    }

    /**
     * Called when user clicks play/pause button in notification.
     * Player.Listener onIsPlayingChanged gets called.
     */
    override fun onBroadcastPausePlay() {
        Timber.d("onBroadcastPausePlay()")
        crashReporter.logData("ZenPlayer.onBroadcastPausePlay()")
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    /**
     * Called when user clicks next button in notification.
     * If we have next song in queue we skip to it.
     * Player.Listener onMediaItemTransition gets called.
     */
    override fun onBroadcastNext() {
        Timber.d("onBroadcastNext()")
        crashReporter.logData("ZenPlayer.onBroadcastNext()")
        if (!exoPlayer.hasNextMediaItem()) {
            showToast("No next song in queue")
            return
        }
        exoPlayer.seekToNextMediaItem()
    }

    /**
     * Called when user clicks previous button in notification.
     * If we have previous song in queue we skip to it.
     * Player.Listener onMediaItemTransition gets called.
     */
    override fun onBroadcastPrevious() {
        Timber.d("onBroadcastPrevious()")
        crashReporter.logData("ZenPlayer.onBroadcastPrevious()")
        if (!exoPlayer.hasPreviousMediaItem()) {
            showToast("No previous song in queue")
            return
        }
        exoPlayer.seekToPreviousMediaItem()
    }

    /**
     * Called when user clicks on like icon (filled and outlined both)
     * This fetches the current song, toggles the favourite and passes the updated song to DataManager
     * DataManager then calls updateNotification of DataManager.Callback
     */
    override fun onBroadcastLike() {
        Timber.d("onBroadcastLike()")
        crashReporter.logData("ZenPlayer.onBroadcastLike()")
        val currentSong = queueService.getSongAtIndex(exoPlayer.currentMediaItemIndex) ?: return
        val updatedSong = currentSong.copy(favourite = !currentSong.favourite)
        scope.launch {
            queueService.update(updatedSong)
            songService.updateSong(updatedSong)
        }
    }

    /**
     * Called when user clicks close button in notification
     * This stops the service and onDestroy is called
     */
    override fun onBroadcastCancel() {
        Timber.d("onBroadcastCancel()")
        crashReporter.logData("ZenPlayer.onBroadcastCancel()")
        /**
         * To close the media session, first call mediaSession.release followed by stopSelf()
         * See issue: https://github.com/androidx/media/issues/389#issuecomment-1546611545
         */
        mediaSession.release()
        stopSelf()
    }

}

fun Song.toMediaItem(): MediaItem {
    return MediaItem.Builder().apply {
        setUri(Uri.fromFile(File(this@toMediaItem.location)))
        setMediaId(this@toMediaItem.location)
        setMediaMetadata(
            MediaMetadata.Builder().apply {
                setArtworkUri(this@toMediaItem.artUri?.toUri())
                setTitle(this@toMediaItem.title)
                setArtist(this@toMediaItem.artist)
                setIsBrowsable(false)
                setIsPlayable(true)
            }.build()
        )
    }.build()
}