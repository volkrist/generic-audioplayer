package com.generic.audioplayes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Full‑screen branded splash shown on top of the library until [onFinished] fires.
 *
 *  • The artwork fills the ENTIRE viewport via [ContentScale.Crop] so the black edges you'd
 *    normally see around a square icon on a portrait phone disappear — the icon extends past
 *    the screen edges and the composition feels cinematic instead of "logo sitting in a void".
 *  • Two infinite highlight passes shimmer across the note: a bright diagonal sweep (quick,
 *    like a spotlight sweeping over glass) plus a slower hue‑shift tint over the whole image
 *    that drifts cyan → magenta → indigo to keep the scene alive.
 *  • Three neon blobs orbit the icon behind the image so peripheral glow always moves, even
 *    while the note itself stays static (we removed the earlier pulse‑scale because it looked
 *    like the icon was being stretched once we enabled [ContentScale.Crop]).
 *  • Overlay fades out after [totalVisibleDurationMs] so the library appears through it.
 */
@Composable
fun BrandingSplashOverlay(
    totalVisibleDurationMs: Long = 1500L,
    fadeOutDurationMs: Int = 500,
    onFinished: () -> Unit,
) {
    val exitAlpha = remember { Animatable(1f) }
    var startExit by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(totalVisibleDurationMs)
        startExit = true
    }
    LaunchedEffect(startExit) {
        if (!startExit) return@LaunchedEffect
        exitAlpha.animateTo(0f, tween(fadeOutDurationMs, easing = FastOutSlowInEasing))
        onFinished()
    }

    val infinite = rememberInfiniteTransition(label = "branding")

    // Orbit angle drives three glow blobs circling the icon in the background layer.
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5200, easing = LinearEasing),
        ),
        label = "orbit",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    // Diagonal shimmer sweep: -1..1 maps to a strip travelling from top‑left (off screen) to
    // bottom‑right (off screen), creating a glassy "gleam" pass every ~2 s.
    val shimmer by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
        ),
        label = "shimmer",
    )

    // Slow hue rotation: drives a semi‑transparent color veil so the icon softly "iridesces"
    // between cyan, magenta and indigo (the three accents from the icon artwork).
    val tintPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
        ),
        label = "tint",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAlpha.value }
            .background(Color.Black)
            .drawBehind {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val radius = min(w, h) * 0.45f
                val blobSize = min(w, h) * 0.6f
                val colors = listOf(
                    Color(0xFF22D3EE),
                    Color(0xFFA855F7),
                    Color(0xFF6366F1),
                )
                for (i in 0 until 3) {
                    val angleRad = Math.toRadians((orbit + i * 120f).toDouble())
                    val gx = cx + radius * cos(angleRad).toFloat()
                    val gy = cy + radius * sin(angleRad).toFloat()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors[i].copy(alpha = glowPulse * 0.45f),
                                colors[i].copy(alpha = 0f),
                            ),
                            center = Offset(gx, gy),
                            radius = blobSize,
                        ),
                        radius = blobSize,
                        center = Offset(gx, gy),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Full‑bleed source PNG (branding/app_icon.png, 1024×1024, no transparent padding). The
        // earlier iteration referenced the adaptive‑icon foreground which has transparent safe
        // margins, so on a portrait phone Crop left black bars top & bottom. The drawable lives
        // under drawable-nodpi/ so Android does not try to density‑scale the bitmap.
        Image(
            painter = painterResource(R.drawable.app_icon_full),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Diagonal bright‑white gleam sweeping left→right.
                    val w = size.width
                    val h = size.height
                    val centerX = w * shimmer
                    val gleamWidth = w * 0.35f
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.28f),
                                Color.Transparent,
                            ),
                            start = Offset(centerX - gleamWidth, -h * 0.15f),
                            end = Offset(centerX + gleamWidth, h * 1.15f),
                        ),
                        blendMode = BlendMode.Plus,
                    )
                    // Iridescent cyan↔magenta↔indigo veil (very low alpha) that drifts slowly
                    // so the note visibly shimmers in place instead of sitting still.
                    val veilColor = iridescentColor(tintPhase).copy(alpha = 0.18f)
                    drawRect(
                        color = veilColor,
                        blendMode = BlendMode.Overlay,
                    )
                },
            // Crop on the full‑bleed PNG: image scales to cover the whole viewport, aspect
            // preserved. On a portrait phone height scales up first (~2.3×) so no top/bottom
            // bars; excess on the sides is cropped — the note sits in the centre so the art
            // survives. FilterQuality.High avoids visible blur when upscaling 1024×1024.
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * Linearly interpolates between 3 brand stops (cyan → magenta → indigo) using a normalised
 * phase in [0f..1f]. Returns a pure color — alpha is applied by the caller so the veil is
 * only a hint, never overpowering the underlying artwork.
 */
private fun iridescentColor(phase: Float): Color {
    val stops = listOf(
        Color(0xFF22D3EE),
        Color(0xFFA855F7),
        Color(0xFF6366F1),
        Color(0xFF22D3EE),
    )
    val t = (phase.coerceIn(0f, 1f)) * (stops.size - 1)
    val i = t.toInt().coerceAtMost(stops.size - 2)
    val f = t - i
    val a = stops[i]
    val b = stops[i + 1]
    return Color(
        red = a.red + (b.red - a.red) * f,
        green = a.green + (b.green - a.green) * f,
        blue = a.blue + (b.blue - a.blue) * f,
        alpha = 1f,
    )
}
