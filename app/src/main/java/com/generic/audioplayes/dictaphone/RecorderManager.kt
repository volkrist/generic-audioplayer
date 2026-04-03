package com.generic.audioplayes.dictaphone

import android.content.Context
import android.media.MediaRecorder
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records AAC audio into MPEG-4 (.m4a) under app external Music/AudioPlayer/Recordings.
 */
@Singleton
class RecorderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashReporter: AudioPlayerCrashReporter,
) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun recordingsDirectory(): File {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "Music")
        return File(base, "AudioPlayer/Recordings").apply { mkdirs() }
    }

    suspend fun startRecording(): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (_isRecording.value) {
                return@withContext Result.failure(IllegalStateException("Already recording"))
            }
            val file = File(recordingsDirectory(), "recording_${System.currentTimeMillis()}.m4a")
            outputFile = file
            val recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecording.value = true
            Result.success(file)
        } catch (e: Exception) {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            outputFile?.delete()
            outputFile = null
            _isRecording.value = false
            crashReporter.logException(e)
            Result.failure(e)
        }
    }

    suspend fun stopRecording(): Result<File?> = withContext(Dispatchers.IO) {
        val recorder = mediaRecorder
        val file = outputFile
        if (recorder == null) {
            return@withContext Result.success(null)
        }
        try {
            recorder.stop()
        } catch (e: RuntimeException) {
            crashReporter.logException(e)
        } finally {
            try {
                recorder.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
            _isRecording.value = false
            outputFile = null
        }
        file?.takeIf { it.exists() && it.length() > 0L }?.let { scanMedia(it) }
        Result.success(file?.takeIf { it.exists() })
    }

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    private fun scanMedia(file: File) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("audio/mp4"),
            null,
        )
    }
}
