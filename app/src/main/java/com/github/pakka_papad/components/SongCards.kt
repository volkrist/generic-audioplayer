package com.github.pakka_papad.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.github.pakka_papad.R
import com.github.pakka_papad.components.more_options.OptionsAlertDialog
import com.github.pakka_papad.components.more_options.SongOptions
import com.github.pakka_papad.data.music.MiniSong
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.ui.theme.UiTokens
import kotlinx.coroutines.launch

@Composable
private fun SongCardBase(
    song: Song,
    onSongClicked: () -> Unit,
    onFavouriteClicked: (Song) -> Unit,
    currentlyPlaying: Boolean,
    backgroundColor: Color,
    currentlyPlayingBackgroundColor: Color,
    onBackgroundColor: Color,
    onCurrentlyPlayingBackgroundColor: Color,
    songOptions: List<SongOptions>,
) {
    val iconModifier = Modifier.size(UiTokens.iconSizeSmall)
    val spacerModifier = Modifier.width(UiTokens.paddingSection)
    val rowBg = when {
        currentlyPlaying -> currentlyPlayingBackgroundColor
        else -> backgroundColor
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightCompact)
            .padding(horizontal = UiTokens.paddingSection, vertical = UiTokens.playlistTileVerticalPadding)
            .clip(RoundedCornerShape(UiTokens.cornerMedium))
            .background(rowBg)
            .clickable(onClick = onSongClicked)
            .padding(horizontal = UiTokens.paddingItem, vertical = UiTokens.metaSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = Modifier
                .size(UiTokens.artworkMini)
                .clip(RoundedCornerShape(UiTokens.cornerSmall))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
            contentScale = ContentScale.Crop
        )
        Spacer(spacerModifier)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor else onBackgroundColor,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor.copy(alpha = 0.92f)
                else onBackgroundColor.copy(alpha = 0.75f),
                overflow = TextOverflow.Ellipsis,
            )
        }
        val scope = rememberCoroutineScope()
        val favouriteButtonScale = remember { Animatable(1f) }
        Icon(
            imageVector = if (song.favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.favourite_button),
            modifier = iconModifier
                .scale(favouriteButtonScale.value)
                .clickable(
                    onClick = {
                        onFavouriteClicked(song)
                        scope.launch {
                            favouriteButtonScale.animateTo(
                                targetValue = 1.2f,
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutLinearInEasing,
                                )
                            )
                            favouriteButtonScale.animateTo(
                                targetValue = 0.8f,
                                animationSpec = tween(
                                    durationMillis = 200,
                                    easing = LinearEasing,
                                )
                            )
                            favouriteButtonScale.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 100,
                                    easing = FastOutLinearInEasing,
                                )
                            )
                        }
                    },
                    indication = rememberRipple(
                        bounded = false,
                        radius = UiTokens.rippleSmall
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                ),
            tint = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor else onBackgroundColor,
        )
        if (songOptions.isNotEmpty()) {
            Spacer(spacerModifier)
            var optionsVisible by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = iconModifier
                    .clickable(
                        onClick = {
                            optionsVisible = true
                        },
                        indication = rememberRipple(
                            bounded = false,
                            radius = UiTokens.rippleSmall
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor else onBackgroundColor,
            )
            if (optionsVisible) {
                OptionsAlertDialog(
                    options = songOptions,
                    title = song.title,
                    onDismissRequest = {
                        optionsVisible = false
                    }
                )
            }
        }
    }
}

@Composable
fun SongCardV1(
    song: Song,
    onSongClicked: () -> Unit,
    onFavouriteClicked: (Song) -> Unit,
    currentlyPlaying: Boolean = false,
    songOptions: List<SongOptions>,
) = SongCardBase(
    song = song,
    onSongClicked = onSongClicked,
    onFavouriteClicked = onFavouriteClicked,
    currentlyPlaying = currentlyPlaying,
    backgroundColor = Color.Transparent,
    currentlyPlayingBackgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    onBackgroundColor = MaterialTheme.colorScheme.onSurface,
    onCurrentlyPlayingBackgroundColor = MaterialTheme.colorScheme.onSurface,
    songOptions = songOptions,
)

@Composable
fun SongCardV2(
    song: Song,
    onSongClicked: () -> Unit,
    onFavouriteClicked: (Song) -> Unit,
    currentlyPlaying: Boolean = false,
    songOptions: List<SongOptions> = listOf(),
) = SongCardBase(
    song = song,
    onSongClicked = onSongClicked,
    onFavouriteClicked = onFavouriteClicked,
    currentlyPlaying = currentlyPlaying,
    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
    currentlyPlayingBackgroundColor = MaterialTheme.colorScheme.secondary,
    onBackgroundColor = MaterialTheme.colorScheme.onSecondaryContainer,
    onCurrentlyPlayingBackgroundColor = MaterialTheme.colorScheme.onSecondary,
    songOptions = songOptions
)

@Composable
fun SongCardV3(
    song: Song,
    onSongClicked: (Song) -> Unit,
) = Column(
    modifier = Modifier
        .widthIn(max = UiTokens.songCardGridMaxWidth)
        .fillMaxWidth()
        .clickable(onClick = { onSongClicked(song) })
        .padding(UiTokens.gridSpacing),
    horizontalAlignment = Alignment.Start,
) {
    AsyncImage(
        model = song.artUri,
        contentDescription = stringResource(R.string.song_image),
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium),
        contentScale = ContentScale.Crop,
    )
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.paddingItem)
    )
    Text(
        text = song.title,
        maxLines = 1,
        style = MaterialTheme.typography.titleMedium,
        overflow = TextOverflow.Ellipsis
    )
    Text(
        text = song.artist,
        maxLines = 1,
        style = MaterialTheme.typography.titleSmall,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun MiniSongCard(
    song: MiniSong,
    onSongClicked: () -> Unit,
    currentlyPlaying: Boolean = false,
    songOptions: List<SongOptions>,
) {
    val iconModifier = Modifier.size(UiTokens.artworkThumbMini)
    val spacerModifier = Modifier.width(UiTokens.gridSpacing)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightStandard)
            .clickable(onClick = onSongClicked)
            .background(if (currentlyPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .padding(horizontal = UiTokens.gridSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = Modifier
                .size(UiTokens.artworkPlaylistHero)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
        Spacer(spacerModifier)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (songOptions.isNotEmpty()) {
            Spacer(spacerModifier)
            var optionsVisible by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = iconModifier
                    .clickable(
                        onClick = {
                            optionsVisible = true
                        },
                        indication = rememberRipple(
                            bounded = false,
                            radius = UiTokens.rippleSmall
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (optionsVisible) {
                OptionsAlertDialog(
                    options = songOptions,
                    title = song.title,
                    onDismissRequest = {
                        optionsVisible = false
                    }
                )
            }
        }
    }
}