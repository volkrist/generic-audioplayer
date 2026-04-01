package com.generic.audioplayes.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens

@Composable
fun Albums(
    albums: List<Album>?,
    gridState: LazyGridState,
    onAlbumClicked: (Album) -> Unit
) {
    if (albums == null) return
    if (albums.isEmpty()) {
        FullScreenSadMessage(
            message = stringResource(R.string.oops_no_albums_found),
            paddingValues = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        )
    } else {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = HomeLibraryTokens.contentHorizontalPadding),
            state = gridState,
            columns = GridCells.Adaptive(HomeLibraryTokens.gridMinSize),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        ) {
            items(
                items = albums,
                key = { it.name }
            ) { album ->
                AlbumCard(
                    album = album,
                    onAlbumClicked = onAlbumClicked
                )
            }
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    onAlbumClicked: (Album) -> Unit,
) {
        Column(
        modifier = Modifier
            .widthIn(max = UiTokens.gridCardMaxWidth)
            .fillMaxWidth()
            .clickable { onAlbumClicked(album) }
            .padding(vertical = UiTokens.gridVerticalSpacing),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = album.albumArtUri,
            contentDescription = stringResource(R.string.album_art),
            modifier = Modifier
                .aspectRatio(ratio = 1f, matchHeightConstraintsFirst = false)
                .fillMaxWidth()
                .clip(RoundedCornerShape(UiTokens.cornerExtraLarge)),
            contentScale = ContentScale.Crop,
        )
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.White,
        )
    }
}