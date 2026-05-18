package com.generic.audioplayes.tags

import android.app.PendingIntent
import android.content.Context
import android.media.MediaScannerConnection
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.generic.audioplayes.R
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.library.LibraryRepository
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SongService
import com.generic.audioplayes.util.MessageStore
import com.generic.audioplayes.util.Stage4DebugLog
import com.generic.audioplayes.util.pathCandidatesForLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val messageStore: MessageStore,
    private val songExtractor: SongExtractor,
    private val songService: SongService,
    private val queueService: QueueService,
    private val libraryRepository: LibraryRepository,
    private val crashReporter: AudioPlayerCrashReporter,
) : ViewModel() {

    private val songPath: String = savedStateHandle.get<String>("songPath")
        ?: savedStateHandle.get<String>("song_location")
        ?: error("songPath navigation argument required")

    private val _ui = MutableStateFlow(TagEditorUiState(loading = true))
    val ui: StateFlow<TagEditorUiState> = _ui.asStateFlow()

    private val writeConfirmationChannel = Channel<PendingIntent>(Channel.BUFFERED)
    val writeConfirmationSender = writeConfirmationChannel.receiveAsFlow()

    private var pendingSaveDraft: TagEditDraft? = null
    private var pendingSaveCoverBytes: ByteArray? = null
    private var pendingSaveCoverMime: String? = null

    init {
        Stage4DebugLog.i("TagEditor opened args songPath=$songPath")
        viewModelScope.launch(Dispatchers.IO) {
            loadEditorState()
        }
    }

    private suspend fun loadEditorState() {
        val roomSong = songService.getSongByLocation(songPath)
        val mediaSong = songExtractor.resolveSong(songPath)
        val baseSong = roomSong ?: mediaSong
        Stage4DebugLog.i(
            "Room song found=${roomSong != null} MediaStore song found=${mediaSong != null} trackPath=$songPath",
        )
        val fromTags = AudioTagEditor.readFile(songPath).getOrElse { e ->
            Stage4DebugLog.w("JAudioTagger read failed trackPath=$songPath err=${e.message}")
            null
        }
        if (fromTags != null) {
            Stage4DebugLog.i("JAudioTagger read success trackPath=$songPath")
        }
        val songDraft = draftFromSong(baseSong)
        val draft = when {
            fromTags != null && songDraft != null -> fromTags.fillBlanksFrom(songDraft)
            fromTags != null -> fromTags
            else -> songDraft
        }
        if (draft == null) {
            Stage4DebugLog.w("TagEditor load failed: no draft trackPath=$songPath")
            _ui.update {
                it.copy(
                    loading = false,
                    loadError = messageStore.getString(R.string.tag_editor_load_failed),
                )
            }
            return
        }
        Stage4DebugLog.i(
            "draft title=${draft.title} draft artist=${draft.artist} draft album=${draft.album}",
        )
        val label = draft.title.ifBlank {
            baseSong?.title?.ifBlank { songPath.substringAfterLast('/') }
                ?: songPath.substringAfterLast('/')
        }
        _ui.update {
            it.copy(
                loading = false,
                fileLabel = label,
                draft = draft,
                loadError = null,
            )
        }
    }

    private fun draftFromSong(song: Song?): TagEditDraft? {
        if (song == null) return null
        fun clean(value: String?) = value?.takeIf { it.isNotBlank() && it != "Unknown" }.orEmpty()
        return TagEditDraft(
            title = song.title,
            artist = clean(song.artist),
            album = clean(song.album),
            albumArtist = clean(song.albumArtist),
            year = song.year.takeIf { it > 0 }?.toString().orEmpty(),
            genre = clean(song.genre),
            lyricist = clean(song.lyricist),
            comment = song.comment.orEmpty(),
        )
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
        if (state.saving || state.loading) return
        Stage4DebugLog.i(
            "Save tags clicked draft before save title=${state.draft.title} artist=${state.draft.artist} album=${state.draft.album}",
        )
        viewModelScope.launch(Dispatchers.IO) {
            performSave(state, writeConsentGranted = false)
        }
    }

    fun onWriteConsentGranted() {
        val draft = pendingSaveDraft ?: _ui.value.draft
        val coverBytes = pendingSaveCoverBytes
        val coverMime = pendingSaveCoverMime
        val clearCover = _ui.value.clearCover
        Stage4DebugLog.i("write consent granted, retry file write path=$songPath")
        viewModelScope.launch(Dispatchers.IO) {
            performSave(
                _ui.value.copy(
                    draft = draft,
                    pickedCoverBytes = coverBytes,
                    pickedCoverMime = coverMime,
                    clearCover = clearCover,
                ),
                writeConsentGranted = true,
            )
        }
    }

    fun onWriteConsentDenied() {
        pendingSaveDraft = null
        pendingSaveCoverBytes = null
        pendingSaveCoverMime = null
        _ui.update { it.copy(saving = false) }
        Stage4DebugLog.w("write consent denied path=$songPath")
    }

    private suspend fun performSave(state: TagEditorUiState, writeConsentGranted: Boolean) {
        _ui.update { it.copy(saving = true, errorMsg = null, done = false) }
        try {
            val coverBytes = when {
                state.clearCover -> ByteArray(0)
                state.pickedCoverBytes != null -> state.pickedCoverBytes
                else -> null
            }
            // Update Room first so the library shows the new title immediately.
            // Do NOT call updateLibraryFromMediaStore() here — it re-reads the file and overwrites Room.
            val roomUpdated = persistDraftToLibrary(state.draft)
            Stage4DebugLog.i("Room update result=$roomUpdated trackPath=$songPath")
            when (
                val writeResult = AudioTagEditor.writeFile(
                    appContext,
                    songPath,
                    state.draft,
                    coverBytes,
                    state.pickedCoverMime,
                    writeConsentGranted = writeConsentGranted,
                )
            ) {
                TagFileWriteResult.Success -> {
                    pendingSaveDraft = null
                    pendingSaveCoverBytes = null
                    pendingSaveCoverMime = null
                    MediaScannerConnection.scanFile(
                        appContext,
                        pathCandidatesForLookup(songPath).toTypedArray(),
                        null,
                        null,
                    )
                    libraryRepository.updateLibraryFromMediaStore()
                    Stage4DebugLog.i("writeFile result=OK trackPath=$songPath UI refresh triggered")
                    _ui.update { it.copy(saving = false, done = true, errorMsg = null) }
                }
                is TagFileWriteResult.NeedsWriteConsent -> {
                    pendingSaveDraft = state.draft
                    pendingSaveCoverBytes = coverBytes
                    pendingSaveCoverMime = state.pickedCoverMime
                    val sent = writeConfirmationChannel.trySend(writeResult.pendingIntent).isSuccess
                    _ui.update { it.copy(saving = false) }
                    Stage4DebugLog.i(
                        "writeFile waiting for user write consent path=$songPath channelSent=$sent",
                    )
                    if (!sent) {
                        _ui.update {
                            it.copy(
                                errorMsg = messageStore.getString(R.string.tag_editor_write_failed),
                                done = roomUpdated,
                            )
                        }
                    }
                }
                is TagFileWriteResult.Error -> {
                    pendingSaveDraft = null
                    val e = writeResult.cause
                    Stage4DebugLog.e("writeFile result=FAIL trackPath=$songPath err=${e.message}", e)
                    crashReporter.logException(e as? Exception ?: Exception(e))
                    _ui.update {
                        it.copy(
                            saving = false,
                            errorMsg = if (roomUpdated) {
                                messageStore.getString(R.string.tag_editor_saved_library_only)
                            } else {
                                messageStore.getString(R.string.tag_editor_write_failed)
                            },
                            done = roomUpdated,
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Stage4DebugLog.e("performSave failed path=$songPath", e)
            crashReporter.logException(e)
            _ui.update {
                it.copy(
                    saving = false,
                    errorMsg = messageStore.getString(R.string.tag_editor_write_failed),
                )
            }
        }
    }

    private suspend fun persistDraftToLibrary(draft: TagEditDraft): Boolean {
        val existing = songService.getSongByLocation(songPath)
            ?: songExtractor.resolveSong(songPath)
        if (existing == null) {
            Stage4DebugLog.w("persistDraftToLibrary: no Room row trackPath=$songPath")
            return false
        }
        fun tagOr(existingVal: String, draftVal: String) =
            draftVal.trim().ifEmpty { existingVal }.ifEmpty { "Unknown" }
        val updated = existing.copy(
            title = draft.title.trim().ifEmpty { existing.title },
            artist = tagOr(existing.artist, draft.artist),
            album = tagOr(existing.album, draft.album),
            albumArtist = tagOr(existing.albumArtist, draft.albumArtist),
            genre = tagOr(existing.genre, draft.genre),
            lyricist = tagOr(existing.lyricist, draft.lyricist),
            comment = draft.comment.trim().ifEmpty { existing.comment },
            year = draft.year.trim().toIntOrNull()?.takeIf { it > 0 } ?: existing.year,
        )
        songService.updateSong(updated)
        queueService.update(updated)
        return true
    }
}

private fun TagEditDraft.fillBlanksFrom(fallback: TagEditDraft): TagEditDraft = copy(
    title = title.ifBlank { fallback.title },
    artist = artist.ifBlank { fallback.artist },
    album = album.ifBlank { fallback.album },
    albumArtist = albumArtist.ifBlank { fallback.albumArtist },
    year = year.ifBlank { fallback.year },
    genre = genre.ifBlank { fallback.genre },
    lyricist = lyricist.ifBlank { fallback.lyricist },
    comment = comment.ifBlank { fallback.comment },
    trackNumber = trackNumber.ifBlank { fallback.trackNumber },
    embeddedCoverBytes = embeddedCoverBytes ?: fallback.embeddedCoverBytes,
)

data class TagEditorUiState(
    val loading: Boolean = false,
    val fileLabel: String = "",
    val draft: TagEditDraft = TagEditDraft(),
    val loadError: String? = null,
    val saving: Boolean = false,
    val errorMsg: String? = null,
    val done: Boolean = false,
    val pickedCoverBytes: ByteArray? = null,
    val pickedCoverMime: String? = null,
    val clearCover: Boolean = false,
)
