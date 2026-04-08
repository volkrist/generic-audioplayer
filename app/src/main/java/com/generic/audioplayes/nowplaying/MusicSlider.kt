package com.generic.audioplayes.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.toMS
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

@Composable
fun MusicSlider(
    modifier: Modifier,
    playerHelper: PlayerHelper,
    currentSongPlaying: Boolean?,
    song: Song, // to update slider when song is changed in paused state
    duration: Long,
    timeLabelColor: Color = Color.Unspecified,
) {
    var currentValue by remember { mutableStateOf(playerHelper.currentPosition.toLong()) }
    /** While true, do not sync from player — allows smooth thumb drag without fighting the UI. */
    var isDragging by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)
                currentValue = 0L
                isDragging = false
            }
        }
        playerHelper.addListener(listener)
        onDispose {
            playerHelper.removeListener(listener)
        }
    }

    LaunchedEffect(currentSongPlaying, song.location, isDragging) {
        if (isDragging) return@LaunchedEffect
        if (currentSongPlaying == true) {
            while (true) {
                currentValue = playerHelper.currentPosition.toLong()
                delay(33)
            }
        } else {
            currentValue = playerHelper.currentPosition.toLong()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val resolvedLabelColor =
        if (timeLabelColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else timeLabelColor

    val durMs = run {
        val fromMeta = duration.takeIf { it > 0 }
        val fromPlayer = playerHelper.duration.takeIf { it > 0f && it.isFinite() }?.toLong()
        (fromMeta ?: fromPlayer ?: 1L).coerceAtLeast(1L)
    }
    val progress = (currentValue.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Slider(
            value = progress,
            onValueChange = { newValue ->
                isDragging = true
                currentValue = (newValue * durMs.toFloat()).roundToLong().coerceIn(0L, durMs)
            },
            onValueChangeFinished = {
                playerHelper.seekTo(currentValue)
                isDragging = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = primaryColor,
                activeTrackColor = primaryColor,
                inactiveTrackColor = primaryColor.copy(alpha = 0.3f),
            ),
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentValue.toMS(),
                fontSize = UiTokens.musicSliderTimeLabelSp,
                color = resolvedLabelColor,
            )
            Text(
                text = durMs.toMS(),
                fontSize = UiTokens.musicSliderTimeLabelSp,
                color = resolvedLabelColor,
            )
        }
    }
}
