package com.generic.audioplayes.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.window.DialogProperties
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.components.SongCardHomeSongsRow
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.nowplaying.HomeLibrarySongActionsBottomSheet
import com.generic.audioplayes.formatToDate
import com.generic.audioplayes.ui.theme.HomeLibraryTokens

private val homePlayShuffleHeight = 52.dp
private val homePlayShuffleCorner = 26.dp
private val songsAccentOrange = Color(0xFFFF9800)

@Composable
private fun HomeSongsPlayShuffleCards(
    onPlayAllClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp > 340
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(homePlayShuffleHeight)
                .clip(RoundedCornerShape(homePlayShuffleCorner))
                .clickable(onClick = onShuffleClicked),
            shape = RoundedCornerShape(homePlayShuffleCorner),
            color = Color(0xFF1A1A22),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (wide) Arrangement.Start else Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_shuffle_40),
                    contentDescription = stringResource(R.string.shuffle_button),
                    modifier = Modifier.size(26.dp),
                    tint = songsAccentOrange,
                )
                if (wide) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.songs_home_shuffle),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(homePlayShuffleHeight)
                .clip(RoundedCornerShape(homePlayShuffleCorner))
                .clickable(onClick = onPlayAllClicked),
            shape = RoundedCornerShape(homePlayShuffleCorner),
            color = Color(0xFF1A1A22),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (wide) Arrangement.Start else Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_play_arrow_40),
                    contentDescription = stringResource(R.string.play_all_button),
                    modifier = Modifier.size(26.dp),
                    tint = songsAccentOrange,
                )
                if (wide) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.songs_home_play),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f),
                    )
                }
            }
        }
    }
}

@Composable
fun AllSongs(
    songs: List<Song>?,
    onSongClicked: (index: Int) -> Unit,
    listState: LazyListState,
    onFavouriteClicked: (Song) -> Unit,
    currentSong: Song?,
    onAddToQueueClicked: (Song) -> Unit,
    onPlayAllClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onAddToPlaylistsClicked: (Song) -> Unit,
    onPlayLibrarySongNext: (Song) -> Unit,
    onOpenAlbum: (Song) -> Unit,
    onPlayerActionEditTags: (Song) -> Unit,
    onPlayerActionHideSong: (Song) -> Unit,
    onPlayerActionDeleteSong: (Song) -> Unit,
    onPlayerActionRingtone: (Song) -> Unit,
    onPlayerActionChangeCover: (Song) -> Unit,
) {
    if (songs == null) return
    if (songs.isEmpty()) {
        FullScreenSadMessage(
            message = stringResource(R.string.no_songs_found_on_this_device),
            paddingValues = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues()
        )
    } else {
        var trackSheetSong by remember { mutableStateOf<Song?>(null) }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HomeLibraryTokens.contentHorizontalPadding),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues()
        ) {
            item {
                HomeSongsPlayShuffleCards(
                    onPlayAllClicked = onPlayAllClicked,
                    onShuffleClicked = onShuffleClicked,
                )
            }
            itemsIndexed(
                items = songs,
                key = { index, song ->
                    song.location
                }
            ) { index, song ->
                SongCardHomeSongsRow(
                    song = song,
                    onSongClicked = {
                        onSongClicked(index)
                    },
                    onFavouriteClicked = onFavouriteClicked,
                    onOverflowMenuClick = { trackSheetSong = song },
                    currentlyPlaying = (song.location == currentSong?.location)
                )
            }
        }
        trackSheetSong?.let { sheetSong ->
            HomeLibrarySongActionsBottomSheet(
                song = sheetSong,
                visible = true,
                onDismiss = { trackSheetSong = null },
                onPlayNext = { onPlayLibrarySongNext(sheetSong) },
                onAddToQueue = { onAddToQueueClicked(sheetSong) },
                onAddToPlaylist = { onAddToPlaylistsClicked(sheetSong) },
                onOpenAlbum = { onOpenAlbum(sheetSong) },
                onPlayerActionEditTags = onPlayerActionEditTags,
                onPlayerActionHideSong = onPlayerActionHideSong,
                onPlayerActionDeleteSong = onPlayerActionDeleteSong,
                onPlayerActionRingtone = onPlayerActionRingtone,
                onPlayerActionChangeCover = onPlayerActionChangeCover,
            )
        }
    }
}

@Composable
fun SongInfo(
    song: Song,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        properties = DialogProperties(

        ),
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = song.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onDismissRequest,
            ) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        },
        text = {
            val spanStyle = SpanStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize*(1.1f)
            )
            val scrollState = rememberScrollState()
            Text(
                text = buildAnnotatedString {
                    append("${stringResource(R.string.location)}\n")
                    withStyle(spanStyle) { append(song.location) }
                    append("\n\n${stringResource(R.string.size)}\n")
                    withStyle(spanStyle) { append(song.size) }
                    append("\n\n${stringResource(R.string.album)}\n")
                    withStyle(spanStyle) { append(song.album) }
                    append("\n\n${stringResource(R.string.artist)}\n")
                    withStyle(spanStyle) { append(song.artist) }
                    append("\n\n${stringResource(R.string.album_artist)}\n")
                    withStyle(spanStyle) { append(song.albumArtist) }
                    append("\n\n${stringResource(R.string.composer)}\n")
                    withStyle(spanStyle) { append(song.composer) }
                    append("\n\n${stringResource(R.string.lyricist)}\n")
                    withStyle(spanStyle) { append(song.lyricist) }
                    append("\n\n${stringResource(R.string.genre)}\n")
                    withStyle(spanStyle) { append(song.genre) }
                    append("\n\n${stringResource(R.string.year)}\n")
                    withStyle(spanStyle) { append(if (song.year == 0) stringResource(R.string.unknown) else song.year.toString()) }
                    append("\n\n${stringResource(R.string.duration)}\n")
                    withStyle(spanStyle) { append(if (song.durationMillis == 0L) stringResource(R.string.unknown) else song.durationFormatted) }
                    append("\n\n${stringResource(R.string.play_count)}\n")
                    withStyle(spanStyle) { append(song.playCount.toString()) }
                    append("\n\n${stringResource(R.string.last_played_on)}\n")
                    withStyle(spanStyle) { append(if (song.lastPlayed == null) stringResource(R.string.never) else song.lastPlayed.formatToDate()) }
                    append("\n\n${stringResource(R.string.mime_type)}\n")
                    withStyle(spanStyle) { append(song.mimeType ?: stringResource(R.string.unknown)) }
                },
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxWidth()
            )
        }
    )
}

