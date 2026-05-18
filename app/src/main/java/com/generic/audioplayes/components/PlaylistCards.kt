package com.generic.audioplayes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.components.more_options.OptionsAlertDialog
import com.generic.audioplayes.util.Stage4DebugLog
import com.generic.audioplayes.components.more_options.PlaylistOptions
import com.generic.audioplayes.data.music.PlaylistWithSongCount
import com.generic.audioplayes.ui.theme.UiTokens

@Composable
private fun BasePlaylistCard(
    onCardClicked: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .height(UiTokens.listItemHeightTall)
        .padding(UiTokens.paddingSection)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.secondaryContainer)
        .clickable(onClick = onCardClicked)
        .padding(horizontal = UiTokens.paddingSection),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingSection),
    content = content
)

@Composable
private fun PlaylistName(
    playlist: PlaylistWithSongCount,
) = Text(
    text = playlist.playlistName,
    color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier = Modifier.background(Color.Transparent),
    style = MaterialTheme.typography.titleMedium
)

@Composable
fun SelectablePlaylistCard(
    playlist: PlaylistWithSongCount,
    isSelected: Boolean,
    onSelectChange: () -> Unit,
) = BasePlaylistCard(
    onCardClicked = onSelectChange,
    content = {
        PlaylistName(
            playlist = playlist,
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Check mark",
                modifier = Modifier.size(UiTokens.artworkThumbSmall),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
)

@Composable
fun PlaylistCard(
    playlistWithSongCount: PlaylistWithSongCount,
    onPlaylistClicked: (Long) -> Unit,
    options: List<PlaylistOptions> = listOf(),
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightStandard)
            .clickable(onClick = { onPlaylistClicked(playlistWithSongCount.playlistId) })
            .padding(horizontal = UiTokens.gridSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playlistWithSongCount.playlistName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${playlistWithSongCount.count} ${if (playlistWithSongCount.count == 1) "song" else "songs"}",
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis
            )
        }
        var optionsVisible by remember { mutableStateOf(false) }
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = null,
            modifier = Modifier
                .size(UiTokens.artworkThumbMini)
                .clickable(
                    onClick = {
                        Stage4DebugLog.i(
                            "Playlist overflow clicked playlist=${playlistWithSongCount.playlistName}",
                        )
                        optionsVisible = true
                    },
                    indication = rememberRipple(
                        bounded = false,
                        radius = UiTokens.rippleSmall
                    ),
                    interactionSource = remember { MutableInteractionSource() }
                )
        )
        if (optionsVisible) {
            OptionsAlertDialog(
                options = options,
                title = playlistWithSongCount.playlistName,
                onDismissRequest = { optionsVisible = false }
            )
        }
    }
}

@Composable
fun PlaylistCardV2(
    playlistWithSongCount: PlaylistWithSongCount,
    onPlaylistClicked: (Long) -> Unit,
    options: List<PlaylistOptions> = listOf(),
) {
    var optionsVisible by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .widthIn(max = UiTokens.gridCardMaxWidth)
            .clickable(onClick = { onPlaylistClicked(playlistWithSongCount.playlistId) })
            .padding(horizontal = UiTokens.metaSpacingSmall, vertical = UiTokens.paddingItem),
        verticalArrangement = Arrangement.spacedBy(UiTokens.gridSpacing)
    ) {
        AsyncImage(
            model = playlistWithSongCount.artUri,
            contentDescription = stringResource(R.string.playlist_art),
            modifier = Modifier
                .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = false)
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.cornerExtraLarge))
                .background(Color.White.copy(alpha = 0.12f)),
            contentScale = ContentScale.Crop,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiTokens.textLineGapTight),
            ) {
                Text(
                    text = playlistWithSongCount.playlistName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Text(
                    text = pluralStringResource(
                        id = R.plurals.song_count,
                        count = playlistWithSongCount.count,
                        playlistWithSongCount.count,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = Modifier
                    .size(UiTokens.artworkThumbMini)
                    .clickable(
                        onClick = {
                            optionsVisible = true
                        },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleSmall),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = Color.White.copy(alpha = 0.85f),
            )
        }
        if (optionsVisible) {
            OptionsAlertDialog(
                options = options,
                title = playlistWithSongCount.playlistName,
                onDismissRequest = { optionsVisible = false }
            )
        }
    }
}

/**
 * Full-width list row: square art, title, song count, overflow (Playlists tab list layout).
 */
@Composable
fun PlaylistUserPlaylistRow(
    playlistWithSongCount: PlaylistWithSongCount,
    onPlaylistClicked: (Long) -> Unit,
    options: List<PlaylistOptions> = listOf(),
) {
    var optionsVisible by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightTall)
            .padding(horizontal = UiTokens.paddingScreen, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = { onPlaylistClicked(playlistWithSongCount.playlistId) },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleLarge),
                    interactionSource = remember { MutableInteractionSource() },
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = playlistWithSongCount.artUri,
                contentDescription = stringResource(R.string.playlist_art),
                modifier = Modifier
                    .size(UiTokens.playlistListRowArt)
                    .clip(RoundedCornerShape(UiTokens.cornerMedium))
                    .background(Color.White.copy(alpha = 0.12f)),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(UiTokens.textLineGapTight),
            ) {
                Text(
                    text = playlistWithSongCount.playlistName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                )
                Text(
                    text = stringResource(
                        R.string.playlist_songs_count_fmt,
                        playlistWithSongCount.count,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = stringResource(R.string.more_menu_button),
            modifier = Modifier
                .size(UiTokens.iconSizeTouch)
                .padding(10.dp)
                .clickable(
                    onClick = { optionsVisible = true },
                    indication = rememberRipple(bounded = false, radius = UiTokens.rippleSmall),
                    interactionSource = remember { MutableInteractionSource() },
                ),
            tint = Color.White.copy(alpha = 0.85f),
        )
    }
    if (optionsVisible) {
        OptionsAlertDialog(
            options = options,
            title = playlistWithSongCount.playlistName,
            onDismissRequest = { optionsVisible = false },
        )
    }
}

