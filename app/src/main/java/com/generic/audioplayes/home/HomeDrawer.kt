package com.generic.audioplayes.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R

private val drawerGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0B1028),
        Color(0xFF12102A),
        Color(0xFF1E0D38),
        Color(0xFF2D0D45),
        Color(0xFF3D1454),
    ),
)

private val drawerItemColors @Composable get() = NavigationDrawerItemDefaults.colors(
    selectedContainerColor = Color.White.copy(alpha = 0.08f),
    unselectedContainerColor = Color.Transparent,
    selectedTextColor = Color.White,
    unselectedTextColor = Color.White,
    selectedIconColor = Color.White,
    unselectedIconColor = Color.White,
)

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
                modifier = Modifier
                    .fillMaxHeight()
                    .drawBehind {
                        drawRect(brush = drawerGradient, size = size)
                        val line = Color(0x6680C8FF)
                        val sw = 1.2.dp.toPx()
                        drawArc(
                            color = line,
                            startAngle = 195f,
                            sweepAngle = 75f,
                            useCenter = false,
                            topLeft = Offset(-size.width * 0.08f, -size.height * 0.02f),
                            size = Size(size.width * 0.58f, size.height * 0.28f),
                            style = Stroke(width = sw),
                        )
                        drawArc(
                            color = line.copy(alpha = 0.45f),
                            startAngle = 175f,
                            sweepAngle = 65f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.12f, size.height * 0.02f),
                            size = Size(size.width * 0.42f, size.height * 0.22f),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        drawArc(
                            color = line.copy(alpha = 0.35f),
                            startAngle = 160f,
                            sweepAngle = 50f,
                            useCenter = false,
                            topLeft = Offset(-size.width * 0.02f, size.height * 0.06f),
                            size = Size(size.width * 0.35f, size.height * 0.18f),
                            style = Stroke(width = 0.8.dp.toPx()),
                        )
                    },
                drawerContainerColor = Color.Transparent,
                drawerContentColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.drawer_header_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    DrawerHeaderLogo(Modifier.size(46.dp))
                }
                Spacer(Modifier.height(16.dp))
                for (item in DrawerMenuDestination.values()) {
                    NavigationDrawerItem(
                        icon = { DrawerMenuIcon(item) },
                        label = { Text(stringResource(item.titleRes)) },
                        selected = false,
                        onClick = { onItemClick(item) },
                        modifier = Modifier.padding(horizontal = 10.dp),
                        colors = drawerItemColors,
                    )
                }
            }
        },
        content = content,
    )
}

@Composable
private fun DrawerHeaderLogo(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(1.dp)
                .background(Color(0xFFFFC107), CircleShape),
        )
        Box(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
                .background(Color(0xFFFF9800), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DrawerMenuIcon(item: DrawerMenuDestination) {
    val tint = Color.White
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
            imageVector = Icons.Outlined.Palette,
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
