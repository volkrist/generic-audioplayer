package com.github.pakka_papad.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.pakka_papad.R
import com.github.pakka_papad.components.FullScreenSadMessage
import com.github.pakka_papad.components.more_options.GenreOptions
import com.github.pakka_papad.components.more_options.OptionsAlertDialog
import com.github.pakka_papad.data.music.GenreWithSongCount
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.listItemHeightCompact)
            .clip(RoundedCornerShape(UiTokens.cornerMedium))
            .clickable(onClick = { onGenreClicked(genreWithSongCount) })
            .padding(horizontal = UiTokens.paddingSection, vertical = UiTokens.metaSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = genreWithSongCount.genreName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    )
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
