package com.github.pakka_papad.player

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaStyleNotificationHelper
import com.github.pakka_papad.R
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.data.services.QueueService
import com.google.common.collect.ImmutableList
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Builds a [androidx.media3.session.MediaStyleNotificationHelper.MediaStyle] notification for
 * [MediaSession], used by [androidx.media3.session.MediaSessionService] for the notification shade
 * and lock screen. Playback state and metadata come from the session’s player; actions are wired
 * through [MediaNotification.ActionFactory].
 */
@ServiceScoped
@UnstableApi
class PlayerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueService: QueueService,
    private val preferenceProvider: ZenPreferenceProvider,
) : MediaNotification.Provider {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationHelper.ensureNotificationChannel(context)
        }
    }

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

        val largeIcon = NotificationHelper.loadArtworkBitmap(context, metadata?.artworkUri)

        val mediaStyle = MediaStyleNotificationHelper.MediaStyle(mediaSession)
            .setShowActionsInCompactView(1, 2, 3)

        val playing = player.isPlaying
        val isLiked = queueService.getSongAtIndex(player.currentMediaItemIndex)?.favourite ?: false

        Timber.d("PlayerNotificationManager track=$title playing=$playing liked=$isLiked")

        val builder = NotificationCompat.Builder(context, NotificationHelper.PLAYER_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_notification)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(largeIcon)
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

        for ((index, commandButton) in customLayout.withIndex()) {
            val action = when (index) {
                0 -> actionFactory.createCustomActionFromCustomCommandButton(
                    mediaSession,
                    if (isLiked) ZenCommandButtons.liked else ZenCommandButtons.unliked,
                )
                1, 3 -> actionFactory.createMediaAction(
                    mediaSession,
                    IconCompat.createWithResource(context, commandButton.iconResId),
                    commandButton.displayName,
                    commandButton.playerCommand,
                )
                2 -> actionFactory.createMediaAction(
                    mediaSession,
                    IconCompat.createWithResource(
                        context,
                        if (playing) R.drawable.ic_baseline_pause_40
                        else R.drawable.ic_baseline_play_arrow_40,
                    ),
                    if (playing) "Pause" else "Play",
                    commandButton.playerCommand,
                )
                4 -> actionFactory.createCustomActionFromCustomCommandButton(
                    mediaSession,
                    commandButton,
                )
                else -> null
            }
            if (action != null) {
                builder.addAction(action)
            }
        }

        return MediaNotification(NotificationHelper.PLAYER_NOTIFICATION_ID, builder.build())
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = false
}
