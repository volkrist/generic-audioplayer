package com.generic.audioplayes.tags

import android.content.Context
import android.media.MediaScannerConnection
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.library.LibraryRepository
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SongService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val songExtractor: SongExtractor,
    private val songService: SongService,
    private val queueService: QueueService,
    private val libraryRepository: LibraryRepository,
    private val crashReporter: AudioPlayerCrashReporter,
) : ViewModel() {

    private val songPath: String = savedStateHandle["songPath"]
        ?: error("songPath required")

    private val _ui = MutableStateFlow(TagEditorUiState())
    val ui: StateFlow<TagEditorUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val song = songExtractor.resolveSong(songPath)
            val fromTags = AudioTagEditor.readFile(songPath).getOrNull()
            val draft = fromTags ?: TagEditDraft(
                title = song?.title.orEmpty(),
                artist = song?.artist?.takeIf { it != "Unknown" }.orEmpty(),
                album = song?.album?.takeIf { it != "Unknown" }.orEmpty(),
                albumArtist = song?.albumArtist?.takeIf { it != "Unknown" }.orEmpty(),
                year = song?.year?.takeIf { it > 0 }?.toString().orEmpty(),
                genre = song?.genre?.takeIf { it != "Unknown" }.orEmpty(),
                lyricist = song?.lyricist?.takeIf { it != "Unknown" }.orEmpty(),
                comment = song?.comment.orEmpty(),
            )
            _ui.update {
                it.copy(
                    fileLabel = song?.title?.ifBlank { songPath.substringAfterLast('/') } ?: songPath.substringAfterLast('/'),
                    draft = draft,
                    loadError = if (fromTags == null && song == null) "load" else null,
                )
            }
        }
    }

    fun updateDraft(transform: (TagEditDraft) -> TagEditDraft) {
        _ui.update { s -> s.copy(draft = transform(s.draft)) }
    }

    fun setPickedCover(bytes: ByteArray?, mimeType: String?) {
        _ui.update { it.copy(pickedCoverBytes = bytes, pickedCoverMime = mimeType, clearCover = false) }
    }

    fun clearCover() {
        _ui.update { it.copy(clearCover = true, pickedCoverBytes = null, pickedCoverMime = null) }
    }

    fun save() {
        val state = _ui.value
        if (state.saving) return
        viewModelScope.launch(Dispatchers.IO) {
            _ui.update { it.copy(saving = true, errorMsg = null, done = false) }
            val coverBytes = when {
                state.clearCover -> ByteArray(0)
                state.pickedCoverBytes != null -> state.pickedCoverBytes
                else -> null
            }
            val coverMime = state.pickedCoverMime
            val result = AudioTagEditor.writeFile(
                songPath,
                state.draft,
                coverBytes,
                coverMime,
            )
            result.onFailure { e ->
                crashReporter.logException(e as? Exception ?: Exception(e))
                _ui.update {
                    it.copy(
                        saving = false,
                        errorMsg = e.message ?: "write_failed",
                    )
                }
                return@launch
            }
            try {
                MediaScannerConnection.scanFile(
                    appContext,
                    arrayOf(songPath),
                    null,
                ) { _, _ -> }
                libraryRepository.updateLibraryFromMediaStore()
                val updated = songExtractor.resolveSong(songPath)
                if (updated != null) {
                    songService.updateSong(updated)
                    queueService.update(updated)
                }
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            _ui.update { it.copy(saving = false, done = true) }
        }
    }
}

data class TagEditorUiState(
    val fileLabel: String = "",
    val draft: TagEditDraft = TagEditDraft(),
    val loadError: String? = null,
    val saving: Boolean = false,
    val errorMsg: String? = null,
    val done: Boolean = false,
    val pickedCoverBytes: ByteArray? = null,
    val pickedCoverMime: String? = null,
    /** User asked to remove embedded art on save */
    val clearCover: Boolean = false,
)
