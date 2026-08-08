package com.generic.audioplayes.player

import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.generic.audioplayes.Constants
import com.generic.audioplayes.data.QueueStateProvider
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.AnalyticsService
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SleepTimerService
import com.generic.audioplayes.data.services.SongService
import com.generic.audioplayes.toCorrectedParams
import com.generic.audioplayes.toExoPlayerPlaybackParameters
import com.generic.audioplayes.volume.VolumeBoosterManager
import com.generic.audioplayes.widgets.PlayerWidgetManager
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
class AudioPlayerService : MediaSessionService(), QueueService.Listener, AudioPlayerBroadcastReceiver.Callback {

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    @Inject lateinit var songExtractor: SongExtractor
    @Inject lateinit var queueService: QueueService
    @Inject lateinit var songService: SongService
    @Inject lateinit var sleepTimerService: SleepTimerService
    @Inject lateinit var analyticsService: AnalyticsService
    @Inject lateinit var exoPlayer: ExoPlayer
    @Inject lateinit var crashReporter: AudioPlayerCrashReporter
    @Inject lateinit var preferencesProvider: AudioPlayerPreferenceProvider
    @Inject lateinit var queueStateProvider: QueueStateProvider
    @Inject lateinit var sessionCallback: SessionCallback
    @Inject lateinit var playerNotificationManager: PlayerNotificationManager
    @Inject lateinit var playerWidgetManager: PlayerWidgetManager
    @Inject lateinit var volumeBoosterManager: VolumeBoosterManager

    private var broadcastReceiver: AudioPlayerBroadcastReceiver? = null

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Position to seek to in [onSetQueue] instead of the default 0. Set only by
     * [resumePersistedPlaybackIfIdle] so a media button restores the saved position rather than
     * restarting the track.
     */
    private var pendingResumePositionMs: Long? = null

    private var resumeFromMediaButtonInFlight = false

    /** Set while a media-button resume is restoring the queue; consumed by [setQueue]. */
    private var playThroughSessionPending = false

    private var resumeController: MediaController? = null

    private var mediaSessionReleased = false

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
        crashReporter.logData("AudioPlayerService.onCreate() isRunning:${isRunning.get()}")
        isRunning.set(true)
        broadcastReceiver = AudioPlayerBroadcastReceiver()
        mediaSession = MediaSession.Builder(applicationContext, exoPlayer)
            .setCallback(sessionCallback)
            .setId(System.currentTimeMillis().toString())
            .build()

