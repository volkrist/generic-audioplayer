package com.generic.audioplayes.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.generic.audioplayes.Constants
import com.generic.audioplayes.MainActivity
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Channel creation, artwork loading, and [PendingIntent]s for media playback notifications.
 */
object NotificationHelper {

    const val PLAYER_NOTIFICATION_ID = 20
    const val PLAYER_CHANNEL_ID = "audio_player_playback"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(context: Context) {
        val channel = NotificationChannel(
            PLAYER_CHANNEL_ID,
            "Playback",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Music playback controls and lock screen"
            setShowBadge(false)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    fun contentActivityIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Swipe-to-dismiss and “clear” should stop playback like the in-notification close action. */
    fun stopPlaybackPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            AudioPlayerBroadcastReceiver.CANCEL_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_CANCEL,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )

    fun loadArtworkBitmap(context: Context, artworkUri: Uri?, maxEdgePx: Int = 320): Bitmap? {
        if (artworkUri == null) return null
        return try {
            context.contentResolver.openInputStream(artworkUri)?.use { stream ->
                val decoded = BitmapFactory.decodeStream(stream) ?: return null
                scaleToMaxEdge(decoded, maxEdgePx)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxEdgePx && h <= maxEdgePx) return bitmap
        val ratio = minOf(maxEdgePx.toFloat() / w, maxEdgePx.toFloat() / h)
        val nw = max(1, (w * ratio).roundToInt())
        val nh = max(1, (h * ratio).roundToInt())
        return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
    }
}
