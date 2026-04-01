package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.components.more_options.GenreOptions
import com.generic.audioplayes.components.more_options.OptionsAlertDialog
import com.generic.audioplayes.data.music.GenreWithSongCount
import com.generic.audioplayes.ui.theme.HomeLibraryTokens
import com.generic.audioplayes.ui.theme.UiTokens

private val genreTilePalette = listOf(
    Color(0xFF5C6BC0),
    Color(0xFF26A69A),
    Color(0xFFAB47BC),
    Color(0xFF42A5F5),
    Color(0xFFFF7043),
    Color(0xFF78909C),
    Color(0xFF7CB342),
)

private fun genreTileColor(name: String): Color {
    val idx = name.hashCode().let { if (it < 0) -it else it } % genreTilePalette.size
    return genreTilePalette[idx]
}

@Composable
fun Genres(
    genresWithSongCount: List<GenreWithSongCount>,
    listState: LazyListState,
    onGenreClicked: (GenreWithSongCount) -> Unit,
) {
    if (genresWithSongCount.isEmpty()) {
        FullScreenSadMessage(
            message = stringResource(R.string.nothing_found)
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = HomeLibraryTokens.contentHorizontalPadding),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ) {
        items(
            items = genresWithSongCount,
            key = { it.genreName }
        ) {
            GenreCard(
                genreWithSongCount = it,
                onGenreClicked = onGenreClicked
            )
        }
    }
}

@Composable
fun GenreCard(
    genreWithSongCount: GenreWithSongCount,
    onGenreClicked: (GenreWithSongCount) -> Unit,
    options: List<GenreOptions> = listOf(),
) {
    val tile = genreTileColor(genreWithSongCount.genreName)
    val letter = genreWithSongCount.genreName.trim().take(1).uppercase().ifEmpty { "?" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = { onGenreClicked(genreWithSongCount) })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = genreWithSongCount.genreName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = pluralStringResource(
                    id = R.plurals.song_count,
                    count = genreWithSongCount.count,
                    genreWithSongCount.count
                ),
                maxLines = 1,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.72f),
            )
        }
        if (options.isNotEmpty()) {
            var optionsVisible by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = Modifier
                    .size(UiTokens.artworkThumbMini)
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
                tint = Color.White.copy(alpha = 0.85f),
            )
            if (optionsVisible) {
                OptionsAlertDialog(
                    options = options,
                    title = genreWithSongCount.genreName,
                    onDismissRequest = { optionsVisible = false }
                )
            }
        }
    }
}