        // Registered before the collectors below so Media3 can post the foreground notification as
        // early as possible — the system kills the process if startForeground() is late by 5s.
        setMediaNotificationProvider(playerNotificationManager)

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
                withContext(Dispatchers.Main) { syncExoPlayerRepeatMode() }
            }
        }
        scope.launch {
            preferencesProvider.widgetStyle.collect {
                withContext(Dispatchers.Main) {
                    queueService.getSongAtIndex(exoPlayer.currentMediaItemIndex)?.let { song ->
                        updateNotification(song.favourite)
                    }
                }
            }
        }
    }

    /**
     * A media button that arrives while the process is dead is delivered before [onCreate] has built
     * the [MediaSession], so Media3 has nowhere to dispatch it and the play command is lost. Nothing
     * then starts playing, no playback notification is posted, and the system kills the service for
     * missing the `startForeground()` deadline. Restoring the saved queue here covers that gap.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON && isPlayRequest(intent)) {
            resumePersistedPlaybackIfIdle()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun isPlayRequest(intent: Intent): Boolean {
        val keyEvent = IntentCompat.getParcelableExtra(
            intent,
            Intent.EXTRA_KEY_EVENT,
            KeyEvent::class.java,
        ) ?: return true
        return when (keyEvent.keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_HEADSETHOOK,
            -> true
            else -> false
        }
    }

    private fun resumePersistedPlaybackIfIdle() {
        if (resumeFromMediaButtonInFlight || exoPlayer.mediaItemCount > 0) return
        resumeFromMediaButtonInFlight = true
        scope.launch {
            try {
                pendingResumePositionMs = queueStateProvider.readStartPositionMs()
                playThroughSessionPending = true
                val restored = queueStateProvider.restoreQueueIfPossible(songService, queueService)
                if (!restored) {
                    pendingResumePositionMs = null
                    playThroughSessionPending = false
                    crashReporter.logData("AudioPlayerService media button: nothing to resume")
                }
            } catch (e: Exception) {
                pendingResumePositionMs = null
                playThroughSessionPending = false
                crashReporter.logException(e)
            } finally {
                resumeFromMediaButtonInFlight = false
            }
        }
    }

    /**
     * Media3 promotes the service to a foreground service only for play requests that travel through
     * the session — that is where its notification bookkeeping lives. Playing on [exoPlayer] directly
     * leaves the service in the background, and once the media button's 10-second temporary allowlist
     * expires the platform pauses the playback again. Repeating the request through a controller of
     * our own session keeps that bookkeeping intact.
     */
    private fun requestPlayThroughSession() {
        val controllerFuture = MediaController.Builder(this, mediaSession.token).buildAsync()
        controllerFuture.addListener(
            {
                try {
                    resumeController?.release()
                    resumeController = controllerFuture.get().apply { play() }
                } catch (e: Exception) {
                    crashReporter.logException(e)
                }
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    /**
     * media3 decides whether the service must run in the foreground from the state of its own
     * notification controller, which mirrors [exoPlayer] with a delay. The notification refresh this
     * service triggers on playback changes can overtake that mirror, so the decision gets fixed at
     * "not playing" and nothing arrives later to correct it: the service stays a background service
     * and the platform stops playback within a minute. Asking for one more update once the mirror
     * caught up makes media3 promote the service.
     */
    private val foregroundRecheckRunnable = Runnable {
        try {
            triggerNotificationUpdate()
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
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
                    playerWidgetManager.notifySongChanged(song, preferencesProvider.widgetStyle.value)
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
            mainHandler.removeCallbacks(foregroundRecheckRunnable)
            if (isPlaying) {
                mainHandler.removeCallbacks(periodicSaveRunnable)
                mainHandler.postDelayed(periodicSaveRunnable, 12_000L)
                mainHandler.postDelayed(foregroundRecheckRunnable, 700L)
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
        crashReporter.logData("AudioPlayerService.onDestroy() isRunning:${isRunning.get()}")
        stopService()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private data class QueuePersistSnapshot(
        val locations: List<String>,
        val queueStartIndex: Int,
        val startPosition: Long,
        val repeatModeOrdinal: Int,
        val shuffleMode: Int,
        val wasPlaying: Boolean,
    )

    /**
     * Reads [ExoPlayer] state on the main thread only, then persistence runs on [Dispatchers.IO]
     * so [runBlocking] does not block the main looper (avoids deadlock / ANR).
     */
    private fun readQueuePersistSnapshotOnMain(): QueuePersistSnapshot? {
        if (queueService.queue.isEmpty()) return null
        return QueuePersistSnapshot(
            locations = queueService.queue.map { it.location },
            queueStartIndex = exoPlayer.currentMediaItemIndex.coerceAtLeast(0),
            startPosition = exoPlayer.currentPosition.coerceAtLeast(0),
            repeatModeOrdinal = queueService.repeatMode.value.ordinal,
            shuffleMode = 0,
            wasPlaying = exoPlayer.isPlaying,
        )
    }

    private fun stopService() {
        isRunning.set(false)
        mainHandler.removeCallbacks(debouncedSaveRunnable)
        mainHandler.removeCallbacks(periodicSaveRunnable)
        mainHandler.removeCallbacks(foregroundRecheckRunnable)
        // Pause before snapshot so wasPlaying=false and position matches where playback stopped
        // (sleep timer / notification close should resume from this point, not "still playing").
        try {
            exoPlayer.pause()
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
        val snapshot = readQueuePersistSnapshotOnMain()
        if (snapshot != null) {
            runBlocking(Dispatchers.IO) {
                try {
                    queueStateProvider.persistStateNow(
                        queue = snapshot.locations,
                        queueStartIndex = snapshot.queueStartIndex,
                        startPosition = snapshot.startPosition,
                        repeatModeOrdinal = snapshot.repeatModeOrdinal,
                        shuffleMode = snapshot.shuffleMode,
                        wasPlaying = snapshot.wasPlaying,
                    )
                } catch (e: Exception) {
                    crashReporter.logException(e)
                }
            }
        }
        volumeBoosterManager.releaseLoudnessEffect()
        resumeController?.release()
        resumeController = null
        releaseMediaSessionOnce()
        with(queueService) {
            clearQueue()
            removeListener(this@AudioPlayerService)
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
        // Order: transport first so compact notification (first 3 slots) is prev | play/pause | next.
        mediaSession.setCustomLayout(
            listOf(
                AudioPlayerCommandButtons.previous,
                AudioPlayerCommandButtons.playPause,
                AudioPlayerCommandButtons.next,
                if (isLiked) AudioPlayerCommandButtons.liked else AudioPlayerCommandButtons.unliked,
                AudioPlayerCommandButtons.cancel
            )
        )
    }

    private fun syncExoPlayerRepeatMode() {
        val n = queueService.queue.size
        exoPlayer.repeatMode = queueService.repeatMode.value.toExoPlayerRepeatMode(n)
    }

    private fun setQueue(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long = 0L,
        autoPlay: Boolean = true,
    ) {
        crashReporter.logData("AudioPlayerService.setQueue(List<MediaItem>,Int,Long,Boolean)")
        scope.launch {
            val repeatMode = queueService.repeatMode.first()
            withContext(Dispatchers.Main) {
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.addMediaItems(mediaItems)
                exoPlayer.prepare()
                exoPlayer.seekTo(startIndex, startPositionMs)
                exoPlayer.repeatMode = repeatMode.toExoPlayerRepeatMode(mediaItems.size)
                exoPlayer.playbackParameters = preferencesProvider.playbackParams.value
                    .toCorrectedParams()
                    .toExoPlayerPlaybackParameters()
                if (autoPlay) exoPlayer.play() else exoPlayer.pause()
                if (playThroughSessionPending) {
                    playThroughSessionPending = false
                    requestPlayThroughSession()
                }
            }
        }
    }

    override fun onAppend(song: Song) {
        exoPlayer.addMediaItem(song.toMediaItem())
        syncExoPlayerRepeatMode()
    }

    override fun onAppend(songs: List<Song>) {
        exoPlayer.addMediaItems(
            songs.map(Song::toMediaItem)
        )
        syncExoPlayerRepeatMode()
    }

    override fun onInsert(atIndex: Int, songs: List<Song>) {
        exoPlayer.addMediaItems(
            atIndex,
            songs.map(Song::toMediaItem),
        )
        syncExoPlayerRepeatMode()
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

    override fun onRemoveAt(index: Int) {
        exoPlayer.removeMediaItem(index)
        syncExoPlayerRepeatMode()
    }

    override fun onClear() {
        crashReporter.logData("AudioPlayerService.onClear()")
        syncExoPlayerRepeatMode()
    }

    override fun onSetQueue(songs: List<Song>, startPlayingFromPosition: Int) {
        crashReporter.logData("AudioPlayerService.onSetQueue(List<Song>,Int)")
        val mediaItems = songs.map(Song::toMediaItem)
        val resumeAtMs = pendingResumePositionMs ?: 0L
        pendingResumePositionMs = null
        setQueue(mediaItems, startPlayingFromPosition, startPositionMs = resumeAtMs, autoPlay = true)
    }

    /**
     * Called when user clicks play/pause button in notification.
     * Player.Listener onIsPlayingChanged gets called.
     */
    override fun onBroadcastPausePlay() {
        Timber.d("onBroadcastPausePlay()")
        crashReporter.logData("AudioPlayerService.onBroadcastPausePlay()")
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    /**
     * Called when user clicks next button in notification.
     * If we have next song in queue we skip to it.
     * Player.Listener onMediaItemTransition gets called.
     */
    override fun onBroadcastNext() {
        Timber.d("onBroadcastNext()")
        crashReporter.logData("AudioPlayerService.onBroadcastNext()")
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
        crashReporter.logData("AudioPlayerService.onBroadcastPrevious()")
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
        crashReporter.logData("AudioPlayerService.onBroadcastLike()")
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
        crashReporter.logData("AudioPlayerService.onBroadcastCancel()")
        /**
         * To close the media session, first call mediaSession.release followed by stopSelf()
         * See issue: https://github.com/androidx/media/issues/389#issuecomment-1546611545
         */
        releaseMediaSessionOnce()
        stopSelf()
    }

    private fun releaseMediaSessionOnce() {
        if (mediaSessionReleased) return
        mediaSessionReleased = true
        try {
            mediaSession.release()
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
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
                val album = this@toMediaItem.album.trim()
                if (album.isNotEmpty() && album != "Unknown") {
                    setAlbumTitle(album)
                }
                setIsBrowsable(false)
                setIsPlayable(true)
            }.build()
        )
    }.build()
}