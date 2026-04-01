package com.generic.audioplayes.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Pure black surfaces for OLED; keeps primary/secondary tints from [ColorScheme].
 */
fun ColorScheme.toAmoled(): ColorScheme {
    val black = Color.Black
    val variant = Color(0xFF121212)
    return copy(
        background = black,
        surface = black,
        surfaceVariant = variant,
    )
}
