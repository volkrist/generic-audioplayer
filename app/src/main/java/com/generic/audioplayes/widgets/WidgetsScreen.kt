package com.generic.audioplayes.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.ui.theme.HomeLibraryTokens

/**
 * Widget gallery. The earlier revision rendered its own hand‑rolled top bar + hardcoded
 * gradient that didn't respect system insets, which is why the screenshot showed content
 * drawn behind the status bar and cropped at the bottom. Now the layout:
 *
 *  • Uses [HomeLibraryTokens.libraryShellGradient] for the page background so it matches
 *    Home, Collections, Tag editor, Volume booster, Dictaphone.
 *  • Top bar applies status-bar insets; list uses [navigationBarsPadding] for gesture nav.
 *  • Uses [TopBarWithBackArrow] (the shared top app bar used everywhere else) so typography
 *    and back behaviour are identical.
 *  • Keeps the card styling but pulls accents from [MaterialTheme.colorScheme] so Material
 *    You / AMOLED / light themes all look coherent.
 */
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
    onApplyWidgetStyle: (WidgetStyle) -> Unit,
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

    val accent = MaterialTheme.colorScheme.primary
    val accentSecondary = MaterialTheme.colorScheme.tertiary
    val cardBg = Color(0xFF1E1B4B).copy(alpha = 0.55f)
    val bannerBg = Color(0xFF11122B).copy(alpha = 0.65f)
    val labelMuted = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HomeLibraryTokens.libraryShellGradient)
    ) {
        TopBarWithBackArrow(
            onBackArrowPressed = onBack,
            title = stringResource(R.string.widgets_screen_title),
            actions = {},
            backgroundColor = Color.Transparent,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(bannerBg, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.VolumeUp,
                contentDescription = null,
                tint = accent,
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
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
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 4.dp,
                bottom = 24.dp,
            ),
        ) {
            items(catalog, key = { it.titleRes }) { entry ->
                WidgetCatalogCard(
                    entry = entry,
                    cardBg = cardBg,
                    labelMuted = labelMuted,
                    applyAccent = accentSecondary,
                    addAccent = accent,
                    onApplyStyle = { onApplyWidgetStyle(entry.style) },
                    onAdd = { onRequestPinWidget(entry.useSmallProvider) },
                )
            }
        }
    }
}

@Composable
private fun WidgetCatalogCard(
    entry: WidgetCatalogEntry,
    cardBg: Color,
    labelMuted: Color,
    applyAccent: Color,
    addAccent: Color,
    onApplyStyle: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onApplyStyle,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, applyAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = applyAccent),
                ) {
                    Text(
                        text = stringResource(R.string.widgets_apply_style),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.5.dp, addAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = addAccent),
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
