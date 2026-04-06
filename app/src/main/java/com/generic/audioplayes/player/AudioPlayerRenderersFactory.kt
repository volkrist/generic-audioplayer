package com.generic.audioplayes.player

import android.content.Context
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.generic.audioplayes.volume.VolumeGainController
import com.generic.audioplayes.volume.VolumePcmGainAudioProcessor

/**
 * Forces 16-bit PCM processing path so [VolumePcmGainAudioProcessor] is always in the pipeline
 * (float/high-res path in DefaultAudioSink omits custom processors).
 */
class AudioPlayerRenderersFactory(
    context: Context,
    private val volumeGainController: VolumeGainController,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        val gainProcessor = VolumePcmGainAudioProcessor(volumeGainController)
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessors(arrayOf(gainProcessor))
            .build()
    }
}
