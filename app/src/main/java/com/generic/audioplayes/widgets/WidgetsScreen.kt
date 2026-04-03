package com.generic.audioplayes.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.R

private val screenBg = Brush.verticalGradient(
    0f to Color(0xFF070A18),
    0.45f to Color(0xFF12102A),
    1f to Color(0xFF1A0B28),
)

private val bannerBg = Color(0xFF1A1528).copy(alpha = 0.92f)
private val cardBg = Color(0xFF2D2340).copy(alpha = 0.92f)
private val labelMuted = Color(0xFFB0B8D0)
private val accentGold = Color(0xFFFFB800)

private data class WidgetCatalogEntry(
    @StringRes val titleRes: Int,
    val cols: Int,
    val rows: Int,
    val useSmallProvider: Boolean,
    val style: WidgetStyle,
    val previewWidth: Dp,
    val previewHeight: Dp,
)

@Composable
fun WidgetsScreen(
    onBack: () -> Unit,
    onRequestPinWidget: (useSmallProvider: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val catalog = remember {
        listOf(
            WidgetCatalogEntry(R.string.widget_style_classic, 4, 1, false, WidgetStyle.CLASSIC, 132.dp, 44.dp),
            WidgetCatalogEntry(R.string.widget_style_lite, 4, 1, false, WidgetStyle.LITE, 132.dp, 48.dp),
            WidgetCatalogEntry(R.string.widget_style_vinyl, 4, 2, false, WidgetStyle.VINYL, 132.dp, 72.dp),
            WidgetCatalogEntry(R.string.widget_style_simple, 2, 1, false, WidgetStyle.SIMPLE, 100.dp, 56.dp),
            WidgetCatalogEntry(R.string.widget_style_round, 4, 2, false, WidgetStyle.ROUND, 132.dp, 56.dp),
            WidgetCatalogEntry(R.string.widget_style_mini, 2, 2, false, WidgetStyle.MINI, 88.dp, 88.dp),
            WidgetCatalogEntry(R.string.widget_style_standard, 4, 2, false, WidgetStyle.STANDARD, 132.dp, 52.dp),
            WidgetCatalogEntry(R.string.widget_style_card, 3, 2, false, WidgetStyle.CARD, 120.dp, 72.dp),
            WidgetCatalogEntry(R.string.widget_style_practical, 4, 4, false, WidgetStyle.PRACTICAL, 120.dp, 120.dp),
            WidgetCatalogEntry(R.string.widget_style_stylish, 4, 4, false, WidgetStyle.STYLISH, 120.dp, 100.dp),
            WidgetCatalogEntry(R.string.widget_style_icon, 1, 1, true, WidgetStyle.ICON, 48.dp, 48.dp),
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B1020))
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.back_button),
                tint = Color.White,
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
            )
            Text(
                text = stringResource(R.string.widgets_screen_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .background(bannerBg, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.widgets_instruction),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(catalog, key = { it.titleRes }) { entry ->
                WidgetCatalogCard(
                    entry = entry,
                    onAdd = { onRequestPinWidget(entry.useSmallProvider) },
                )
            }
        }
    }
}

@Composable
private fun WidgetCatalogCard(
    entry: WidgetCatalogEntry,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = entry.previewHeight)
            .background(cardBg, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .width(entry.previewWidth)
                .height(entry.previewHeight),
            contentAlignment = Alignment.Center,
        ) {
            WidgetStylePreview(
                style = entry.style,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Text(
                text = stringResource(entry.titleRes),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.widgets_size_label, entry.cols, entry.rows),
                color = labelMuted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, accentGold),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentGold,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.widgets_add),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}
