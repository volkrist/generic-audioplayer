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
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.ImageProvider as UriImageProvider
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.generic.audioplayes.Constants
import com.generic.audioplayes.MainActivity
import com.generic.audioplayes.R
import com.generic.audioplayes.player.AudioPlayerBroadcastReceiver

private val MediumAlbumArtSize = 56.dp

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
            MediumWidgetContent()
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
        val style = parseWidgetStyle()
        val layoutFamily = style.layoutFamily()
        val titleStyle = TextStyle(
            color = ColorProvider(style.glanceOnWidgetTitleColor()),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        val artistStyle = TextStyle(
            color = ColorProvider(style.glanceOnWidgetSubtitleColor()),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        )

        val baseModifier = GlanceModifier
            .fillMaxSize()
            .then(widgetBackground(style))
            .padding(12.dp)

        when (layoutFamily) {
            WidgetLayoutFamily.LITE_CENTER,
            WidgetLayoutFamily.STANDARD_CENTER,
            WidgetLayoutFamily.STYLISH_CENTER,
            -> {
                Column(
                    modifier = baseModifier,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        MediumAlbumArt(
                            imageUri = imageUri,
                            style = style,
                            openApp = openApp,
                        )
                        Spacer(GlanceModifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Horizontal.Start) {
                            Text(
                                text = title,
                                style = titleStyle,
                                maxLines = 1,
                                modifier = GlanceModifier.clickable(openApp),
                            )
                            Text(
                                text = artist,
                                style = artistStyle,
                                maxLines = 1,
                                modifier = GlanceModifier.clickable(openApp),
                            )
                        }
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically,
                        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                    ) {
                        TransportControls(isPlaying = isPlaying, style = style)
                    }
                }
            }
            else -> {
                Row(modifier = baseModifier) {
                    MediumAlbumArt(
                        imageUri = imageUri,
                        style = style,
                        openApp = openApp,
                    )
                    Spacer(GlanceModifier.width(12.dp))
                    Column(modifier = GlanceModifier.fillMaxSize()) {
                        Box(modifier = GlanceModifier.fillMaxWidth().clickable(openApp)) {
                            Column {
                                Text(
                                    text = title,
                                    style = titleStyle,
                                    maxLines = 1,
                                )
                                Text(
                                    text = artist,
                                    style = artistStyle,
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
                            TransportControls(isPlaying = isPlaying, style = style)
                        }
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun MediumAlbumArt(
        imageUri: String,
        style: WidgetStyle,
        openApp: Action,
    ) {
        Box(
            modifier = GlanceModifier
                .width(MediumAlbumArtSize)
                .height(MediumAlbumArtSize)
                .then(mediumAlbumShape(style))
                .clickable(openApp),
        ) {
            Image(
                provider = albumArtProvider(imageUri),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }

    private fun albumArtProvider(imageUri: String): ImageProvider =
        if (imageUri.isNotBlank()) UriImageProvider(imageUri.toUri())
        else ImageProvider(R.drawable.ic_outline_music_note_40)

    @androidx.compose.runtime.Composable
    private fun mediumAlbumShape(style: WidgetStyle): GlanceModifier {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return GlanceModifier
        return when (style.layoutFamily()) {
            WidgetLayoutFamily.VINYL_ROW -> GlanceModifier.cornerRadius(MediumAlbumArtSize / 2)
            else -> GlanceModifier.cornerRadius(16.dp)
        }
    }

    @androidx.compose.runtime.Composable
    private fun TransportControls(isPlaying: Boolean, style: WidgetStyle) {
        val tint = ColorProvider(style.glanceOnWidgetIconTint())
        val mod = GlanceModifier.width(40.dp).height(40.dp)
        Image(
            provider = ImageProvider(R.drawable.ic_baseline_skip_previous_40),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = mod.clickable { pendingPreviousIntent.send() },
        )
        Spacer(GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(
                if (isPlaying) R.drawable.ic_baseline_pause_40 else R.drawable.ic_baseline_play_arrow_40,
            ),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = mod.clickable { pendingPausePlayIntent.send() },
        )
        Spacer(GlanceModifier.width(6.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_baseline_skip_next_40),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = mod.clickable { pendingNextIntent.send() },
        )
    }

    @androidx.compose.runtime.Composable
    private fun parseWidgetStyle(): WidgetStyle {
        val raw = currentState(widgetStyleKey) ?: ""
        return try {
            WidgetStyle.valueOf(raw)
        } catch (_: Exception) {
            WidgetStyle.CLASSIC
        }
    }

    @androidx.compose.runtime.Composable
    private fun widgetBackground(style: WidgetStyle): GlanceModifier {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            GlanceModifier
                .cornerRadius(28.dp)
                .background(ColorProvider(style.glanceBackgroundColor()))
        } else {
            GlanceModifier.background(ImageProvider(R.drawable.music_widget_background))
        }
    }
}

class PlayerWidgetProvider : PlayerWidgetReceiverBase() {
    override val glanceAppWidget: GlanceAppWidget
        get() = PlayerWidget
}
