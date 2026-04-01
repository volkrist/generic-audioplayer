package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.Screens
import com.generic.audioplayes.ui.theme.UiTokens

private val homeNavIconSize = 28.dp
private val navBarCorner = 28.dp

private val bottomNavBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0x991A0D3A),
        Color(0xB34A1570),
        Color(0xCC1A237E),
    ),
)

/** Bottom nav labels (shell copy, matches reference). */
private fun Screens.bottomLabelRu(): String = when (this) {
    Screens.Songs -> "Песни"
    Screens.Albums -> "Альбомы"
    Screens.Artists -> "Артисты"
    Screens.Playlists -> "Плейлисты"
    Screens.Genres -> "Жанры"
    Screens.Folders -> "Папки"
}

@Composable
fun HomeBottomBar(
    currentScreen: Screens,
    onScreenChange: (Screens) -> Unit,
    @Suppress("UNUSED_PARAMETER") bottomBarColor: Color,
    selectedTabs: List<Int>?,
) {
    if (selectedTabs == null) return
    val screens = Screens.values()
    val pill = Color.White.copy(alpha = 0.32f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(navBarCorner))
            .background(bottomNavBrush)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .height(UiTokens.bottomNavHeight),
            containerColor = Color.Transparent,
            tonalElevation = UiTokens.elevationNone,
        ) {
            selectedTabs.filter { it >= 0 && it < screens.size }
                .map { screens[it] }
                .forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onScreenChange(screen) },
                        icon = {
                            Icon(
                                painter = painterResource(screen.filledIcon),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(homeNavIconSize)
                                    .padding(2.dp),
                                tint = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                            )
                        },
                        label = {
                            Text(
                                text = screen.bottomLabelRu(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    letterSpacing = 0.06.sp,
                                ),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = pill,
                            unselectedIconColor = Color.White.copy(alpha = 0.55f),
                            unselectedTextColor = Color.White.copy(alpha = 0.55f),
                        ),
                    )
                }
        }
    }
}
