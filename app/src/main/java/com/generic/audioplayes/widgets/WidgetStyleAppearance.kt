package com.generic.audioplayes.widgets

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Layout family for mini player / medium widget (catalog previews as reference). */
enum class WidgetLayoutFamily {
    CLASSIC_ROW,
    LITE_CENTER,
    VINYL_ROW,
    SIMPLE_COMPACT,
    ROUND_DARK,
    MINI_STACK,
    STANDARD_CENTER,
    CARD_STACK,
    PRACTICAL_GRID,
    STYLISH_CENTER,
    ICON_MINI,
}

fun WidgetStyle.layoutFamily(): WidgetLayoutFamily = when (this) {
    WidgetStyle.CLASSIC -> WidgetLayoutFamily.CLASSIC_ROW
    WidgetStyle.LITE -> WidgetLayoutFamily.LITE_CENTER
    WidgetStyle.VINYL -> WidgetLayoutFamily.VINYL_ROW
    WidgetStyle.SIMPLE -> WidgetLayoutFamily.SIMPLE_COMPACT
    WidgetStyle.ROUND -> WidgetLayoutFamily.ROUND_DARK
    WidgetStyle.MINI -> WidgetLayoutFamily.MINI_STACK
    WidgetStyle.STANDARD -> WidgetLayoutFamily.STANDARD_CENTER
    WidgetStyle.CARD -> WidgetLayoutFamily.CARD_STACK
    WidgetStyle.PRACTICAL -> WidgetLayoutFamily.PRACTICAL_GRID
    WidgetStyle.STYLISH -> WidgetLayoutFamily.STYLISH_CENTER
    WidgetStyle.ICON -> WidgetLayoutFamily.ICON_MINI
}

fun WidgetStyle.miniPlayerBackgroundBrush(): Brush = when (this) {
    WidgetStyle.CLASSIC -> Brush.horizontalGradient(
        listOf(Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFF2E7D32)),
    )
    WidgetStyle.LITE -> Brush.horizontalGradient(
        listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFF6A1B9A)),
    )
    WidgetStyle.VINYL -> Brush.verticalGradient(
        listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFFFF8F00)),
    )
    WidgetStyle.SIMPLE -> Brush.horizontalGradient(
        listOf(Color(0xFFAD1457), Color(0xFF6A1B9A)),
    )
    WidgetStyle.ROUND -> Brush.verticalGradient(
        listOf(Color(0xFF0D1424), Color(0xFF1A1A2E)),
    )
    WidgetStyle.MINI -> Brush.verticalGradient(
        listOf(Color(0xFF004D40), Color(0xFF00695C)),
    )
    WidgetStyle.STANDARD -> Brush.horizontalGradient(
        listOf(Color(0xFF311B92), Color(0xFF6A1B9A)),
    )
    WidgetStyle.CARD -> Brush.verticalGradient(
        listOf(Color(0xFFBF360C), Color(0xFFE65100), Color(0xFFFF6E40)),
    )
    WidgetStyle.PRACTICAL -> Brush.verticalGradient(
        listOf(Color(0xFF263238), Color(0xFF37474F)),
    )
    WidgetStyle.STYLISH -> Brush.verticalGradient(
        listOf(Color(0xFFFF6F00), Color(0xFF6A1B9A), Color(0xFF311B92)),
    )
    WidgetStyle.ICON -> Brush.linearGradient(
        listOf(Color(0xFFE64A19), Color(0xFFFF5722)),
    )
}

/** Solid tint for Glance (no gradient API); matches catalog dominant tone. */
fun WidgetStyle.glanceBackgroundArgb(): Int = when (this) {
    WidgetStyle.CLASSIC -> Color(0xFF2E7D32).toArgb()
    WidgetStyle.LITE -> Color(0xFF6A1B9A).toArgb()
    WidgetStyle.VINYL -> Color(0xFF5D4037).toArgb()
    WidgetStyle.SIMPLE -> Color(0xFFAD1457).toArgb()
    WidgetStyle.ROUND -> Color(0xFF1A1A2E).toArgb()
    WidgetStyle.MINI -> Color(0xFF00695C).toArgb()
    WidgetStyle.STANDARD -> Color(0xFF6A1B9A).toArgb()
    WidgetStyle.CARD -> Color(0xFFE65100).toArgb()
    WidgetStyle.PRACTICAL -> Color(0xFF37474F).toArgb()
    WidgetStyle.STYLISH -> Color(0xFF6A1B9A).toArgb()
    WidgetStyle.ICON -> Color(0xFFFF5722).toArgb()
}

/** Compose [Color] for Glance backgrounds (same pixels as [glanceBackgroundArgb]). */
fun WidgetStyle.glanceBackgroundColor(): Color = Color(glanceBackgroundArgb())

/** Text/icons on top of [glanceBackgroundColor] — opaque cards, high contrast (not Material onSurface). */
fun WidgetStyle.glanceOnWidgetTitleColor(): Color = Color(0xFFFFFFFF)

fun WidgetStyle.glanceOnWidgetSubtitleColor(): Color = Color(0xE6FFFFFF)

fun WidgetStyle.glanceOnWidgetIconTint(): Color = Color(0xFFF5F5F5)

/**
 * Solid background for **colorized** media notifications (shade / lock screen).
 * Matches [glanceBackgroundArgb] / the widget card so the system chrome aligns with the catalog style.
 *
 * **Why notifications cannot mirror the widget 1:1:** Android draws media playback with
 * [androidx.media3.session.MediaStyleNotificationHelper.MediaStyle] — a fixed template (compact/expanded,
 * action slots, OEM skins). You cannot embed Glance/Compose layouts, gradients, or per-style geometry.
 * AOD and some lock screens show only metadata + art. This color + rich [androidx.media3.common.MediaMetadata]
 * (title, artist, album, art) is the supported maximum.
 */
fun WidgetStyle.notificationColorizedBackgroundArgb(): Int = glanceBackgroundArgb()

/** Brighter accent (chips / highlights); optional for UI outside the notification template. */
fun WidgetStyle.notificationAccentArgb(): Int = when (this) {
    WidgetStyle.CLASSIC -> Color(0xFF4CAF50).toArgb()
    WidgetStyle.LITE -> Color(0xFFAB47BC).toArgb()
    WidgetStyle.VINYL -> Color(0xFFFF8F00).toArgb()
    WidgetStyle.SIMPLE -> Color(0xFFE91E63).toArgb()
    WidgetStyle.ROUND -> Color(0xFF4FC3F7).toArgb()
    WidgetStyle.MINI -> Color(0xFF26A69A).toArgb()
    WidgetStyle.STANDARD -> Color(0xFF7E57C2).toArgb()
    WidgetStyle.CARD -> Color(0xFFFF7043).toArgb()
    WidgetStyle.PRACTICAL -> Color(0xFF78909C).toArgb()
    WidgetStyle.STYLISH -> Color(0xFFFFB300).toArgb()
    WidgetStyle.ICON -> Color(0xFFFF7043).toArgb()
}
