package com.github.pakka_papad.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.github.pakka_papad.R
import com.github.pakka_papad.Screens
import com.github.pakka_papad.components.SortOptionChooser
import com.github.pakka_papad.components.getSortOptions
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

@Composable
fun HomeTopBar(
    onMenuClicked: () -> Unit,
    onSettingsClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    currentScreen: Screens,
    onSortOptionChosen: (currScreen: Int, option: Int) -> Unit,
    currentSortOrder: Map<Int, Int>,
) {
    var sortMenuVisible by remember { mutableStateOf(false) }
    val options by remember(currentScreen.ordinal) {
        derivedStateOf {
            currentScreen.getSortOptions()
        }
    }
    val openDrawerLabel = stringResource(R.string.open_navigation_drawer)
    val scheme = MaterialTheme.colorScheme
    val barBg = HomeLibraryTokens.barBackground(scheme)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(barBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(UiTokens.topBarHeight)
            .padding(horizontal = UiTokens.paddingItemTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onMenuClicked,
            modifier = Modifier.semantics { contentDescription = openDrawerLabel },
        ) {
            TwoLineMenuIcon(
                tint = scheme.onSurface,
                modifier = Modifier.padding(UiTokens.paddingItemTight),
            )
        }

        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = scheme.onSurface,
                    ),
                ) {
                    append("Zen ")
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 19.sp,
                        color = scheme.primary,
                    ),
                ) {
                    append("Music")
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = UiTokens.paddingItem),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "search-btn",
                modifier = Modifier
                    .size(UiTokens.iconSizeTouch)
                    .padding(UiTokens.paddingHorizontalComfort)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = onSearchClicked,
                    ),
                tint = scheme.onSurface,
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_sort_40),
                contentDescription = "sort-btn",
                modifier = Modifier
                    .size(UiTokens.iconSizeTouch)
                    .padding(UiTokens.paddingHorizontalComfort)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = { sortMenuVisible = true },
                    ),
                tint = scheme.onSurface,
            )
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "settings-btn",
                modifier = Modifier
                    .size(UiTokens.iconSizeTouch)
                    .padding(UiTokens.paddingHorizontalComfort)
                    .rotate(90f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = UiTokens.rippleMedium),
                        onClick = onSettingsClicked,
                    ),
                tint = scheme.onSurface,
            )
        }
    }

    if (sortMenuVisible) {
        SortOptionChooser(
            options = options,
            selectedOption = currentSortOrder[currentScreen.ordinal] ?: options.first().ordinal,
            onOptionSelect = { option ->
                onSortOptionChosen(currentScreen.ordinal, option)
                sortMenuVisible = false
            },
            onChooserDismiss = { sortMenuVisible = false },
        )
    }
}
