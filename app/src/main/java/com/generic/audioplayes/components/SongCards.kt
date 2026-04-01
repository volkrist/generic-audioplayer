package com.generic.audioplayes.components

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
import com.generic.audioplayes.R
import com.generic.audioplayes.components.more_options.OptionsAlertDialog
import com.generic.audioplayes.components.more_options.SongOptions
import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class SongRowLayout {
    /** Dense rows (queue, playlist editor). */
    Compact,
    /** Home / library song list: roomier, larger art. */
    HomeLibrary,
}

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
    layout: SongRowLayout = SongRowLayout.Compact,
) {
    val scheme = MaterialTheme.colorScheme
    val isHome = layout == SongRowLayout.HomeLibrary
    val rowHeight = if (isHome) 92.dp else UiTokens.listItemHeightCompact
    val artSize = if (isHome) 64.dp else UiTokens.artworkMini
    val artCorner = if (isHome) UiTokens.cornerMedium else UiTokens.cornerSmall
    val rowCorner = if (isHome) UiTokens.cornerLarge else UiTokens.cornerMedium
    val horizontalOuter = if (isHome) 4.dp else UiTokens.paddingSection
    val verticalOuter = if (isHome) 6.dp else UiTokens.playlistTileVerticalPadding
    val horizontalInner = if (isHome) 14.dp else UiTokens.paddingItem
    val verticalInner = if (isHome) 10.dp else UiTokens.metaSpacingSmall
    val iconModifier = Modifier.size(if (isHome) 28.dp else UiTokens.iconSizeSmall)
    val spacerModifier = Modifier.width(if (isHome) 14.dp else UiTokens.paddingSection)
    val rowBg = when {
        currentlyPlaying -> currentlyPlayingBackgroundColor
        else -> backgroundColor
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .padding(horizontal = horizontalOuter, vertical = verticalOuter)
            .clip(RoundedCornerShape(rowCorner))
            .background(rowBg)
            .clickable(onClick = onSongClicked)
            .padding(horizontal = horizontalInner, vertical = verticalInner),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = Modifier
                .size(artSize)
                .clip(RoundedCornerShape(artCorner))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.surfaceVariant,
                            scheme.surface,
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
                style = if (isHome) {
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.15.sp,
                    )
                } else {
                    MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                },
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor else onBackgroundColor,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                maxLines = 1,
                style = if (isHome) {
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                    )
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                modifier = Modifier.fillMaxWidth(),
                color = if (currentlyPlaying) onCurrentlyPlayingBackgroundColor.copy(alpha = 0.92f)
                else onBackgroundColor.copy(alpha = if (isHome) 0.78f else 0.75f),
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

private fun Song.dateAddedShortLabel(): String {
    if (dateAddedSec > 0L) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateAddedSec * 1000 }
        return SimpleDateFormat("MM-dd", Locale.getDefault()).format(cal.time)
    }
    return ""
}

/**
 * Flat list row for Home → Songs tab (reference: title / artist–album / date / actions, no heavy card chrome).
 */
@Composable
fun SongCardHomeSongsRow(
    song: Song,
    onSongClicked: () -> Unit,
    onFavouriteClicked: (Song) -> Unit,
    currentlyPlaying: Boolean = false,
    songOptions: List<SongOptions>,
) {
    val scheme = MaterialTheme.colorScheme
    val artSize = 56.dp
    val artCorner = UiTokens.cornerSmall
    val onColor = Color.White
    val playingBg = Color.White.copy(alpha = 0.08f)
    val rowBg = if (currentlyPlaying) playingBg else Color.Transparent
    val subtitle = buildString {
        append(song.artist)
        if (song.album.isNotBlank() && song.album != "Unknown") {
            append(" — ")
            append(song.album)
        }
    }
    val dateStr = song.dateAddedShortLabel()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UiTokens.listItemHeightSongsHome)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(UiTokens.cornerSmall))
            .background(rowBg)
            .clickable(onClick = onSongClicked)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = Modifier
                .size(artSize)
                .clip(RoundedCornerShape(artCorner))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            scheme.surfaceVariant,
                            scheme.surface,
                        ),
                    ),
                ),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                ),
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                color = onColor,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle.ifBlank { stringResource(R.string.unknown) },
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                color = onColor.copy(alpha = 0.52f),
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (dateStr.isNotBlank()) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = onColor.copy(alpha = 0.45f),
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        val scope = rememberCoroutineScope()
        val favouriteButtonScale = remember { Animatable(1f) }
        val iconMod = Modifier.size(26.dp)
        Icon(
            imageVector = if (song.favourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(R.string.favourite_button),
            modifier = iconMod
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
            tint = onColor.copy(alpha = 0.85f),
        )
        Spacer(modifier = Modifier.width(4.dp))
        if (songOptions.isNotEmpty()) {
            var optionsVisible by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = iconMod
                    .clickable(
                        onClick = { optionsVisible = true },
                        indication = rememberRipple(
                            bounded = false,
                            radius = UiTokens.rippleSmall
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = onColor.copy(alpha = 0.85f),
            )
            if (optionsVisible) {
                OptionsAlertDialog(
                    options = songOptions,
                    title = song.title,
                    onDismissRequest = { optionsVisible = false }
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
    backgroundColor = Color.White.copy(alpha = 0.1f),
    currentlyPlayingBackgroundColor = Color.White.copy(alpha = 0.22f),
    onBackgroundColor = Color.White,
    onCurrentlyPlayingBackgroundColor = Color.White,
    songOptions = songOptions,
    layout = SongRowLayout.HomeLibrary,
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
    /** Folders tab on library gradient: translucent row, light text — no solid [ColorScheme.surface] slab. */
    shellListStyle: Boolean = false,
) {
    val iconModifier = Modifier.size(UiTokens.artworkThumbMini)
    val spacerModifier = Modifier.width(UiTokens.gridSpacing)
    val rowBg = if (shellListStyle) {
        if (currentlyPlaying) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.07f)
    } else {
        if (currentlyPlaying) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    }
    val primaryText = if (shellListStyle) {
        Color.White
    } else {
        if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    }
    val secondaryText = if (shellListStyle) {
        Color.White.copy(alpha = 0.65f)
    } else {
        if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    }
    val menuTint = if (shellListStyle) {
        Color.White.copy(alpha = 0.88f)
    } else {
        if (currentlyPlaying) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (shellListStyle) {
                    Modifier
                        .padding(vertical = 4.dp)
                        .height(UiTokens.listItemHeightStandard)
                        .clip(RoundedCornerShape(UiTokens.cornerMedium))
                        .background(rowBg)
                } else {
                    Modifier
                        .height(UiTokens.listItemHeightStandard)
                        .background(rowBg)
                },
            )
            .clickable(onClick = onSongClicked)
            .padding(horizontal = if (shellListStyle) 12.dp else UiTokens.gridSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri,
            contentDescription = stringResource(R.string.song_image),
            modifier = Modifier
                .size(UiTokens.artworkPlaylistHero)
                .clip(RoundedCornerShape(if (shellListStyle) UiTokens.cornerSmall else UiTokens.cornerMedium)),
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
                color = primaryText,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                color = secondaryText,
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
                tint = menuTint,
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