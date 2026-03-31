package com.github.pakka_papad.home

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.github.pakka_papad.Screens
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

@Composable
fun HomeBottomBar(
    currentScreen: Screens,
    onScreenChange: (Screens) -> Unit,
    @Suppress("UNUSED_PARAMETER") bottomBarColor: Color,
    selectedTabs: List<Int>?,
) {
    if (selectedTabs == null) return
    val screens = Screens.values()
    val scheme = MaterialTheme.colorScheme
    val navBg = HomeLibraryTokens.navBarBackground(scheme)
    val pill = HomeLibraryTokens.navPill(scheme)

    NavigationBar(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .height(UiTokens.bottomBarHeight),
        containerColor = navBg,
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
                            modifier = Modifier.size(UiTokens.iconSizeSmall),
                        )
                    },
                    label = {
                        Text(
                            text = screen.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onSurface,
                        selectedTextColor = scheme.onSurface,
                        indicatorColor = pill,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant,
                    ),
                )
            }
    }
}
