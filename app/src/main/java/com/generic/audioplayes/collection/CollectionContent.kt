package com.generic.audioplayes.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.components.SongCardV1
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.home.PlayShuffleCard


fun LazyListScope.collectionContent(
    songs: List<Song>,
    onSongClicked: (index: Int) -> Unit,
    onSongFavouriteClicked: (Song) -> Unit,
    currentSong: Song?,
    onPlayAllClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
    onTrackOverflowClick: (Song) -> Unit,
) {
    item {
        PlayShuffleCard(
            onPlayAllClicked = onPlayAllClicked,
            onShuffleClicked = onShuffleClicked,
        )
    }
    itemsIndexed(
        items = songs,
        key = { _, song ->
            song.location
        }
    ) { index, song ->
        SongCardV1(
            song = song,
            onSongClicked = {
                onSongClicked(index)
            },
            onFavouriteClicked = onSongFavouriteClicked,
            songOptions = emptyList(),
            onOverflowClick = { onTrackOverflowClick(song) },
            currentlyPlaying = (song.location == currentSong?.location)
        )
    }
}

@Composable
fun CollectionImage(
    imageUri: String? = "",
    title: String? = ""
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = imageUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            text = title ?: "",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                    )
                )
                .padding(10.dp),
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold,
            overflow = TextOverflow.Ellipsis
        )
    }
}