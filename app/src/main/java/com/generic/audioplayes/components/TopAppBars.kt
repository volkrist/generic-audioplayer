package com.generic.audioplayes.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.ripple
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R

@Composable
private fun BaseTopBar(
    appBar: @Composable BoxScope.() -> Unit,
    backgroundColor: Color,
    showBottomDivider: Boolean = true,
) = Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier
        .background(backgroundColor),
    content = {
        appBar()
        if (showBottomDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
)

@Composable
private fun TopBarTitle(
    title: String,
    titleMaxLines: Int,
    textColor: Color,
) = Text(
    text = title,
    style = MaterialTheme.typography.titleLarge,
    maxLines = titleMaxLines,
    overflow = TextOverflow.Ellipsis,
    color = textColor,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CenterAlignedTopBar(
    leadingIcon: @Composable () -> Unit,
    title: String,
    actions: @Composable RowScope.() -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onBackgroundColor: Color = MaterialTheme.colorScheme.onSurface,
    titleMaxLines: Int,
) = BaseTopBar(
    appBar = {
        CenterAlignedTopAppBar(
            modifier = Modifier
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            navigationIcon = leadingIcon,
            title = {
                TopBarTitle(
                    title = title,
                    titleMaxLines = titleMaxLines,
                    textColor = onBackgroundColor,
                )
            },
            actions = actions,
        )
    },
    backgroundColor = backgroundColor,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallTopBar(
    leadingIcon: @Composable () -> Unit,
    title: AnnotatedString,
    actions: @Composable RowScope.() -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onBackgroundColor: Color = MaterialTheme.colorScheme.onSurface,
    titleMaxLines: Int,
    showBottomDivider: Boolean = true,
) = BaseTopBar(
    appBar = {
        TopAppBar(
            modifier = Modifier
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
            navigationIcon = leadingIcon,
            title = {
                Text(
                    text = title,
                    maxLines = titleMaxLines,
                    color = onBackgroundColor,
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Ellipsis
                )
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = backgroundColor
            )
        )
    },
    backgroundColor = backgroundColor,
    showBottomDivider = showBottomDivider,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarWithBackArrow(
    onBackArrowPressed: () -> Unit,
    title: String,
    actions: @Composable RowScope.() -> Unit,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    onBackgroundColor: Color = MaterialTheme.colorScheme.onSurface,
    titleMaxLines: Int = 1,
    centerTitle: Boolean = true,
    showBottomDivider: Boolean = true,
    backgroundBrush: Brush? = null,
) {
    val leadingIcon: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.back_button),
            modifier = Modifier
                .padding(16.dp)
                .size(30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        radius = 25.dp,
                    ),
                    onClick = onBackArrowPressed
                ),
            tint = onBackgroundColor
        )
    }
    val barModifier = Modifier
        .then(
            if (backgroundBrush != null) Modifier.background(backgroundBrush)
            else Modifier.background(backgroundColor)
        )
        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))

    val titleComposable: @Composable () -> Unit = {
        TopBarTitle(
            title = title,
            titleMaxLines = titleMaxLines,
            textColor = onBackgroundColor,
        )
    }

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .then(
                if (backgroundBrush != null) Modifier.background(Color.Transparent)
                else Modifier.background(backgroundColor)
            ),
    ) {
        if (centerTitle) {
            CenterAlignedTopAppBar(
                modifier = barModifier,
                navigationIcon = leadingIcon,
                title = titleComposable,
                actions = actions,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = onBackgroundColor,
                    titleContentColor = onBackgroundColor,
                    actionIconContentColor = onBackgroundColor,
                ),
            )
        } else {
            TopAppBar(
                modifier = barModifier,
                navigationIcon = leadingIcon,
                title = titleComposable,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = onBackgroundColor,
                    titleContentColor = onBackgroundColor,
                    actionIconContentColor = onBackgroundColor,
                ),
            )
        }
        if (showBottomDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}


@Composable
fun CancelConfirmTopBar(
    onCancelClicked: () -> Unit,
    onConfirmClicked: () -> Unit,
    title: String,
) = CenterAlignedTopBar(
    leadingIcon = {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.close_button),
            modifier = Modifier
                .padding(16.dp)
                .size(30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        radius = 25.dp,
                    ),
                    onClick = onCancelClicked
                ),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    },
    title = title,
    actions = {
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = stringResource(R.string.check_button),
            modifier = Modifier
                .padding(16.dp)
                .size(30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = false,
                        radius = 25.dp,
                    ),
                    onClick = onConfirmClicked
                ),
            tint = MaterialTheme.colorScheme.onSurface
        )
    },
    titleMaxLines = 1
)