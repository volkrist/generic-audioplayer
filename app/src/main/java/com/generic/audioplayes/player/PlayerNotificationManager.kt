package com.generic.audioplayes.player

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.generic.audioplayes.R
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.widgets.notificationColorizedBackgroundArgb
import com.generic.audioplayes.data.services.QueueService
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Builds a [MediaStyleNotificationHelper.MediaStyle] notification for [MediaSession], used by
 * [androidx.media3.session.MediaSessionService] for the shade, lock screen, and (where the OEM allows)
 * media surfaces on AOD.
 *
 * **Platform limits (why this is not the home widget):** the system owns the media notification layout
 * (Material / OEM themes, compact vs expanded, action row). Apps supply [NotificationCompat] + [MediaStyle]
 * + metadata + colorized tint + artwork — not arbitrary Compose/Glance. Full “catalog” geometry (gradients,
 * rounded cards, per-style grids) is impossible here by Android design; we align **palette** with
 * [com.generic.audioplayes.widgets.notificationColorizedBackgroundArgb] and **rich metadata** (album, art).
 */
@ServiceScoped
@UnstableApi
class PlayerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueService: QueueService,
    private val preferenceProvider: AudioPlayerPreferenceProvider,
) : MediaNotification.Provider {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.ensureNotificationChannel(context)
        }
    }

    /**
     * Lets [androidx.media3.session.MediaSessionService] post a placeholder foreground notification
     * before [createNotification] runs, so a media button arriving on a cold start cannot trip the
     * 5-second `startForeground()` deadline.
     */
    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        MediaNotification.Provider.NotificationChannelInfo(
            NotificationHelper.PLAYER_CHANNEL_ID,
            NotificationHelper.PLAYER_CHANNEL_NAME,
        )

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val player = mediaSession.player
        val metadata = player.currentMediaItem?.mediaMetadata
        val title = metadata?.title?.toString()
        val artist = metadata?.artist?.toString()
        val albumTitle = metadata?.albumTitle?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "Unknown" }

        val largeIcon = NotificationHelper.loadArtworkBitmap(context, metadata?.artworkUri)

        val playing = player.isPlaying
        val isLiked = queueService.getSongAtIndex(player.currentMediaItemIndex)?.favourite ?: false

        Timber.d("PlayerNotificationManager track=$title playing=$playing liked=$isLiked")

        val actions = buildActions(mediaSession, player, actionFactory, customLayout, playing, isLiked)

        // Transport occupies the leading actions, so those are the ones worth keeping in the
        // collapsed shade.
        val compactIndices = IntArray(minOf(TRANSPORT_ACTION_COUNT, actions.size)) { it }
        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(*compactIndices)

        val styleBg = preferenceProvider.widgetStyle.value.notificationColorizedBackgroundArgb()
        val builder = NotificationCompat.Builder(context, NotificationHelper.PLAYER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(title)
            .setContentText(artist)
            .apply {
                if (albumTitle != null) {
                    setSubText(albumTitle)
                }
            }
            .setLargeIcon(largeIcon)
            .setColor(styleBg)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setColorized(true)
                }
            }
            .setStyle(mediaStyle)
            .setContentIntent(NotificationHelper.contentActivityIntent(context))
            .setDeleteIntent(NotificationHelper.stopPlaybackPendingIntent(context))
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(
                if (preferenceProvider.showOnLockScreen.value) {
                    NotificationCompat.VISIBILITY_PUBLIC
                } else {
                    NotificationCompat.VISIBILITY_SECRET
                },
            )
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(playing)
            .setPriority(NotificationCompat.PRIORITY_MAX)

        actions.forEach(builder::addAction)

        return MediaNotification(NotificationHelper.PLAYER_NOTIFICATION_ID, builder.build())
    }

    /**
     * Transport actions are derived from the player instead of from [mediaButtonPreferences], because
     * the preferences only carry this app's custom buttons — media3 fills the play/pause, previous and
     * next slots from the available player commands itself. Reading transport out of that list by
     * position is what left the shade without a media card at all.
     */
    private fun buildActions(
        mediaSession: MediaSession,
        player: Player,
        actionFactory: MediaNotification.ActionFactory,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        playing: Boolean,
        isLiked: Boolean,
    ): List<NotificationCompat.Action> = buildList {
        add(
            actionFactory.createMediaAction(
                mediaSession,
                IconCompat.createWithResource(context, R.drawable.ic_baseline_skip_previous_40),
                "Previous",
                Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            ),
        )
        add(
            actionFactory.createMediaAction(
                mediaSession,
                IconCompat.createWithResource(
                    context,
                    if (playing) R.drawable.ic_baseline_pause_40
                    else R.drawable.ic_baseline_play_arrow_40,
                ),
                if (playing) "Pause" else "Play",
                Player.COMMAND_PLAY_PAUSE,
            ),
        )
        add(
            actionFactory.createMediaAction(
                mediaSession,
                IconCompat.createWithResource(context, R.drawable.ic_baseline_skip_next_40),
                "Next",
                Player.COMMAND_SEEK_TO_NEXT,
            ),
        )
        for (button in mediaButtonPreferences) {
            val customAction = button.sessionCommand?.customAction ?: continue
            val resolved = when (customAction) {
                AudioPlayerCommands.LIKE, AudioPlayerCommands.UNLIKE ->
                    if (isLiked) AudioPlayerCommandButtons.liked else AudioPlayerCommandButtons.unliked
                else -> button
            }
            add(actionFactory.createCustomActionFromCustomCommandButton(mediaSession, resolved))
        }
    }

    private companion object {
        const val TRANSPORT_ACTION_COUNT = 3
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = false
}
