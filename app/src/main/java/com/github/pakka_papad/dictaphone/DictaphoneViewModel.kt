package com.github.pakka_papad.dictaphone

import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.github.pakka_papad.R
import com.github.pakka_papad.data.library.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class RecordingListItem(
    val file: File,
    val displayTitle: String,
    val durationMs: Long?,
)

@HiltViewModel
class DictaphoneViewModel @Inject constructor(
    @ApplicationContext private val appContext: android.content.Context,
    private val recorderManager: RecorderManager,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    val isRecording: StateFlow<Boolean> = recorderManager.isRecording

    private val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val _recordings = MutableStateFlow<List<RecordingListItem>>(emptyList())
    val recordings: StateFlow<List<RecordingListItem>> = _recordings.asStateFlow()

    private val _recordingElapsedMs = MutableStateFlow(0L)
    val recordingElapsedMs: StateFlow<Long> = _recordingElapsedMs.asStateFlow()

    private val _currentlyPlayingPath = MutableStateFlow<String?>(null)
    val currentlyPlayingPath: StateFlow<String?> = _currentlyPlayingPath.asStateFlow()

    private val _isPlayerPlaying = MutableStateFlow(false)
    val isPlayerPlaying: StateFlow<Boolean> = _isPlayerPlaying.asStateFlow()

    private val _renameTarget = MutableStateFlow<File?>(null)
    val renameTarget: StateFlow<File?> = _renameTarget.asStateFlow()

    private val _userMessageRes = MutableStateFlow<Int?>(null)
    val userMessageRes: StateFlow<Int?> = _userMessageRes.asStateFlow()

    private var recordingTickJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlayerPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    _currentlyPlayingPath.value = null
                    _isPlayerPlaying.value = false
                }
            }
        })
        refreshRecordings()
    }

    fun consumeMessage() {
        _userMessageRes.value = null
    }

    fun refreshRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = recorderManager.recordingsDirectory()
            val files = dir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".m4a", ignoreCase = true) }
                ?.sortedByDescending { it.lastModified() }
                ?: emptyList()
            val items = files.map { file ->
                val duration = readDurationMs(file)
                RecordingListItem(
                    file = file,
                    displayTitle = file.nameWithoutExtension,
                    durationMs = duration,
                )
            }
            _recordings.value = items
        }
    }

    private fun readDurationMs(file: File): Long? {
        return try {
            val r = MediaMetadataRetriever()
            r.setDataSource(file.absolutePath)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            r.release()
            d
        } catch (_: Exception) {
            null
        }
    }

    fun startRecording() {
        viewModelScope.launch {
            val result = recorderManager.startRecording()
            if (result.isFailure) {
                _userMessageRes.value = R.string.dictaphone_error_recording
                return@launch
            }
            recordingTickJob?.cancel()
            recordingTickJob = viewModelScope.launch {
                val start = SystemClock.elapsedRealtime()
                while (isActive && recorderManager.isRecording.value) {
                    _recordingElapsedMs.value = SystemClock.elapsedRealtime() - start
                    delay(100)
                }
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            recordingTickJob?.cancel()
            recordingTickJob = null
            _recordingElapsedMs.value = 0L
            recorderManager.stopRecording()
            refreshRecordings()
            libraryRepository.updateLibraryFromMediaStore()
        }
    }

    fun playRecording(file: File) {
        val path = file.absolutePath
        if (_currentlyPlayingPath.value == path) {
            if (player.isPlaying) player.pause() else player.play()
            return
        }
        _currentlyPlayingPath.value = path
        player.setMediaItem(MediaItem.fromUri(file.toUri()))
        player.prepare()
        player.play()
    }

    fun stopPlayback() {
        player.stop()
        _currentlyPlayingPath.value = null
        _isPlayerPlaying.value = false
    }

    fun requestRename(file: File) {
        _renameTarget.value = file
    }

    fun dismissRename() {
        _renameTarget.value = null
    }

    fun confirmRename(newBaseName: String) {
        val target = _renameTarget.value ?: return
        val trimmed = newBaseName.trim().ifEmpty { return }
        val safe = trimmed.replace(Regex("[\\\\/:*?\"<>|]"), "_")
        viewModelScope.launch(Dispatchers.IO) {
            val dir = target.parentFile ?: return@launch
            val newFile = File(dir, "$safe.m4a")
            if (newFile.exists() && newFile != target) {
                _userMessageRes.value = R.string.dictaphone_rename_exists
                return@launch
            }
            val playingPath = _currentlyPlayingPath.value
            if (playingPath == target.absolutePath) {
                withContext(Dispatchers.Main) { stopPlayback() }
            }
            val ok = target.renameTo(newFile)
            if (ok) {
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(newFile.absolutePath),
                    arrayOf("audio/mp4"),
                    null,
                )
                dismissRename()
                refreshRecordings()
                libraryRepository.updateLibraryFromMediaStore()
            } else {
                _userMessageRes.value = R.string.dictaphone_rename_failed
            }
        }
    }

    fun deleteRecording(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_currentlyPlayingPath.value == file.absolutePath) {
                withContext(Dispatchers.Main) { stopPlayback() }
            }
            if (file.delete()) {
                refreshRecordings()
                libraryRepository.updateLibraryFromMediaStore()
            } else {
                _userMessageRes.value = R.string.dictaphone_delete_failed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}
