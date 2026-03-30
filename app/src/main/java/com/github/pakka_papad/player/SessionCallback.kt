package com.github.pakka_papad.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.github.pakka_papad.Constants
import com.github.pakka_papad.data.QueueState
import com.github.pakka_papad.data.ZenCrashReporter
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.data.services.QueueService
import com.github.pakka_papad.data.services.SongService
import com.github.pakka_papad.nowplaying.RepeatMode
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@ServiceScoped
class SessionCallback @Inject constructor(
    @ApplicationContext context: Context,
    private val queueService: QueueService,
    private val songService: SongService,
    private val scope: CoroutineScope,
    private val queueState: DataStore<QueueState>,
    private val crashReporter: ZenCrashReporter,
): MediaSession.Callback {

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        crashReporter.logData("SessionCallback.onConnect() connectionResult:${connectionResult.isAccepted}")
        val availableCommands = connectionResult.availableSessionCommands.buildUpon()
        availableCommands.add(ZenCommandButtons.liked.sessionCommand!!)
        availableCommands.add(ZenCommandButtons.unliked.sessionCommand!!)
        availableCommands.add(ZenCommandButtons.cancel.sessionCommand!!)
        return MediaSession.ConnectionResult.accept(
            availableCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
        super.onPostConnect(session, controller)
        val isLiked = queueService.currentSong.value?.favourite ?: false
        Timber.d("onPostConnect() -> ${session.player.currentMediaItem?.mediaMetadata?.title} isLiked: $isLiked")
        session.setCustomLayout(
            controller,
            listOf(
                if (isLiked) ZenCommandButtons.liked else ZenCommandButtons.unliked,
                ZenCommandButtons.previous,
                ZenCommandButtons.playPause,
                ZenCommandButtons.next,
                ZenCommandButtons.cancel
            )
        )
    }

    private val closeAction =  PendingIntent.getBroadcast(
        context, ZenBroadcastReceiver.CANCEL_ACTION_REQUEST_CODE,
        Intent(Constants.PACKAGE_NAME).putExtra(
            ZenBroadcastReceiver.AUDIO_CONTROL,
            ZenBroadcastReceiver.ZEN_PLAYER_CANCEL
        ),
        PendingIntent.FLAG_IMMUTABLE
    )

    private val likeUnlikeAction = PendingIntent.getBroadcast(
        context, ZenBroadcastReceiver.LIKE_ACTION_REQUEST_CODE,
        Intent(Constants.PACKAGE_NAME).putExtra(
            ZenBroadcastReceiver.AUDIO_CONTROL,
            ZenBroadcastReceiver.ZEN_PLAYER_LIKE
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
            ZenCommands.LIKE, ZenCommands.UNLIKE -> {
                likeUnlikeAction.send()
            }
            ZenCommands.CLOSE -> {
                closeAction.send()
            }
        }
        result.set(SessionResult(SessionResult.RESULT_SUCCESS))
        return result
    }

    @UnstableApi
    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        crashReporter.logData("SessionCallback.onPlaybackResumption()")
        val result = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
        scope.launch {
            try {
                val state = queueState.data.first()
                val savedCount = state.locationsCount
                val rawIdx = state.startIndex
                val rawPos = state.startPositionMs
                Timber.d(
                    "SessionCallback.onPlaybackResumption savedLocations=$savedCount rawIndex=$rawIdx rawPositionMs=$rawPos",
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
                    queueState.updateData { QueueState.getDefaultInstance() }
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
                    queueState.updateData { QueueState.getDefaultInstance() }
                } catch (eClear: Exception) {
                    crashReporter.logException(eClear)
                }
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

}