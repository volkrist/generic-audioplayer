package com.generic.audioplayes.nowplaying

import androidx.annotation.DrawableRes
import com.generic.audioplayes.R
import com.generic.audioplayes.components.more_options.MoreOptions

sealed class NowPlayingOptions(
    override val onClick: () -> Unit,
    override val text: String,
    @DrawableRes override val icon: Int,
) : MoreOptions(
    onClick = onClick,
    text = text,
    icon = icon
) {
    data class SaveToPlaylist(override val onClick: () -> Unit) :
        NowPlayingOptions(
            onClick = onClick,
            text = "Save queue",
            icon = R.drawable.ic_baseline_playlist_add_40
        )
}
