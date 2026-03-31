package com.github.pakka_papad.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.pakka_papad.R
import com.github.pakka_papad.ui.theme.HomeLibraryTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeNavigationDrawer(
    drawerState: DrawerState,
    onItemClick: (DrawerMenuDestination) -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = HomeLibraryTokens.barBackground(MaterialTheme.colorScheme),
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.drawer_header_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                )
                Spacer(Modifier.height(8.dp))
                for (item in DrawerMenuDestination.values()) {
                    NavigationDrawerItem(
                        icon = { DrawerMenuIcon(item) },
                        label = { Text(stringResource(item.titleRes)) },
                        selected = false,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun DrawerMenuIcon(item: DrawerMenuDestination) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    val modifier = Modifier.size(24.dp)
    when (item) {
        DrawerMenuDestination.Library -> Icon(
            painter = painterResource(R.drawable.ic_outline_library_music_40),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.Settings -> Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.Equalizer -> Icon(
            painter = painterResource(R.drawable.ic_baseline_piano_40),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.SleepTimer -> Icon(
            painter = painterResource(R.drawable.outline_timer_24),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.GraphicTheme -> Icon(
            painter = painterResource(R.drawable.baseline_palette_40),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.Widgets -> Icon(
            painter = painterResource(R.drawable.ic_baseline_queue_music_40),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.VolumeBooster -> Icon(
            painter = painterResource(R.drawable.baseline_speed_24),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
        DrawerMenuDestination.Dictaphone -> Icon(
            painter = painterResource(R.drawable.baseline_send_40),
            contentDescription = null,
            modifier = modifier,
            tint = tint,
        )
    }
}

@Composable
fun TwoLineMenuIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.size(24.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.55f)
                .height(2.dp)
                .background(tint, RoundedCornerShape(1.dp)),
        )
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .height(2.dp)
                .background(tint, RoundedCornerShape(1.dp)),
        )
    }
}
