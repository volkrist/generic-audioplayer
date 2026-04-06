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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.widgets.WidgetLayoutFamily
import com.generic.audioplayes.widgets.WidgetStyle
import com.generic.audioplayes.widgets.layoutFamily
import com.generic.audioplayes.widgets.miniPlayerBackgroundBrush

private val miniPlayerArtSize = 52.dp
private val miniPlayFabSize = 48.dp
private val practicalArtSize = 56.dp
private val iconStyleArtSize = 36.dp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiniPlayer(
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    song: Song?,
    widgetStyle: WidgetStyle,
    modifier: Modifier = Modifier,
    /** Tap artwork + title to open full player (play/queue keep their own actions). */
    onExpandPlayer: () -> Unit = {},
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

    val brush = widgetStyle.miniPlayerBackgroundBrush()
    val family = widgetStyle.layoutFamily()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(brush),
    ) {
        when (family) {
            WidgetLayoutFamily.CLASSIC_ROW ->
                MiniPlayerClassicRow(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.LITE_CENTER,
            WidgetLayoutFamily.STANDARD_CENTER,
            WidgetLayoutFamily.STYLISH_CENTER,
            ->
                MiniPlayerCenteredColumn(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.VINYL_ROW ->
                MiniPlayerVinylRow(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.PRACTICAL_GRID ->
                MiniPlayerPracticalRow(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.SIMPLE_COMPACT ->
                MiniPlayerSimpleCompact(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.ROUND_DARK ->
                MiniPlayerRoundDark(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.MINI_STACK ->
                MiniPlayerMiniStack(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.CARD_STACK ->
                MiniPlayerCardStack(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
            WidgetLayoutFamily.ICON_MINI ->
                MiniPlayerIconMini(
                    song = song,
                    subtitle = subtitle,
                    labelPrimary = labelPrimary,
                    labelSecondary = labelSecondary,
                    showPlayButton = showPlayButton,
                    onPausePlayPressed = onPausePlayPressed,
                    onQueueClick = onQueueClick,
                    onExpandPlayer = onExpandPlayer,
                )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerClassicRow(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
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
                modifier = Modifier
                    .clickable(
                        onClick = onExpandPlayer,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                    ),
            ) {
                MiniPlayerCircleArt(song = song, size = miniPlayerArtSize)
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
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerCenteredColumn(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
            MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerVinylRow(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = UiTokens.miniPlayerVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = song.location,
            transitionSpec = { fadeIn(tween(220)) with fadeOut(tween(180)) },
            label = "miniVinyl",
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(miniPlayerArtSize)
                        .clip(shape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF212121), Color(0xFF424242), Color(0xFF212121)),
                            ),
                        ),
                ) {
                    AsyncImage(
                        model = song.artUri,
                        contentDescription = stringResource(R.string.song_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
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
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerPracticalRow(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedContent(
            targetState = song.location,
            transitionSpec = { fadeIn(tween(220)) with fadeOut(tween(180)) },
            label = "miniPractical",
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            ) {
                Box(modifier = Modifier.size(practicalArtSize)) {
                    AsyncImage(
                        model = song.artUri,
                        contentDescription = stringResource(R.string.song_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1E1E2E)),
                        contentScale = ContentScale.Crop,
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(practicalArtSize),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelSecondary,
                    )
                }
            }
        }
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerSimpleCompact(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelPrimary,
                textAlign = TextAlign.Start,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelSecondary,
            )
        }
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerRoundDark(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = UiTokens.miniPlayerVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedContent(
            targetState = song.location,
            transitionSpec = { fadeIn(tween(220)) with fadeOut(tween(180)) },
            label = "miniRound",
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(miniPlayerArtSize)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF4FC3F7))),
                        ),
                ) {
                    AsyncImage(
                        model = song.artUri,
                        contentDescription = stringResource(R.string.song_image),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
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
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = labelSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Color(0xFFFFB800).copy(alpha = 0.65f)),
                    )
                }
            }
        }
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerMiniStack(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(
                onClick = onExpandPlayer,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
            ),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = labelPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelSecondary,
                modifier = Modifier.weight(1f),
            )
            MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
            MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerCardStack(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(
                onClick = onExpandPlayer,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
            ),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelPrimary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = labelSecondary,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.28f), RoundedCornerShape(1.dp)),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
            Spacer(Modifier.width(8.dp))
            MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun MiniPlayerIconMini(
    song: Song,
    subtitle: String,
    labelPrimary: Color,
    labelSecondary: Color,
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    onQueueClick: () -> Unit,
    onExpandPlayer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onExpandPlayer,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = Color.White.copy(alpha = 0.12f)),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(iconStyleArtSize)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFE64A19), Color(0xFFFF5722)))),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = stringResource(R.string.song_image),
                    modifier = Modifier
                        .size(iconStyleArtSize - 6.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = labelPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = labelSecondary,
                )
            }
        }
        MiniPlayerPlayFab(showPlayButton = showPlayButton, onPausePlayPressed = onPausePlayPressed)
        MiniPlayerQueueIcon(onQueueClick = onQueueClick, tint = labelPrimary)
    }
}

@Composable
private fun MiniPlayerCircleArt(song: Song, size: Dp) {
    Box(modifier = Modifier.size(size)) {
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
}

@Composable
private fun MiniPlayerPlayFab(
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
) {
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
}

@Composable
private fun MiniPlayerQueueIcon(
    onQueueClick: () -> Unit,
    tint: Color,
) {
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
        tint = tint,
    )
}
