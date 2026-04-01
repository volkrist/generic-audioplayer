package com.generic.audioplayes.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens

private val miniPlayerArtSize = 52.dp
private val miniPlayFabSize = 48.dp

private val miniPlayerBarBrush = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF1A237E),
        Color(0xFF4A148C),
        Color(0xFFAD1457),
    ),
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiniPlayer(
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    song: Song?,
    modifier: Modifier = Modifier,
) {
    if (song == null) return
    val labelPrimary = Color.White
    val labelSecondary = Color.White.copy(alpha = 0.72f)
    val subtitle = buildString {
        if (song.artist.isNotBlank()) append(song.artist)
        if (song.album.isNotBlank()) {
            if (isNotEmpty()) append(" - ")
            append(song.album)
        }
    }.ifBlank { stringResource(R.string.unknown) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(miniPlayerBarBrush),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.miniPlayerRowHeight)
                .padding(
                    horizontal = 12.dp,
                    vertical = UiTokens.miniPlayerVerticalPadding,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AnimatedContent(
                targetState = song.location,
                transitionSpec = {
                    fadeIn(tween(220)) with fadeOut(tween(180))
                },
                label = "miniPlayerTrack",
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(miniPlayerArtSize)) {
                        AsyncImage(
                            model = song.artUri,
                            contentDescription = stringResource(R.string.song_image),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E1E2E)),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color(0xFF2A2A38), CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(miniPlayerArtSize),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.08.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = labelPrimary,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = labelSecondary,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .size(miniPlayFabSize)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable(
                        onClick = onPausePlayPressed,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = true, radius = 24.dp),
                    )
                    .padding(11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(
                        if (showPlayButton) R.drawable.ic_baseline_play_arrow_40 else R.drawable.ic_baseline_pause_40,
                    ),
                    contentDescription = stringResource(
                        if (showPlayButton) R.string.play_button else R.string.pause_button,
                    ),
                    modifier = Modifier.size(26.dp),
                    tint = Color.White,
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_baseline_queue_music_40),
                contentDescription = stringResource(R.string.queue_button),
                modifier = Modifier
                    .size(44.dp)
                    .padding(8.dp)
                    .clickable(
                        onClick = onQueueClick,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = 22.dp),
                    ),
                tint = labelPrimary,
            )
        }
    }
}
