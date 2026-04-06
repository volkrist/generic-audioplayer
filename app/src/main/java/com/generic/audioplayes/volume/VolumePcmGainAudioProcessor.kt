package com.generic.audioplayes.volume

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Multiplies 16-bit little-endian PCM samples by [VolumeGainController.linearGain] (up to 2.0 for 200%).
 */
@UnstableApi
class VolumePcmGainAudioProcessor(
    private val gainController: VolumeGainController,
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return AudioFormat(
            inputAudioFormat.sampleRate,
            inputAudioFormat.channelCount,
            C.ENCODING_PCM_16BIT,
        )
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val position = inputBuffer.position()
        val limit = inputBuffer.limit()
        val remaining = limit - position
        if (remaining == 0) return

        val gain = gainController.linearGain.coerceIn(0.5f, 2.5f)
        if (gain <= 1.0005f) {
            val out = replaceOutputBuffer(remaining)
            val dup = inputBuffer.duplicate()
            out.put(dup)
            out.flip()
            inputBuffer.position(limit)
            return
        }

        val out = replaceOutputBuffer(remaining)
        val dup = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        dup.position(position)
        dup.limit(limit)
        while (dup.remaining() >= 2) {
            val s = dup.short.toInt()
            var v = (s * gain).roundToInt()
            if (v > Short.MAX_VALUE) v = Short.MAX_VALUE.toInt()
            if (v < Short.MIN_VALUE) v = Short.MIN_VALUE.toInt()
            out.putShort(v.toShort())
        }
        out.flip()
        inputBuffer.position(limit)
    }

    override fun onFlush() {
        // BaseAudioProcessor clears output; nothing to reset.
    }
}
