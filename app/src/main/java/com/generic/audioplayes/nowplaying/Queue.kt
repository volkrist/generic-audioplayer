package com.generic.audioplayes.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.generic.audioplayes.R
import com.generic.audioplayes.components.SongCardV2
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.ui.theme.UiTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.Queue(
    queue: List<Song>,
    onFavouriteClicked: (Song) -> Unit,
    currentSong: Song?,
    expanded: Boolean,
    playerHelper: PlayerHelper,
    onDrag: (fromIndex: Int, toIndex: Int) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(key1 = currentSong, key2 = expanded) {
        delay(600)
        if (!expanded) {
            listState.scrollToItem(playerHelper.currentMediaItemIndex)
            return@LaunchedEffect
        }
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(playerHelper.currentMediaItemIndex)
        }
    }
    val dragDropState = rememberDragDropState(listState) { fromIndex, toIndex ->
        onDrag(fromIndex,toIndex)
    }
    val queueRevealAlpha = remember { Animatable(1f) }
    LaunchedEffect(expanded) {
        if (expanded) {
            queueRevealAlpha.snapTo(0.88f)
            queueRevealAlpha.animateTo(
                1f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )
        }
    }
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .align(Alignment.CenterHorizontally)
            .graphicsLayer { alpha = queueRevealAlpha.value }
            .dragContainer(dragDropState),
        state = listState,
        contentPadding = WindowInsets.systemBars
            .only(WindowInsetsSides.Bottom)
            .asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(UiTokens.elevationNone),
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = UiTokens.paddingItem,
                            bottom = UiTokens.paddingItemTight,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(UiTokens.queueSheetDragHandleWidth)
                            .height(UiTokens.queueSheetDragHandleHeight)
                            .clip(RoundedCornerShape(UiTokens.queueSheetDragHandleCorner))
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.35f)),
                    )
                }
                Divider(
                    modifier = Modifier.padding(
                        horizontal = UiTokens.queueDividerHorizontalPadding,
                        vertical = UiTokens.queueDividerVerticalPadding,
                    ),
                    color = scheme.outline.copy(alpha = 0.22f),
                )
                Text(
                    text = stringResource(R.string.queue_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = UiTokens.paddingSheetHorizontal,
                            vertical = UiTokens.paddingSection,
                        ),
                )
            }
        }
        itemsIndexed(
            items = queue,
            key = { _, song -> song.location }
        ) { index, song ->
            val isPlaying = currentSong?.location == song.location
            DraggableItem(dragDropState, index) {
                SongCardV2(
                    song = song,
                    onSongClicked = { if(!isPlaying){ playerHelper.seekTo(index,0) } },
                    onFavouriteClicked = onFavouriteClicked,
                    currentlyPlaying = isPlaying,
                )
            }
        }
    }
}