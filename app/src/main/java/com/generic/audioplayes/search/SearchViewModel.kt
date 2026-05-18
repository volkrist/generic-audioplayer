package com.generic.audioplayes.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.generic.audioplayes.Constants
import com.generic.audioplayes.R
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.search.SearchRepository
import com.generic.audioplayes.data.services.PlayerService
import com.generic.audioplayes.util.MessageStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val messageStore: MessageStore,
    private val playerService: PlayerService,
    private val searchRepository: SearchRepository,
    private val crashReporter: AudioPlayerCrashReporter,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val searchResult = _query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { raw ->
            flow {
                val trimmed = raw.trim()
                if (trimmed.isEmpty()) {
                    emit(SearchResult())
                } else {
                    emit(SearchResult(isLoading = true))
                    emit(searchRepository.search(trimmed))
                }
            }
        }
        .catch { exception ->
            Timber.e(exception)
            crashReporter.logException(exception as? Exception)
            emit(SearchResult(errorMsg = messageStore.getString(R.string.some_error_occurred)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SearchResult(),
        )

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    fun clearQueryText() {
        _query.update { "" }
    }

    fun updateQuery(query: String) {
        _query.update { query }
    }

    fun setQueue(songs: List<Song>?, startPlayingFromIndex: Int = 0) {
        if (songs == null) return
        crashReporter.logData("SearchViewModel.setQueue()")
        viewModelScope.launch {
            playerService.startServiceIfNotRunning(songs, startPlayingFromIndex)
        }
        showMessage(messageStore.getString(R.string.playing))
    }

    /**
     * Queues the whole current search result section (library songs or dictaphone list) so playback
     * continues with the next row after the current track ends.
     */
    fun playSongFromSearchResults(song: Song) {
        val result = searchResult.value
        val fromLibrary = result.songs
        val idxLib = fromLibrary.indexOfFirst { it.location == song.location }
        if (idxLib >= 0 && fromLibrary.isNotEmpty()) {
            setQueue(fromLibrary, idxLib)
            return
        }
        val fromDict = result.dictaphoneRecordings
        val idxDict = fromDict.indexOfFirst { it.location == song.location }
        if (idxDict >= 0 && fromDict.isNotEmpty()) {
            setQueue(fromDict, idxDict)
            return
        }
        setQueue(listOf(song), 0)
    }

    private fun showMessage(message: String) {
        viewModelScope.launch {
            _message.update { message }
            delay(Constants.MESSAGE_DURATION)
            _message.update { "" }
        }
    }
}
