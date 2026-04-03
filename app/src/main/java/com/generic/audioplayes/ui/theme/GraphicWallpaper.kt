package com.generic.audioplayes.ui.theme

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.generic.audioplayes.data.UserPreferences

/** Built-in library shell wallpapers (reference-style gradients). */
object GraphicWallpaper {

    const val PRESET_MIN = 0
    const val PRESET_MAX = 11

    fun allPresetIds(): List<Int> = (PRESET_MIN..PRESET_MAX).toList()

    fun shellBrush(preset: Int): Brush {
        val p = preset.coerceIn(PRESET_MIN, PRESET_MAX)
        return when (p) {
            0 -> HomeLibraryTokens.libraryShellGradient
            1 -> Brush.verticalGradient(
                listOf(Color(0xFF001428), Color(0xFF003A5C), Color(0xFF0066AA), Color(0xFF1E90FF)),
            )
            2 -> Brush.verticalGradient(
                listOf(Color(0xFF2A1038), Color(0xFF5C1E6E), Color(0xFF9B59B6), Color(0xFFE8A0D8)),
            )
            3 -> Brush.verticalGradient(
                listOf(Color(0xFF1A0508), Color(0xFF5C0A0A), Color(0xFFB71C1C), Color(0xFFFF6B35)),
            )
            4 -> Brush.verticalGradient(
                listOf(Color(0xFF120818), Color(0xFF3D1454), Color(0xFF8B2F8B), Color(0xFFFF66C4)),
            )
            5 -> Brush.verticalGradient(
                listOf(Color(0xFF0C1420), Color(0xFF1E3A5F), Color(0xFF4A6FA5), Color(0xFFB8D4E8)),
            )
            6 -> Brush.verticalGradient(
                listOf(Color(0xFF061A12), Color(0xFF0D4D2E), Color(0xFF1FA463), Color(0xFF7FD99F)),
            )
            7 -> Brush.verticalGradient(
                listOf(Color(0xFF102018), Color(0xFF2D4A22), Color(0xFF4E7C3A), Color(0xFFA8C686)),
            )
            8 -> Brush.verticalGradient(
                listOf(Color(0xFF1A1208), Color(0xFF4A3010), Color(0xFF8B5A2B), Color(0xFFE8C89A)),
            )
            9 -> Brush.verticalGradient(
                listOf(Color(0xFF18100C), Color(0xFF3D2418), Color(0xFF6B4423), Color(0xFFC4A484)),
            )
            10 -> Brush.verticalGradient(
                listOf(Color(0xFF0E1018), Color(0xFF1C2840), Color(0xFF3D5A80), Color(0xFF98C1D9)),
            )
            11 -> Brush.verticalGradient(
                listOf(Color(0xFF140818), Color(0xFF301040), Color(0xFF602080), Color(0xFFB060C0)),
            )
            else -> HomeLibraryTokens.libraryShellGradient
        }
    }

}

/**
 * Full-screen library shell: custom image or built-in gradient preset.
 */
@Composable
fun LibraryShellBackdrop(
    themePreference: ThemePreference,
    modifier: Modifier = Modifier,
) {
    val uri = themePreference.graphicWallpaperCustomUri
    if (uri.isNotBlank()) {
        AsyncImage(
            model = Uri.parse(uri),
            contentDescription = null,
            modifier = modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(GraphicWallpaper.shellBrush(themePreference.graphicWallpaperPreset)),
        )
    }
}

/** Swatch fill for the color grid (accent mapping + decorative gradients). */
fun graphicAccentSwatchBrush(
    accent: UserPreferences.Accent,
    variantIndex: Int,
): Brush {
    val base = accent.getSeedColor()
    return when (variantIndex % 3) {
        0 -> Brush.radialGradient(
            colors = listOf(base.copy(alpha = 0.95f), base.copy(alpha = 0.55f), Color(0xFF1A1028)),
        )
        1 -> Brush.linearGradient(
            colors = listOf(base, base.copy(red = (base.red + 0.08f).coerceAtMost(1f)), Color(0xFF12081A)),
        )
        else -> Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1A2E), base, base.copy(alpha = 0.7f)),
        )
    }
}
