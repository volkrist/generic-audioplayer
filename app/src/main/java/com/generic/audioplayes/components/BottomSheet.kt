package com.generic.audioplayes.components

import androidx.compose.animation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Vertical swipe between expanded [content] (full player) and collapsed [peekContent] (mini player).
 * Bottom anchor height is [peekHeight]; does not handle system insets — host supplies padded [peekContent].
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheet(
    peekHeight: Dp,
    peekContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
    swipeableState: SwipeableState<Int> = rememberSwipeableState(initialValue = 0),
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val anchors = mapOf(
            0f to 1,
            with(density) { (maxHeight - peekHeight).toPx() } to 0
        )
        val peekContentAlpha = remember {
            derivedStateOf {
                if (swipeableState.progress.from == 0){
                    // at 0 or moving away from 0
                    if (swipeableState.progress.to == 0) 1f
                    else if (swipeableState.progress.fraction < 0.25f) 1f-swipeableState.progress.fraction*4
                    else 0f
                } else {
                    // at 1 or moving away from 1
                    if (swipeableState.progress.to == 1) 0f
                    else if (swipeableState.progress.fraction > 0.75f) 1f-(1f-swipeableState.progress.fraction)*4
                    else 0f
                }
            }
        }
        val peekAlpha by peekContentAlpha
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationY = swipeableState.offset.value
                }
                .fillMaxSize()
                .align(Alignment.BottomCenter)
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Vertical
                )
        ) {
            /**
             * Order of composable matters.
             * Reversed order would result in clickables in peekContent not working.
             * When peek alpha is ~0, do not compose peek: invisible peek still hit-tests at the top
             * (mini bar is laid out at TopStart), stealing taps meant for the full player (e.g. overflow ⋮).
             */
            Box(Modifier.graphicsLayer { alpha = 1f - peekAlpha }) {
                content()
            }
            if (peekAlpha > 0.01f) {
                Box(Modifier.graphicsLayer { alpha = peekAlpha }) {
                    peekContent()
                }
            }
        }
    }
}