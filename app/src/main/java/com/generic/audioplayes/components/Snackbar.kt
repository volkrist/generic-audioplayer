package com.generic.audioplayes.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Styled snackbar used by every screen (`HomeFragment`, `CollectionFragment`,
 * `BackupRestoreFragment`, etc. route their toast‑level messages through this composable so
 * changes here ripple everywhere).
 *
 * Style mirrors the launcher icon / library gradient: a deep violet → indigo pill with a neon
 * cyan accent column on the left, rounded corners and a soft shadow so it reads as "the same
 * app" instead of the default Material 3 grey chip. Action label (e.g. "Settings" snackbar in
 * the permissions flow) uses the theme's primary so it still looks tappable.
 */
@Composable
fun Snackbar(
    snackbarData: SnackbarData,
) {
    val shape = RoundedCornerShape(18.dp)
    val gradient = Brush.linearGradient(
        colors = listOf(
            BrandSnackbarColors.start,
            BrandSnackbarColors.end,
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 12.dp, shape = shape, clip = false)
            .clip(shape)
            .background(brush = gradient, shape = shape)
            .border(
                width = 1.dp,
                color = BrandSnackbarColors.border,
                shape = shape,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BrandSnackbarColors.accent),
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = snackbarData.visuals.message,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            val actionLabel = snackbarData.visuals.actionLabel
            if (!actionLabel.isNullOrEmpty()) {
                TextButton(
                    onClick = { snackbarData.performAction() },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = actionLabel,
                        color = BrandSnackbarColors.accent,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}

/**
 * Wraps [Snackbar] in the same slide‑in/fade animation the stock M3 snackbar uses so the
 * custom styling still feels native. Kept public in case an individual screen wants to tweak
 * entrance timing without rebuilding the whole bar.
 */
@Composable
fun AnimatedBrandSnackbar(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically { it },
        exit = fadeOut() + slideOutVertically { it },
    ) {
        content()
    }
}

private object BrandSnackbarColors {
    // Deep‑violet → indigo gradient matches HomeLibraryTokens.libraryShellGradient (end stops)
    // and the neon palette of the launcher icon, so the snackbar never clashes with any screen.
    val start: Color = Color(0xFF2D0D45)
    val end: Color = Color(0xFF1E1B4B)
    val accent: Color = Color(0xFF22D3EE) // cyan highlight from the app icon
    val border: Color = Color(0xFFA855F7).copy(alpha = 0.25f)
}
