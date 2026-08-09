package com.generic.audioplayes.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.generic.audioplayes.Constants
import com.generic.audioplayes.data.QueueState
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SongService
import com.generic.audioplayes.nowplaying.RepeatMode
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class SessionCallback @Inject constructor(
    @ApplicationContext context: Context,
    private val queueService: QueueService,
    private val songService: SongService,
    private val scope: CoroutineScope,
    private val queueState: DataStore<QueueState>,
    private val crashReporter: AudioPlayerCrashReporter,
): MediaSession.Callback {

    /**
     * The player commands granted here are the full set rather than the ones the session reports at
     * this moment. media3 builds its notification controller as soon as the session exists, when
     * [ExoPlayer][androidx.media3.exoplayer.ExoPlayer] still has no items and therefore almost no
     * available commands. Echoing that snapshot back pins the controller to it for good: the platform
     * session then advertises no transport actions, so System UI shows no media card and headset
     * buttons have nowhere to go, and media3 sees an empty timeline and drops the notification
     * altogether. The player itself still rejects anything it cannot currently do.
     */
    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        crashReporter.logData("SessionCallback.onConnect() connectionResult:${connectionResult.isAccepted}")
        val availableCommands = connectionResult.availableSessionCommands.buildUpon()
        availableCommands.add(AudioPlayerCommandButtons.liked.sessionCommand!!)
        availableCommands.add(AudioPlayerCommandButtons.unliked.sessionCommand!!)
        availableCommands.add(AudioPlayerCommandButtons.cancel.sessionCommand!!)
        return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(availableCommands.build())
            .setAvailablePlayerCommands(Player.Commands.EMPTY.buildUpon().addAllCommands().build())
            .setMediaButtonPreferences(
                AudioPlayerCommandButtons.mediaButtonPreferences(
                    queueService.currentSong.value?.favourite ?: false,
                ),
            )
            .build()
    }

    /**
     * Where [AudioPlayerService.onSetQueue] should start the restored queue. Repopulating
     * [queueService] in [onPlaybackResumption] notifies the service, which otherwise starts the track
     * from the beginning and loses the position the user stopped at.
     */
    private var pendingResumePositionMs: Long? = null

    fun consumePendingResumePositionMs(): Long? =
        pendingResumePositionMs.also { pendingResumePositionMs = null }

    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
        super.onPostConnect(session, controller)
        val isLiked = queueService.currentSong.value?.favourite ?: false
        Timber.d("onPostConnect() -> ${session.player.currentMediaItem?.mediaMetadata?.title} isLiked: $isLiked")
        session.setMediaButtonPreferences(
            controller,
            AudioPlayerCommandButtons.mediaButtonPreferences(isLiked),
        )
    }

    private val closeAction =  PendingIntent.getBroadcast(
        context, AudioPlayerBroadcastReceiver.CANCEL_ACTION_REQUEST_CODE,
        Intent(Constants.PACKAGE_NAME).putExtra(
            AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
            AudioPlayerBroadcastReceiver.AUDIO_PLAYER_CANCEL
        ),
        PendingIntent.FLAG_IMMUTABLE
    )

    private val likeUnlikeAction = PendingIntent.getBroadcast(
        context, AudioPlayerBroadcastReceiver.LIKE_ACTION_REQUEST_CODE,
        Intent(Constants.PACKAGE_NAME).putExtra(
            AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
            AudioPlayerBroadcastReceiver.AUDIO_PLAYER_LIKE
        ),
        PendingIntent.FLAG_IMMUTABLE
    )

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        val result = SettableFuture.create<SessionResult>()
        when(customCommand.customAction) {
            AudioPlayerCommands.LIKE, AudioPlayerCommands.UNLIKE -> {
                likeUnlikeAction.send()
            }
            AudioPlayerCommands.CLOSE -> {
                closeAction.send()
            }
        }
        result.set(SessionResult(SessionResult.RESULT_SUCCESS))
        return result
    }

    /**
     * Probed by Media3 whenever a new [MediaSession.ControllerInfo] attaches to the session — in
     * practice that includes: MainActivity resuming (it builds a MediaController to sync the
     * widget), the home‑screen widgets reconnecting, the system media‑resumption UI, BT/headset
     * events, and every drawer navigation that lands on a fragment which incidentally creates a
     * controller.
     *
     * Earlier implementation unconditionally called [queueService.setQueue], which re-fires the
     * [com.generic.audioplayes.data.services.QueueService.Listener] on [AudioPlayerService].
     * That listener rebuilds the exoplayer media items with `startPositionMs = 0L` and
     * `autoPlay = true` — i.e. the currently‑playing track gets yanked back to the beginning.
     * That's exactly the "audio resets when I open the drawer menu" bug.
     *
     * Fix:
     *  1. If the session player is already populated, echo back its current queue + position so
     *     Media3 is happy and nothing else touches the player.
     *  2. Only fall through to the full persisted‑state load when the session is genuinely idle
     *     (first‑launch notification resumption, cold start after swipe‑kill, etc).
     */
    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        crashReporter.logData("SessionCallback.onPlaybackResumption()")
        val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            try {
                // Read the current session state on the main thread — ExoPlayer is single-threaded.
                val sessionSnapshot = withContext(Dispatchers.Main) {
                    val player = mediaSession.player
                    val count = player.mediaItemCount
                    if (count == 0) null else LiveSessionSnapshot(
                        items = buildList {
                            for (i in 0 until count) add(player.getMediaItemAt(i))
                        },
                        index = player.currentMediaItemIndex.coerceAtLeast(0),
                        positionMs = player.currentPosition.coerceAtLeast(0L),
                    )
                }
                if (sessionSnapshot != null) {
                    Timber.d(
                        "SessionCallback.onPlaybackResumption echo live session items=${sessionSnapshot.items.size} idx=${sessionSnapshot.index} pos=${sessionSnapshot.positionMs}",
                    )
                    result.set(
                        MediaSession.MediaItemsWithStartPosition(
                            sessionSnapshot.items,
                            sessionSnapshot.index,
                            sessionSnapshot.positionMs,
                        ),
                    )
                    return@launch
                }

                // Genuine cold resume — the service has no live queue, repopulate from DataStore.
                val state = queueState.data.first()
                val savedCount = state.locationsCount
                val rawIdx = state.startIndex
                val rawPos = state.startPositionMs
                Timber.d(
                    "SessionCallback.onPlaybackResumption cold-start savedLocations=$savedCount rawIndex=$rawIdx rawPositionMs=$rawPos",
                )
                val songs = songService.getSongsFromLocations(state.locationsList)
                val locationMap = buildMap {
                    for (song in songs) {
                        put(song.location, song)
                    }
                }
                val orderedSongs = buildList {
                    for (location in state.locationsList) {
                        if (locationMap.containsKey(location)) {
                            add(locationMap[location]!!)
                        }
                    }
                }
                Timber.d(
                    "SessionCallback.onPlaybackResumption restoredCount=${orderedSongs.size}",
                )
                if (orderedSongs.isEmpty()) {
                    // Intentionally do NOT clear persisted state here — the library DB may just
                    // be warming up after a cold boot. Clearing it silently loses the user's
                    // last-played track; leaving it lets a later restore retry succeed.
                    result.set(
                        MediaSession.MediaItemsWithStartPosition(
                            emptyList(),
                            0,
                            0L,
                        ),
                    )
                    return@launch
                }
                val repeatValues = RepeatMode.values()
                val repeatMode = repeatValues.getOrNull(
                    state.repeatModeOrdinal.coerceIn(0, repeatValues.lastIndex),
                ) ?: RepeatMode.NO_REPEAT
                var safeIndex = rawIdx
                if (safeIndex < 0) safeIndex = 0
                if (safeIndex >= orderedSongs.size) safeIndex = 0
                var safePositionMs = rawPos.coerceAtLeast(0L)
                if (rawIdx < 0 || rawIdx >= orderedSongs.size) {
                    safePositionMs = 0L
                }
                Timber.d(
                    "SessionCallback.onPlaybackResumption applying index=$safeIndex positionMs=$safePositionMs",
                )
                queueService.updateRepeatMode(repeatMode)
                queueService.clearQueue()
                pendingResumePositionMs = safePositionMs
                queueService.setQueue(orderedSongs, safeIndex)
                result.set(
                    MediaSession.MediaItemsWithStartPosition(
                        orderedSongs.map(Song::toMediaItem),
                        safeIndex,
                        safePositionMs,
                    ),
                )
            } catch (e: Exception) {
                crashReporter.logException(e)
                try {
                    result.set(
                        MediaSession.MediaItemsWithStartPosition(
                            emptyList(),
                            0,
                            0L,
                        ),
                    )
                } catch (e2: Exception) {
                    crashReporter.logException(e2)
                }
            }
        }
        return result
    }

    private data class LiveSessionSnapshot(
        val items: List<androidx.media3.common.MediaItem>,
        val index: Int,
        val positionMs: Long,
    )

}