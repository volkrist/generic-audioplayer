package com.generic.audioplayes.widgets

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.ImageProvider as UriImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.generic.audioplayes.Constants
import com.generic.audioplayes.MainActivity
import com.generic.audioplayes.R
import com.generic.audioplayes.player.AudioPlayerBroadcastReceiver

/**
 * Medium / resizable widget: artwork, title, artist, transport controls. Tap artwork or text to open the app.
 */
object PlayerWidget : GlanceAppWidget() {

    private lateinit var pendingPausePlayIntent: PendingIntent
    private lateinit var pendingPreviousIntent: PendingIntent
    private lateinit var pendingNextIntent: PendingIntent

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        pendingPausePlayIntent = PendingIntent.getBroadcast(
            context, AudioPlayerBroadcastReceiver.PAUSE_PLAY_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_PAUSE_PLAY,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )
        pendingPreviousIntent = PendingIntent.getBroadcast(
            context, AudioPlayerBroadcastReceiver.PREVIOUS_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_PREVIOUS,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )
        pendingNextIntent = PendingIntent.getBroadcast(
            context, AudioPlayerBroadcastReceiver.NEXT_ACTION_REQUEST_CODE,
            Intent(Constants.PACKAGE_NAME).putExtra(
                AudioPlayerBroadcastReceiver.AUDIO_CONTROL,
                AudioPlayerBroadcastReceiver.AUDIO_PLAYER_NEXT,
            ),
            PendingIntent.FLAG_IMMUTABLE,
        )
        provideContent {
            GlanceTheme {
                MediumWidgetContent()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MediumWidgetContent() {
        val context = LocalContext.current
        val openApp = actionStartActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
        )
        val imageUri = currentState(imageUriKey) ?: ""
        val title = currentState(titleKey) ?: ""
        val artist = currentState(artistKey) ?: ""
        val isPlaying = currentState(isPlayingKey) ?: false

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .then(widgetBackground())
                .padding(12.dp),
        ) {
            Box(
                modifier = GlanceModifier
                    .wrapContentWidth()
                    .fillMaxHeight()
                    .then(albumCorner())
                    .clickable(openApp),
            ) {
                Image(
                    provider = UriImageProvider(imageUri.toUri()),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxHeight(),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Box(modifier = GlanceModifier.fillMaxWidth().clickable(openApp)) {
                    Column {
                        Text(
                            text = title,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                        )
                        Text(
                            text = artist,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSecondaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                            ),
                            maxLines = 1,
                        )
                    }
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    TransportControls(isPlaying = isPlaying)
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun TransportControls(isPlaying: Boolean) {
        val mod = GlanceModifier.width(40.dp).height(40.dp)
        Image(
            provider = ImageProvider(R.drawable.ic_baseline_skip_previous_40),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            modifier = mod.clickable { pendingPreviousIntent.send() },
        )
        Spacer(GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(
                if (isPlaying) R.drawable.ic_baseline_pause_40 else R.drawable.ic_baseline_play_arrow_40,
            ),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            modifier = mod.clickable { pendingPausePlayIntent.send() },
        )
        Spacer(GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_baseline_skip_next_40),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
            modifier = mod.clickable { pendingNextIntent.send() },
        )
    }

    @androidx.compose.runtime.Composable
    private fun widgetBackground(): GlanceModifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceModifier
                .cornerRadius(28.dp)
                .background(GlanceTheme.colors.secondaryContainer)
        } else {
            GlanceModifier.background(ImageProvider(R.drawable.music_widget_background))
        }
    }

    @androidx.compose.runtime.Composable
    private fun albumCorner(): GlanceModifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceModifier.cornerRadius(16.dp)
        } else {
            GlanceModifier
        }
    }
}

class PlayerWidgetProvider : PlayerWidgetReceiverBase() {
    override val glanceAppWidget: GlanceAppWidget
        get() = PlayerWidget
}
