package com.github.pakka_papad.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.github.pakka_papad.R
import com.github.pakka_papad.data.music.Song
import com.github.pakka_papad.ui.theme.UiTokens

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MiniPlayer(
    showPlayButton: Boolean,
    onPausePlayPressed: () -> Unit,
    song: Song?,
    modifier: Modifier = Modifier,
) {
    if (song == null) return
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(UiTokens.miniPlayerRowHeight)
            .padding(horizontal = UiTokens.paddingSection, vertical = UiTokens.metaSpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(UiTokens.gridSpacing),
    ) {
        AnimatedContent(
            targetState = song.location,
            transitionSpec = {
                fadeIn(tween(220)) with fadeOut(tween(180))
            },
            label = "miniPlayerTrack",
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(UiTokens.gridSpacing),
            ) {
                AsyncImage(
                    model = song.artUri,
                    contentDescription = stringResource(R.string.song_image),
                    modifier = Modifier
                        .size(UiTokens.artworkMini)
                        .clip(RoundedCornerShape(UiTokens.cornerSmall))
                        .background(scheme.surfaceVariant.copy(alpha = 0.65f)),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(UiTokens.artworkMedium),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurface,
                    )
                    Text(
                        text = song.artist.ifBlank { stringResource(R.string.unknown) },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        Icon(
            painter = painterResource(
                if (showPlayButton) R.drawable.ic_baseline_play_arrow_40 else R.drawable.ic_baseline_pause_40,
            ),
            contentDescription = stringResource(
                if (showPlayButton) R.string.play_button else R.string.pause_button,
            ),
            modifier = Modifier
                .size(UiTokens.iconSizeTouch)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.18f))
                .clickable(
                    onClick = onPausePlayPressed,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = true, radius = UiTokens.rippleMedium),
                )
                .padding(UiTokens.paddingItem),
            tint = scheme.primary,
        )
    }
}
