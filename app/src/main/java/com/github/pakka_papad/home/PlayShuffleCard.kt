package com.github.pakka_papad.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.github.pakka_papad.R
import com.github.pakka_papad.ui.theme.HomeLibraryTokens
import com.github.pakka_papad.ui.theme.UiTokens

@Composable
fun PlayShuffleCard(
    onPlayAllClicked: () -> Unit,
    onShuffleClicked: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val spacerModifier = Modifier.width(UiTokens.paddingItem)
    val iconModifier = Modifier.size(UiTokens.artworkThumbMini)
    val scheme = MaterialTheme.colorScheme
    val wide = configuration.screenWidthDp > 340
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(UiTokens.playShuffleCardHeight)
            .padding(
                horizontal = HomeLibraryTokens.contentHorizontalPadding,
                vertical = UiTokens.paddingSection,
            ),
        horizontalArrangement = Arrangement.spacedBy(UiTokens.paddingSection),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onPlayAllClicked,
            modifier = Modifier
                .weight(1f)
                .height(UiTokens.playShuffleButtonHeight),
            shape = RoundedCornerShape(UiTokens.playShuffleCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primaryContainer,
                contentColor = scheme.onPrimaryContainer,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = UiTokens.elevationNone),
            contentPadding = PaddingValues(horizontal = UiTokens.gridSpacing),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_play_arrow_40),
                modifier = iconModifier,
                contentDescription = stringResource(R.string.play_all_button),
            )
            if (wide) {
                Spacer(spacerModifier)
                Text(
                    text = stringResource(R.string.play_all),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Button(
            onClick = onShuffleClicked,
            modifier = Modifier
                .weight(1f)
                .height(UiTokens.playShuffleButtonHeight),
            shape = RoundedCornerShape(UiTokens.playShuffleCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = UiTokens.elevationNone),
            contentPadding = PaddingValues(horizontal = UiTokens.gridSpacing),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_shuffle_40),
                modifier = iconModifier,
                contentDescription = stringResource(R.string.shuffle_button),
            )
            if (wide) {
                Spacer(spacerModifier)
                Text(
                    text = stringResource(R.string.shuffle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayShuffleCardPreview() {
    PlayShuffleCard(
        onPlayAllClicked = { },
        onShuffleClicked = { }
    )
}
