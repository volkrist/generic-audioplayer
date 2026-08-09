package com.generic.audioplayes.player

import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import com.generic.audioplayes.R

object AudioPlayerCommandButtons {

    val liked by lazy {
        CommandButton.Builder()
            .apply {
                setSessionCommand(SessionCommand(AudioPlayerCommands.UNLIKE, Bundle()))
                setDisplayName("Unlike")
                setIconResId(R.drawable.ic_baseline_favorite_24)
            }.build()
    }

    val unliked by lazy {
        CommandButton.Builder()
            .apply {
                setSessionCommand(SessionCommand(AudioPlayerCommands.LIKE, Bundle()))
                setDisplayName("Like")
                setIconResId(R.drawable.ic_baseline_favorite_border_24)
            }.build()
    }

    val cancel by lazy {
        CommandButton.Builder()
            .apply {
                setSessionCommand(SessionCommand(AudioPlayerCommands.CLOSE, Bundle()))
                setDisplayName("Close")
                setIconResId(R.drawable.ic_baseline_close_40)
            }.build()
    }

    /**
     * Media button preferences must only list the app's own buttons. media3 fills the play/pause,
     * previous and next slots from the player's available commands; claiming those slots with buttons
     * declared here strips play/pause and track skipping from the system media controls and from
     * headset buttons, because the platform session cannot represent them.
     */
    fun mediaButtonPreferences(isLiked: Boolean): List<CommandButton> = listOf(
        if (isLiked) liked else unliked,
        cancel,
    )
}