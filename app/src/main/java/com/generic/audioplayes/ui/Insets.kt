package com.generic.audioplayes.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.LayoutDirection

/**
 * Merges [scaffoldPadding] from [androidx.compose.material3.Scaffold] with system navigation bar
 * and IME so scrollable content clears gesture nav and the keyboard on edge-to-edge screens.
 */
@Composable
fun scaffoldContentPaddingWithSystemBars(
    scaffoldPadding: PaddingValues,
    includeIme: Boolean = true,
): PaddingValues {
    val bottomInsets = if (includeIme) {
        WindowInsets.navigationBars.union(WindowInsets.ime)
    } else {
        WindowInsets.navigationBars
    }.asPaddingValues()
    val layoutDirection = LayoutDirection.Ltr
    return PaddingValues(
        top = scaffoldPadding.calculateTopPadding(),
        bottom = scaffoldPadding.calculateBottomPadding() + bottomInsets.calculateBottomPadding(),
        start = scaffoldPadding.calculateLeftPadding(layoutDirection),
        end = scaffoldPadding.calculateRightPadding(layoutDirection),
    )
}
