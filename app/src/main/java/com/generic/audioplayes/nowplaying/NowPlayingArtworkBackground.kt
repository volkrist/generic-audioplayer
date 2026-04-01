package com.generic.audioplayes.nowplaying

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.generic.audioplayes.data.music.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Artwork-derived gradient + scrim for Now Playing. Falls back to theme surface when there is no art
 * or palette extraction fails.
 */
data class NowPlayingArtworkPalette(
    val top: Color,
    val bottom: Color,
    /** Dark overlay on top of the gradient for text / control contrast. */
    val scrimAlpha: Float,
) {
    companion object {
        fun fromTheme(scheme: ColorScheme): NowPlayingArtworkPalette =
            NowPlayingArtworkPalette(
                top = scheme.surface,
                bottom = scheme.surfaceVariant,
                scrimAlpha = 0.12f,
            )
    }
}

@Composable
fun rememberNowPlayingArtworkPalette(song: Song): NowPlayingArtworkPalette {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val imageLoader = remember(context) { ImageLoader(context) }
    var palette by remember { mutableStateOf(NowPlayingArtworkPalette.fromTheme(scheme)) }

    LaunchedEffect(song.location, song.artUri, scheme) {
        val uri = song.artUri
        if (uri.isNullOrBlank()) {
            palette = NowPlayingArtworkPalette.fromTheme(scheme)
            return@LaunchedEffect
        }
        val loaded = withContext(Dispatchers.IO) {
            loadPaletteFromArtUri(imageLoader, context, uri)
        }
        palette = loaded ?: NowPlayingArtworkPalette.fromTheme(scheme)
    }

    return palette
}

private val paletteTween =
    tween<Color>(durationMillis = 420, easing = FastOutSlowInEasing)

private val paletteScrimTween =
    tween<Float>(durationMillis = 420, easing = FastOutSlowInEasing)

@Composable
fun NowPlayingArtworkBackgroundLayer(
    palette: NowPlayingArtworkPalette,
    modifier: Modifier = Modifier,
) {
    val top by animateColorAsState(palette.top, paletteTween, label = "npPaletteTop")
    val bottom by animateColorAsState(palette.bottom, paletteTween, label = "npPaletteBottom")
    val scrimAlpha by animateFloatAsState(palette.scrimAlpha, paletteScrimTween, label = "npPaletteScrim")
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(top, bottom),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha)),
        )
    }
}

private suspend fun loadPaletteFromArtUri(
    imageLoader: ImageLoader,
    context: android.content.Context,
    uriString: String,
): NowPlayingArtworkPalette? {
    val request = ImageRequest.Builder(context)
        .data(uriString)
        .size(320)
        .allowHardware(false)
        .build()
    val result = imageLoader.execute(request)
    if (result !is SuccessResult) return null
    val drawable = result.drawable
    val raw = drawable.toBitmap()
    val bitmap = raw.copy(Bitmap.Config.ARGB_8888, false)
    if (raw !== bitmap) raw.recycle()
    val palette = Palette.from(bitmap).generate()
    bitmap.recycle()

    val topSwatch = palette.darkMutedSwatch
        ?: palette.mutedSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.vibrantSwatch
        ?: palette.swatches.maxByOrNull { it.population }
    val bottomSwatch = palette.vibrantSwatch
        ?: palette.lightMutedSwatch
        ?: palette.mutedSwatch
        ?: palette.darkMutedSwatch
        ?: topSwatch

    if (topSwatch == null && bottomSwatch == null) return null

    val topBase = (topSwatch ?: bottomSwatch!!).rgb.toComposeColor()
    val bottomBase = (bottomSwatch ?: topSwatch!!).rgb.toComposeColor()
    return NowPlayingArtworkPalette(
        top = topBase.darken(0.72f),
        bottom = bottomBase.darken(0.82f),
        scrimAlpha = 0.46f,
    )
}

private fun Int.toComposeColor(): Color {
    val r = android.graphics.Color.red(this) / 255f
    val g = android.graphics.Color.green(this) / 255f
    val b = android.graphics.Color.blue(this) / 255f
    return Color(r, g, b, 1f)
}

private fun Color.darken(factor: Float): Color {
    return Color(
        red * factor,
        green * factor,
        blue * factor,
        alpha = alpha,
    )
}
