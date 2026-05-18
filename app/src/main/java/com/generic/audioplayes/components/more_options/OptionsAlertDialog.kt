package com.generic.audioplayes.components.more_options

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.ui.theme.UiTokens
import com.generic.audioplayes.util.Stage4DebugLog

/**
 * Material 3 bottom sheet for overflow actions (replaces legacy [androidx.compose.material3.AlertDialog] menu).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsAlertDialog(
    options: List<MoreOptions>,
    title: String? = null,
    onDismissRequest: () -> Unit,
) {
    if (options.isEmpty()) return
    LaunchedEffect(title, options.size) {
        Stage4DebugLog.i("OptionsAlertDialog opened title=$title options=${options.size}")
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(
            topStart = UiTokens.sheetCornerTopLarge,
            topEnd = UiTokens.sheetCornerTopLarge,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = UiTokens.paddingSheetBottom),
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = UiTokens.paddingSheetHorizontal,
                            vertical = UiTokens.paddingItem,
                        ),
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            }
            options.forEach { option ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingItem),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(UiTokens.cornerSmall))
                        .clickable(
                            onClick = {
                                onDismissRequest()
                                option.onClick()
                            },
                            indication = rememberRipple(radius = 160.dp),
                            interactionSource = remember { MutableInteractionSource() },
                        )
                        .padding(
                            horizontal = UiTokens.paddingSheetHorizontal,
                            vertical = UiTokens.paddingItem,
                        ),
                ) {
                    Icon(
                        painter = painterResource(option.icon),
                        contentDescription = option.text,
                        modifier = Modifier.size(UiTokens.iconSizeMedium),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = option.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(UiTokens.paddingItem))
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiTokens.paddingScreen),
            ) {
                Text(stringResource(R.string.folder_sheet_close))
            }
        }
    }
}
