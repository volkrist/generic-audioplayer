package com.generic.audioplayes.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Edge-to-edge system bar icon contrast (replaces Accompanist [SystemUiController]).
 * Window transparency is set in [com.generic.audioplayes.MainActivity] via [androidx.activity.enableEdgeToEdge].
 */
/**
 * @param useDarkStatusBarIcons `true` = dark icons on light backgrounds (Material default for light theme).
 */
@Composable
fun SyncSystemBarsTheme(useDarkStatusBarIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowInsetsControllerCompat(window, view).apply {
            isAppearanceLightStatusBars = useDarkStatusBarIcons
            isAppearanceLightNavigationBars = useDarkStatusBarIcons
        }
    }
}
