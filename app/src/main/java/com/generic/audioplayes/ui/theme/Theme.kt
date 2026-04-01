package com.generic.audioplayes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.generic.audioplayes.data.UserPreferences
import com.google.accompanist.systemuicontroller.SystemUiController
import com.google.accompanist.systemuicontroller.rememberSystemUiController

/**
 * Dense dark “library shell” chrome for [com.generic.audioplayes.home.HomeFragment].
 * Blends with dynamic [ColorScheme] so accent still follows user theme.
 */
object HomeLibraryTokens {
    /**
     * Full-screen blue–violet shell behind library [HomeFragment] (reference layout).
     */
    val libraryShellGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0B0B20),
                Color(0xFF12102A),
                Color(0xFF2D0D45),
                Color(0xFF4A1570),
            ),
        )

    fun barBackground(scheme: ColorScheme): Color = scheme.surface
    @Suppress("UNUSED_PARAMETER")
    fun canvasBackground(scheme: ColorScheme): Color = Color.Transparent
    fun navBarBackground(scheme: ColorScheme): Color = scheme.surfaceVariant.copy(alpha = 0.55f)
    fun navPill(scheme: ColorScheme): Color = scheme.secondary.copy(alpha = 0.4f)
    fun rowHover(scheme: ColorScheme): Color = scheme.surfaceVariant.copy(alpha = 0.35f)
    /** Mini player strip (row + thin progress) above bottom nav when a track is active. */
    fun miniPlayerSurface(scheme: ColorScheme): Color =
        scheme.surface.copy(alpha = 0.98f)

    val gridMinSize get() = UiTokens.gridMinCellSize
    val contentHorizontalPadding get() = UiTokens.paddingScreen
    /** Library inset when no track: bottom nav + gap (see [UiTokens.scaffoldBottomPaddingIdle]). */
    val scaffoldBottomPaddingIdle get() = UiTokens.scaffoldBottomPaddingIdle
    /** Mini strip height: [UiTokens.miniPlayerRowHeight] + vertical padding + [UiTokens.progressBarThin]. */
    val miniPlayerPeekHeight get() = UiTokens.miniPlayerPeekHeight
    val bottomNavHeight get() = UiTokens.bottomNavHeight
}


@Composable
fun DefaultTheme(content: @Composable () -> Unit) {
    ZenTheme(
        themePreference = ThemePreference(
            useMaterialYou = true,
            theme = UserPreferences.Theme.USE_SYSTEM_MODE,
            accent = UserPreferences.Accent.Elm,
        ),
        content = content,
    )
}

@Composable
fun ZenTheme(
    themePreference: ThemePreference,
    systemUiController: SystemUiController = rememberSystemUiController(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isSystemDark = isSystemInDarkTheme()
    val wantsAmoled = themePreference.theme == UserPreferences.Theme.AMOLED_MODE
    val isDark = when (themePreference.theme) {
        UserPreferences.Theme.LIGHT_MODE -> false
        UserPreferences.Theme.DARK_MODE, UserPreferences.Theme.AMOLED_MODE -> true
        UserPreferences.Theme.USE_SYSTEM_MODE, UserPreferences.Theme.UNRECOGNIZED -> isSystemDark
    }
    val colourScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && themePreference.useMaterialYou -> {
            val base = when {
                wantsAmoled -> dynamicDarkColorScheme(context)
                isDark -> dynamicDarkColorScheme(context)
                else -> dynamicLightColorScheme(context)
            }
            if (wantsAmoled) base.toAmoled() else base
        }
        wantsAmoled -> themePreference.accent.getColorScheme(isDark = true).toAmoled()
        else -> themePreference.accent.getColorScheme(isDark)
    }
    DisposableEffect(themePreference, systemUiController) {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = !isDark,
            isNavigationBarContrastEnforced = false,
            transformColorForLightContent = {
                Color.Transparent
            }
        )
        onDispose { }
    }
    MaterialTheme(
        colorScheme = colourScheme,
        typography = ZenTypography,
        content = {
            CompositionLocalProvider(LocalThemePreference provides themePreference) {
                content()
            }
        }
    )
}

val LocalThemePreference = compositionLocalOf { ThemePreference() }