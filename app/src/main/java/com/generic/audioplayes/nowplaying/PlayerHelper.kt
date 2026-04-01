package com.generic.audioplayes.nowplaying

import androidx.compose.runtime.Stable
import androidx.media3.common.C
import androidx.media3.common.Player.Listener
import androidx.media3.exoplayer.ExoPlayer

@Stable
class PlayerHelper(
    private val exoPlayer: ExoPlayer,
) {
    val currentPosition: Float
        get() = exoPlayer.currentPosition.toFloat()

    val duration: Float
        get() = exoPlayer.duration.toFloat()

    val currentMediaItemIndex: Int
        get() = exoPlayer.currentMediaItemIndex

    fun addListener(listener: Listener) {
        exoPlayer.addListener(listener)
    }

    fun removeListener(listener: Listener) {
        exoPlayer.removeListener(listener)
    }

    fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        exoPlayer.seekTo(mediaItemIndex, positionMs)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    /** Seeks by [deltaMs] milliseconds, clamped to the current media duration (if known). */
    fun seekRelative(deltaMs: Long) {
        val durationMs = exoPlayer.duration
        val cur = exoPlayer.currentPosition
        val max = if (durationMs != C.TIME_UNSET && durationMs > 0) durationMs else Long.MAX_VALUE
        exoPlayer.seekTo((cur + deltaMs).coerceIn(0L, max))
    }
}