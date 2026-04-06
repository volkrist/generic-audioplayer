package com.generic.audioplayes.nowplaying

import android.content.res.ColorStateList
import android.widget.SeekBar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.toMS
import kotlinx.coroutines.delay

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(
            factory = { context ->
                SeekBar(context).apply {
                    setOnSeekBarChangeListener(
                        object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(
                                seekBar: SeekBar?,
                                progress: Int,
                                fromUser: Boolean
                            ) {
                                if (fromUser) {
                                    currentValue = progress.toLong()
                                }
                            }

                            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                                isDragging = true
                            }

                            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                                playerHelper.seekTo(currentValue)
                                isDragging = false
                            }
                        }
                    )
                    thumb = resources.getDrawable(R.drawable.seekbar_thumb, null)
                    progressDrawable = resources.getDrawable(R.drawable.progress, null)
                    thumbTintList =
                        colorStateListOf(
                            intArrayOf(android.R.attr.state_enabled) to primaryColor.toArgb(),
                        )
                    progressBackgroundTintList =
                        colorStateListOf(
                            intArrayOf(android.R.attr.state_enabled) to primaryColor.copy(alpha = 0.3f).toArgb(),
                        )
                    progressTintList =
                        colorStateListOf(
                            intArrayOf(android.R.attr.state_enabled) to primaryColor.toArgb(),
                        )
                }
            },
            update = { seekBar ->
                val maxDur = duration.toInt().coerceAtLeast(1)
                seekBar.max = maxDur
                if (!isDragging) {
                    seekBar.progress = currentValue.toInt().coerceIn(0, seekBar.max)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
                text = duration.toMS(),
                fontSize = UiTokens.musicSliderTimeLabelSp,
                color = resolvedLabelColor,
            )
        }
    }
}

fun colorStateListOf(vararg mapping: Pair<IntArray, Int>): ColorStateList {
    val (states, colors) = mapping.unzip()
    return ColorStateList(states.toTypedArray(), colors.toIntArray())
}
