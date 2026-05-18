package com.generic.audioplayes.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import com.generic.audioplayes.data.UserPreferences.Accent
import com.generic.audioplayes.ui.theme.GraphicWallpaper
import com.generic.audioplayes.ui.theme.graphicAccentSwatchBrush

private val screenBg = Brush.verticalGradient(
    0f to Color(0xFF060818),
    0.45f to Color(0xFF0E1430),
    1f to Color(0xFF1A0F2E),
)

private val labelOnBg = Color(0xFFF8F8FC)
private val sectionMuted = Color(0xFF9AA3C4)
private val checkYellow = Color(0xFFFFD54F)
private val cardStrokeSubtle = Color.White.copy(alpha = 0.10f)

private data class ColorSlot(
    val accent: Accent,
    val variant: Int,
    val premium: Boolean,
)

private val colorSlots: List<ColorSlot> = listOf(
    ColorSlot(Accent.JacksonsPurple, 0, true),
    ColorSlot(Accent.Malibu, 0, true),
    ColorSlot(Accent.Magenta, 0, false),
    ColorSlot(Accent.Elm, 0, false),
    ColorSlot(Accent.Melrose, 0, true),
    ColorSlot(Accent.Default, 0, false),
    ColorSlot(Accent.JacksonsPurple, 1, false),
    ColorSlot(Accent.Malibu, 1, false),
    ColorSlot(Accent.Magenta, 1, true),
    ColorSlot(Accent.Elm, 1, false),
    ColorSlot(Accent.Melrose, 1, false),
    ColorSlot(Accent.Default, 1, false),
    ColorSlot(Accent.JacksonsPurple, 2, false),
    ColorSlot(Accent.Malibu, 2, true),
    ColorSlot(Accent.Magenta, 2, false),
)

@Composable
fun ThemeScreen(
    viewModel: ThemeViewModel,
    onPickCustomWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        SectionTitle(text = stringResource(R.string.graphic_theme_color_section))

        Spacer(Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            colorSlots.chunked(5).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    row.forEachIndexed { colIdx, slot ->
                        val globalIndex = rowIndex * 5 + colIdx
                        ColorSwatch(
                            brush = graphicAccentSwatchBrush(slot.accent, slot.variant),
                            selected = themePreference.graphicColorSlot == globalIndex,
                            premium = slot.premium,
                            onClick = {
                                viewModel.updateTheme(
                                    themePreference.copy(
                                        accent = slot.accent,
                                        graphicColorSlot = globalIndex,
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        SectionTitle(text = stringResource(R.string.graphic_theme_tab_all))
        Text(
            text = stringResource(R.string.graphic_theme_wallpapers_hint),
            color = sectionMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )

        val wallpaperCells = buildList {
            add(WallpaperCell.Personalization)
            GraphicWallpaper.allPresetIds().forEach { add(WallpaperCell.Preset(it)) }
        }

        wallpaperCells.chunked(3).forEach { rowCells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowCells.forEach { cell ->
                    when (cell) {
                        is WallpaperCell.Personalization -> {
                            WallpaperPersonalizationCard(
                                selected = themePreference.graphicWallpaperCustomUri.isNotBlank(),
                                onClick = onPickCustomWallpaper,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        is WallpaperCell.Preset -> {
                            val presetId = cell.id
                            val selected = themePreference.graphicWallpaperCustomUri.isBlank() &&
                                themePreference.graphicWallpaperPreset == presetId
                            WallpaperPresetCard(
                                brush = GraphicWallpaper.shellBrush(presetId),
                                selected = selected,
                                onClick = {
                                    viewModel.updateTheme(
                                        themePreference.copy(
                                            graphicWallpaperPreset = presetId,
                                            graphicWallpaperCustomUri = "",
                                        ),
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                repeat(3 - rowCells.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = labelOnBg,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.2.sp,
    )
}

private sealed class WallpaperCell {
    object Personalization : WallpaperCell()
    data class Preset(val id: Int) : WallpaperCell()
}

@Composable
private fun ColorSwatch(
    brush: Brush,
    selected: Boolean,
    premium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    Box(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .size(56.dp)
            .clip(shape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = shape,
            )
            .background(brush, shape)
            .clickable(onClick = onClick),
    ) {
        if (premium) {
            Text(
                text = "👑",
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp),
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .size(20.dp)
                    .background(checkYellow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color(0xFF1A1028),
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun WallpaperPersonalizationCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val grad = Brush.linearGradient(
        listOf(Color(0xFF4A148C), Color(0xFF1565C0), Color(0xFF0D47A1)),
    )
    Card(
        modifier = modifier
            .aspectRatio(0.58f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 6.dp else 3.dp,
        ),
        border = if (selected) {
            BorderStroke(2.dp, Color.White)
        } else {
            BorderStroke(1.dp, cardStrokeSubtle)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(grad)
                .clickable(onClick = onClick)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.graphic_theme_personalization),
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun WallpaperPresetCard(
    brush: Brush,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = modifier.aspectRatio(0.58f),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 6.dp else 3.dp,
        ),
        border = if (selected) {
            BorderStroke(2.dp, Color.White)
        } else {
            BorderStroke(1.dp, cardStrokeSubtle)
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(brush)
                .clickable(onClick = onClick),
        )
    }
}
