package com.generic.audioplayes.widgets

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val goldPlayTop = Color(0xFFFFF176)
private val goldPlayBot = Color(0xFFFF9800)

@Composable
fun MiniPlayButton(modifier: Modifier = Modifier, paused: Boolean = false) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(
                Brush.verticalGradient(listOf(goldPlayTop, goldPlayBot)),
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (paused) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
    }
}

@Composable
fun WidgetStylePreview(style: WidgetStyle, modifier: Modifier = Modifier) {
    when (style) {
        WidgetStyle.CLASSIC -> PreviewClassic(modifier)
        WidgetStyle.LITE -> PreviewLite(modifier)
        WidgetStyle.VINYL -> PreviewVinyl(modifier)
        WidgetStyle.SIMPLE -> PreviewSimple(modifier)
        WidgetStyle.ROUND -> PreviewRound(modifier)
        WidgetStyle.MINI -> PreviewMini(modifier)
        WidgetStyle.STANDARD -> PreviewStandard(modifier)
        WidgetStyle.CARD -> PreviewCard(modifier)
        WidgetStyle.PRACTICAL -> PreviewPractical(modifier)
        WidgetStyle.STYLISH -> PreviewStylish(modifier)
        WidgetStyle.ICON -> PreviewIcon(modifier)
    }
}

@Composable
private fun PreviewClassic(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFF2E7D32)),
                ),
            )
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF3E2723)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    "Love Story",
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(10.dp))
            Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(10.dp))
            MiniPlayButton()
            Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(10.dp))
            Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
    }
}

@Composable
private fun PreviewLite(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFF6A1B9A)),
                ),
            )
            .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Love Story",
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(10.dp))
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(10.dp))
                MiniPlayButton()
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(10.dp))
                Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
private fun PreviewVinyl(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFFFF8F00)),
                ),
            )
            .padding(4.dp),
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF212121), Color(0xFF424242), Color(0xFF212121)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF5D4037)),
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("Taylor Swift", color = Color.White.copy(alpha = 0.85f), fontSize = 6.sp)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    MiniPlayButton()
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewSimple(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFFAD1457), Color(0xFF6A1B9A))),
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Love Story",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(12.dp))
                MiniPlayButton(Modifier.size(18.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun PreviewRound(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1A2E))
            .padding(4.dp),
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF4FC3F7))),
                    ),
            )
            Column(Modifier.weight(1f)) {
                Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                Text("Taylor Swift", color = Color.White.copy(alpha = 0.75f), fontSize = 6.sp)
                Spacer(Modifier.height(3.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(Color(0xFFFFB800).copy(alpha = 0.7f)),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(9.dp))
                    MiniPlayButton(Modifier.size(14.dp))
                    Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(9.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewMini(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF004D40), Color(0xFF00695C))),
            )
            .padding(5.dp),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(12.dp))
                MiniPlayButton(Modifier.size(20.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

@Composable
private fun PreviewStandard(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF311B92), Color(0xFF6A1B9A))),
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(9.dp))
                MiniPlayButton(Modifier.size(14.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
private fun PreviewCard(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFFBF360C), Color(0xFFE65100), Color(0xFFFF6E40))),
            )
            .padding(5.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Text("Taylor Swift", color = Color.White.copy(alpha = 0.9f), fontSize = 6.sp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(1.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(9.dp))
                MiniPlayButton(Modifier.size(14.dp), paused = true)
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
private fun PreviewPractical(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF263238), Color(0xFF37474F))),
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF212121), Color(0xFF424242)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF0277BD), Color(0xFF81D4FA))),
                        ),
                )
            }
            Text("Love Story", color = Color.White, fontSize = 6.sp, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(8.dp))
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(8.dp))
                MiniPlayButton(Modifier.size(12.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(8.dp))
                Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun PreviewStylish(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFF6F00), Color(0xFF6A1B9A), Color(0xFF311B92)),
                ),
            )
            .padding(5.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.25f)),
            )
            Text("Love Story", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color(0xFFFFB800).copy(alpha = 0.8f), RoundedCornerShape(1.dp)),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(9.dp))
                MiniPlayButton(Modifier.size(14.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(9.dp))
                Icon(Icons.Outlined.Queue, null, tint = Color.White, modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
private fun PreviewIcon(modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFFE64A19), Color(0xFFFF5722))),
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .background(Color(0xFFFFC107), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.MusicNote, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
