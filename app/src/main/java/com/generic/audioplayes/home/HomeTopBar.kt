package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.R
import com.generic.audioplayes.Screens
import com.generic.audioplayes.components.SortOptionChooser
import com.generic.audioplayes.components.getSortOptions
import com.generic.audioplayes.ui.theme.UiTokens

private val topIconTint = Color.White
private val tabInactive = Color.White.copy(alpha = 0.48f)

/** Top strip: main library sections (order matches library shell). */
private val topNavOrder = listOf(
    Screens.Songs,
    Screens.Playlists,
    Screens.Folders,
    Screens.Albums,
    Screens.Artists,
    Screens.Genres,
)

private fun Screens.topNavLabelRu(): String = when (this) {
    Screens.Songs -> "Песни"
    Screens.Albums -> "Альбомы"
    Screens.Artists -> "Артисты"
    Screens.Playlists -> "Плейлисты"
    Screens.Genres -> "Жанры"
    Screens.Folders -> "Папки"
}

@Composable
fun HomeTopBar(
    onMenuClicked: () -> Unit,
    onThemeIconClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    currentScreen: Screens,
    onSortOptionChosen: (currScreen: Int, option: Int) -> Unit,
    currentSortOrder: Map<Int, Int>,
    selectedTabs: List<Int>?,
    onScreenChange: (Screens) -> Unit,
    sectionSubtitle: String,
    onLayoutIconClicked: () -> Unit = {},
    /** When set, Songs tab sort icon opens this instead of the default [SortOptionChooser] dialog. */
    onSongsSortSheetRequest: (() -> Unit)? = null,
    /** When set, Folders tab sort icon opens this instead of the default [SortOptionChooser] dialog. */
    onFoldersSortSheetRequest: (() -> Unit)? = null,
) {
    var sortMenuVisible by remember { mutableStateOf(false) }
    val options by remember(currentScreen.ordinal) {
        derivedStateOf { currentScreen.getSortOptions() }
    }
    val openDrawerLabel = stringResource(R.string.open_navigation_drawer)

    val visibleTabs by remember(selectedTabs) {
        derivedStateOf {
            if (selectedTabs == null) {
                topNavOrder
            } else {
                topNavOrder.filter { tab -> selectedTabs.contains(tab.ordinal) }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                IconButton(
                    onClick = onMenuClicked,
                    modifier = Modifier.semantics { contentDescription = openDrawerLabel },
                ) {
                    TwoLineMenuIcon(
                        tint = topIconTint,
                        modifier = Modifier.padding(4.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6D00)),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Outlined.Palette,
                contentDescription = stringResource(R.string.drawer_graphic_theme),
                modifier = Modifier
                    .size(44.dp)
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = onThemeIconClicked,
                    ),
                tint = topIconTint,
            )

            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "search-btn",
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = onSearchClicked,
                    ),
                tint = topIconTint,
            )

            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "settings-btn",
                modifier = Modifier
                    .size(44.dp)
                    .padding(10.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = onSettingsClicked,
                    ),
                tint = topIconTint,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            visibleTabs.forEach { screen ->
                val selected = currentScreen == screen

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onScreenChange(screen) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(
                            id = if (selected) screen.filledIcon else screen.outlinedIcon,
                        ),
                        contentDescription = null,
                        tint = if (selected) Color.White else tabInactive,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = screen.topNavLabelRu(),
                        color = if (selected) Color.White else tabInactive,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(if (selected) 3.dp else 0.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (selected) Color.White else Color.Transparent),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = sectionSubtitle,
                color = Color.White.copy(alpha = if (currentScreen == Screens.Songs) 1f else 0.92f),
                style = if (currentScreen == Screens.Songs) {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
                } else {
                    MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (currentScreen == Screens.Songs) 6.dp else 0.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_sort_40),
                    contentDescription = stringResource(R.string.library_section_sort),
                    modifier = Modifier
                        .size(30.dp)
                        .padding(4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                            onClick = {
                                when {
                                    currentScreen == Screens.Songs && onSongsSortSheetRequest != null ->
                                        onSongsSortSheetRequest()
                                    currentScreen == Screens.Folders && onFoldersSortSheetRequest != null ->
                                        onFoldersSortSheetRequest()
                                    else -> sortMenuVisible = true
                                }
                            },
                        ),
                    tint = Color.White,
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(22.dp)
                        .background(Color.White.copy(alpha = 0.22f)),
                )

                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_list_24),
                    contentDescription = "layout",
                    modifier = Modifier
                        .size(30.dp)
                        .padding(4.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                            onClick = onLayoutIconClicked,
                        ),
                    tint = Color.White,
                )
            }
        }
    }

    if (sortMenuVisible) {
        SortOptionChooser(
            options = options,
            selectedOption = currentSortOrder[currentScreen.ordinal] ?: options.first().ordinal,
            onOptionSelect = { option ->
                onSortOptionChosen(currentScreen.ordinal, option)
                sortMenuVisible = false
            },
            onChooserDismiss = { sortMenuVisible = false },
        )
    }
}
