package com.generic.audioplayes.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.components.FullScreenSadMessage
import com.generic.audioplayes.R
import com.generic.audioplayes.components.MiniSongCard
import com.generic.audioplayes.components.more_options.FolderOptions
import com.generic.audioplayes.components.more_options.OptionsAlertDialog
import com.generic.audioplayes.components.more_options.SongOptions
import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.storage_explorer.Directory
import com.generic.audioplayes.storage_explorer.DirectoryContents

@Composable
fun Files(
    contents: DirectoryContents,
    onDirectoryClicked: (Directory) -> Unit,
    onSongClicked: (index: Int) -> Unit,
    currentSong: Song?,
    onAddToPlaylistClicked: (MiniSong) -> Unit,
    onAddToQueueClicked: (MiniSong) -> Unit,
    onFolderAddToBlacklistRequest: (Directory) -> Unit,
    /** Same track count as when opening a folder: [com.generic.audioplayes.data.music.SongExtractor.extractMini]. */
    folderTrackCount: (String) -> Int,
) {
    if (contents.directories.isEmpty() && contents.songs.isEmpty()){
        FullScreenSadMessage(stringResource(R.string.nothing_here))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = UiTokens.paddingScreen),
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
    ){
        items(
            items = contents.directories,
            key = { it.absolutePath }
        ){
            Folder(
                folder = it,
                onDirectoryClicked = onDirectoryClicked,
                folderTrackCount = folderTrackCount,
                options = listOf(
                    FolderOptions.Blacklist { onFolderAddToBlacklistRequest(it) }
                )
            )
        }
        itemsIndexed(
            items = contents.songs,
            key = { index, song -> song.location }
        ){index, song ->
            MiniSongCard(
                song = song,
                onSongClicked = { onSongClicked(index) },
                songOptions = listOf(
                    SongOptions.AddToPlaylist{ onAddToPlaylistClicked(song) },
                    SongOptions.AddToQueue{ onAddToQueueClicked(song) },
                ),
                currentlyPlaying = (song.location == currentSong?.location),
                shellListStyle = true,
            )
        }
    }
}

@Composable
fun Folder(
    folder: Directory,
    onDirectoryClicked: (Directory) -> Unit,
    folderTrackCount: (String) -> Int,
    options: List<FolderOptions>,
) {
    val resource = painterResource(R.drawable.ic_baseline_folder_40)
    var showClickIndicator by remember { mutableStateOf(false) }
    var showFileMenu by remember { mutableStateOf(false) }
    var songCount by remember(folder.absolutePath) { mutableStateOf<Int?>(null) }
    LaunchedEffect(folder.absolutePath) {
        songCount = withContext(Dispatchers.IO) {
            folderTrackCount(folder.absolutePath)
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                onDirectoryClicked(folder)
                showClickIndicator = true
            }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = resource,
            contentDescription = stringResource(R.string.folder_icon),
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFFFC107),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            songCount?.let { n ->
                Text(
                    text = formatTrackCountRu(n),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    maxLines = 1,
                )
            }
        }
        if (showClickIndicator) {
            CircularProgressIndicator(
                modifier = Modifier.size(26.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu_button),
                modifier = Modifier
                    .size(26.dp)
                    .clickable(
                        onClick = {
                            showFileMenu = true
                        },
                        indication = rememberRipple(
                            bounded = false,
                            radius = 20.dp
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                tint = Color.White.copy(alpha = 0.9f),
            )
        }
    }
    if (showFileMenu) {
        OptionsAlertDialog(
            options = options,
            title = folder.name,
            onDismissRequest = {
                showFileMenu = false
            }
        )
    }
}

private fun formatTrackCountRu(n: Int): String = when {
    n % 100 in 11..14 -> "$n песен"
    n % 10 == 1 -> "$n песня"
    n % 10 in 2..4 -> "$n песни"
    else -> "$n песен"
}