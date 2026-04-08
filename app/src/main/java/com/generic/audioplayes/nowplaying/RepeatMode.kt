package com.generic.audioplayes.nowplaying

import androidx.annotation.DrawableRes
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.R

enum class RepeatMode(@DrawableRes val iconResource: Int) {
    NO_REPEAT(R.drawable.baseline_arrow_forward_40),
    REPEAT_ALL(R.drawable.baseline_repeat_40),
    REPEAT_ONE(R.drawable.baseline_repeat_one_40);

    fun next(): RepeatMode {
        return when(this){
            NO_REPEAT -> REPEAT_ALL
            REPEAT_ALL -> REPEAT_ONE
            REPEAT_ONE -> NO_REPEAT
        }
    }

    /**
     * [queueSize] must match ExoPlayer playlist size. When [REPEAT_ALL] is selected but there is
     * only one track, ExoPlayer would loop that single item forever; map to [REPEAT_MODE_OFF] so
     * playback ends and multi-item queues still wrap as expected.
     */
    fun toExoPlayerRepeatMode(queueSize: Int): Int {
        return when (this) {
            NO_REPEAT -> ExoPlayer.REPEAT_MODE_OFF
            REPEAT_ALL ->
                if (queueSize <= 1) ExoPlayer.REPEAT_MODE_OFF else ExoPlayer.REPEAT_MODE_ALL
            REPEAT_ONE -> ExoPlayer.REPEAT_MODE_ONE
        }
    }
}