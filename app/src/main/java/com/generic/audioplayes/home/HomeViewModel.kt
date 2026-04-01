package com.generic.audioplayes.home

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.generic.audioplayes.Constants
import com.generic.audioplayes.R
import com.generic.audioplayes.components.SortOptions
import com.generic.audioplayes.data.QueueStateProvider
import com.generic.audioplayes.data.ZenCrashReporter
import com.generic.audioplayes.data.library.LibraryRepository
import com.generic.audioplayes.data.ZenPreferenceProvider
import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.PlaylistWithSongCount
import com.generic.audioplayes.data.music.SmartPlaylistCounts
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.BlacklistService
import com.generic.audioplayes.data.services.PlayerService
import com.generic.audioplayes.data.services.PlaylistService
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.data.services.SongService
import com.generic.audioplayes.storage_explorer.Directory
import com.generic.audioplayes.storage_explorer.DirectoryContents
import com.generic.audioplayes.storage_explorer.MusicFileExplorer
import com.generic.audioplayes.player.ZenPlayer
import com.generic.audioplayes.util.AudioFileActions
import com.generic.audioplayes.util.MediaDeleteResult
import com.generic.audioplayes.util.MessageStore
import com.generic.audioplayes.util.NaturalOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val messageStore: MessageStore,
    private val exoPlayer: ExoPlayer,
    private val songExtractor: SongExtractor,
    private val libraryRepository: LibraryRepository,
    private val prefs: ZenPreferenceProvider,
    private val playlistService: PlaylistService,
    private val blacklistService: BlacklistService,
    private val songService: SongService,
    private val queueService: QueueService,
    private val playerService: PlayerService,
    private val queueStateProvider: QueueStateProvider,
    private val crashReporter: ZenCrashReporter,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val deleteConfirmationSenderInternal = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    val deleteConfirmationSender = deleteConfirmationSenderInternal.asSharedFlow()

    @Volatile
    private var pendingDeleteSong: Song? = null

    @Volatile
    private var pendingDeleteUri: Uri? = null

    val songs = songService.songs
        .combine(prefs.songSortOrder){ songs, sortOrder ->
            when(sortOrder){
                SortOptions.TitleASC.ordinal -> songs.sortedWith(compareBy(NaturalOrder.stringComparator) { it.title })
                SortOptions.TitleDSC.ordinal -> songs.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.title })
                SortOptions.AlbumASC.ordinal -> songs.sortedWith(compareBy(NaturalOrder.stringComparator) { it.album })
                SortOptions.AlbumDSC.ordinal -> songs.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.album })
                SortOptions.ArtistASC.ordinal -> songs.sortedWith(compareBy(NaturalOrder.stringComparator) { it.artist })
                SortOptions.ArtistDSC.ordinal -> songs.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.artist })
                SortOptions.YearASC.ordinal -> songs.sortedBy { it.year }
                SortOptions.YearDSC.ordinal -> songs.sortedByDescending { it.year }
                SortOptions.DurationASC.ordinal -> songs.sortedBy { it.durationMillis }
                SortOptions.DurationDSC.ordinal -> songs.sortedByDescending { it.durationMillis }
                else -> songs
            }
        }.catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val albums = songService.albums
        .combine(prefs.albumSortOrder){ albums, sortOrder ->
            when(sortOrder){
                SortOptions.TitleASC.ordinal -> albums.sortedWith(compareBy(NaturalOrder.stringComparator) { it.name })
                SortOptions.TitleDSC.ordinal -> albums.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.name })
                else -> albums
            }
        }.catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _selectedPerson = MutableStateFlow(Person.Artist)
    val selectedPerson = _selectedPerson.asStateFlow()

    fun onPersonSelect(person: Person) {
        _selectedPerson.update { person }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val personsWithSongCount = _selectedPerson
        .flatMapLatest {
            when (it) {
                Person.Artist -> songService.artists
                Person.AlbumArtist -> songService.albumArtists
                Person.Composer -> songService.composers
                Person.Lyricist -> songService.lyricists
            }
        }.combine(prefs.artistSortOrder){ artists, sortOrder ->
            when(sortOrder){
                SortOptions.NameASC.ordinal -> artists.sortedWith(compareBy(NaturalOrder.stringComparator) { it.name })
                SortOptions.NameDSC.ordinal -> artists.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.name })
                SortOptions.SongsCountASC.ordinal -> artists.sortedBy { it.count }
                SortOptions.SongsCountDSC.ordinal -> artists.sortedByDescending { it.count }
                else -> artists
            }
        }.catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val playlistsWithSongCount = playlistService.playlists
        .combine(prefs.playlistSortOrder){ playlists, sortOrder ->
            when(sortOrder){
                SortOptions.NameASC.ordinal -> playlists.sortedWith(compareBy(NaturalOrder.stringComparator) { it.playlistName })
                SortOptions.NameDSC.ordinal -> playlists.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.playlistName })
                SortOptions.SongsCountASC.ordinal -> playlists.sortedBy { it.count }
                SortOptions.SongsCountDSC.ordinal -> playlists.sortedByDescending { it.count }
                else -> playlists
            }
        }.catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val smartPlaylistCounts = songService.smartPlaylistCounts
        .catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SmartPlaylistCounts(0, 0, 0, 0),
        )

    val genresWithSongCount = songService.genres
        .combine(prefs.genreSortOrder){ genres, sortOrder ->
            when(sortOrder){
                SortOptions.NameASC.ordinal -> genres.sortedWith(compareBy(NaturalOrder.stringComparator) { it.genreName })
                SortOptions.NameDSC.ordinal -> genres.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.genreName })
                SortOptions.SongsCountASC.ordinal -> genres.sortedBy { it.count }
                SortOptions.SongsCountDSC.ordinal -> genres.sortedByDescending { it.count }
                else -> genres
            }
        }.catch { exception ->
            Timber.e(exception)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun saveSortOption(screen: Int, option: Int){
        prefs.updateSortOrder(screen, option)
    }

    val currentSong = queueService.currentSong

    val queue = mutableStateListOf<Song>()

    val repeatMode = queueService.repeatMode

    fun toggleRepeatMode(){
        queueService.updateRepeatMode(repeatMode.value.next())
    }

    private val _currentSongPlaying = MutableStateFlow<Boolean?>(null)
    val currentSongPlaying = _currentSongPlaying.asStateFlow()

    private val queueServiceListener = object : QueueService.Listener {
        override fun onAppend(song: Song) {
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.add(song) }
        }

        override fun onAppend(songs: List<Song>) {
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.addAll(songs) }
        }

        override fun onUpdate(updatedSong: Song, position: Int) {
            if (position < 0 || position >= queue.size) return
            viewModelScope.launch(Dispatchers.Main.immediate) { queue[position] = updatedSong }
        }

        override fun onMove(from: Int, to: Int) {
            if (from < 0 || to < 0 || from >= queue.size || to >= queue.size) return
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.apply { add(to, removeAt(from)) } }
        }

        override fun onClear() {
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.clear() }
        }

        override fun onSetQueue(songs: List<Song>, startPlayingFromPosition: Int) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                queue.apply {
                    clear()
                    addAll(songs)
                }
            }
        }
    }

    private val exoPlayerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            _currentSongPlaying.update { isPlaying }
        }
    }

    init {
        queueService.addListener(queueServiceListener)
        viewModelScope.launch(Dispatchers.IO) {
            libraryRepository.loadLibraryFromCache()
            try {
                queueStateProvider.restoreQueueIfPossible(songService, queueService)
            } catch (e: Exception) {
                crashReporter.logException(e)
                Timber.e(e, "HomeViewModel: restore queue failed, clearing persisted state")
                try {
                    queueStateProvider.clearPersistedState()
                } catch (e2: Exception) {
                    crashReporter.logException(e2)
                }
            }
            libraryRepository.updateLibraryFromMediaStore()
        }
        _currentSongPlaying.update { exoPlayer.isPlaying }
        exoPlayer.addListener(exoPlayerListener)
    }

    fun onMiniPlayerPlayPause() {
        viewModelScope.launch {
            if (queueService.queue.isEmpty()) return@launch
            if (ZenPlayer.isRunning.get()) {
                playerService.togglePlayPauseIfRunning()
            } else {
                playerService.startServiceIfNotRunning(
                    songs = queueService.queue,
                    startPlayingFromPosition = queueService.currentQueueIndex(),
                    startPositionMs = queueStateProvider.readStartPositionMs(),
                    autoPlay = true,
                    usePersistedShuffle = true,
                )
            }
        }
    }

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private fun showMessage(message: String){
        viewModelScope.launch {
            _message.update { message }
            delay(Constants.MESSAGE_DURATION)
            _message.update { "" }
        }
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.removeListener(exoPlayerListener)
        explorer.removeListener(directoryChangeListener)
        queueService.removeListener(queueServiceListener)
    }

    /**
     * Shuffle the queue and start playing from first song
     */
    fun shufflePlay(songs: List<Song>?) = setQueue(songs?.shuffled(), 0)

    fun onSongBlacklist(song: Song) {
        viewModelScope.launch {
            try {
                blacklistService.blacklistSongs(listOf(song))
                refreshQueueAfterRemovingSong(song)
                showMessage(messageStore.getString(R.string.done))
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(messageStore.getString(R.string.some_error_occurred))
            }
        }
    }

    /**
     * Deletes the file from storage and removes DB metadata; updates queue/player.
     * On [MediaDeleteResult.Recoverable], UI must launch [deleteConfirmationSender] and then call
     * [onDeleteConfirmedByUser] or [onDeleteConfirmationCancelled].
     */
    fun deleteSongFromDevice(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = AudioFileActions.deleteAudioFileFromDevice(appContext, song.location)) {
                    MediaDeleteResult.Success -> completeDeleteAfterFileRemoved(song)
                    is MediaDeleteResult.Recoverable -> {
                        pendingDeleteSong = song
                        pendingDeleteUri = result.uri
                        deleteConfirmationSenderInternal.emit(result.pendingIntent)
                    }
                    MediaDeleteResult.Failed -> {
                        withContext(Dispatchers.Main) {
                            showMessage(messageStore.getString(R.string.player_delete_failed))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.some_error_occurred))
                }
            }
        }
    }

    /** Call after system delete confirmation (`Activity.RESULT_OK`). */
    fun onDeleteConfirmedByUser() {
        val song = pendingDeleteSong ?: return
        val uri = pendingDeleteUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (val result = AudioFileActions.deleteAudioFileByUri(appContext, uri)) {
                    MediaDeleteResult.Success -> {
                        pendingDeleteSong = null
                        pendingDeleteUri = null
                        completeDeleteAfterFileRemoved(song)
                    }
                    is MediaDeleteResult.Recoverable -> {
                        deleteConfirmationSenderInternal.emit(result.pendingIntent)
                    }
                    MediaDeleteResult.Failed -> {
                        pendingDeleteSong = null
                        pendingDeleteUri = null
                        withContext(Dispatchers.Main) {
                            showMessage(messageStore.getString(R.string.player_delete_failed))
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                pendingDeleteSong = null
                pendingDeleteUri = null
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.some_error_occurred))
                }
            }
        }
    }

    fun onDeleteConfirmationCancelled() {
        pendingDeleteSong = null
        pendingDeleteUri = null
    }

    private suspend fun completeDeleteAfterFileRemoved(song: Song) {
        songService.removeSongFromLibraryMetadata(song)
        withContext(Dispatchers.Main) {
            refreshQueueAfterRemovingSong(song)
        }
        libraryRepository.updateLibraryFromMediaStore()
        withContext(Dispatchers.Main) {
            showMessage(messageStore.getString(R.string.player_delete_ok))
        }
    }

    private suspend fun refreshQueueAfterRemovingSong(song: Song) {
        withContext(Dispatchers.Main) {
            val q = queueService.queue
            if (q.none { it.location == song.location }) return@withContext
            val removedIndex = q.indexOfFirst { it.location == song.location }
            val currentIndex = queueService.currentQueueIndex()
            val newQueue = q.filter { it.location != song.location }
            if (newQueue.isEmpty()) {
                queueService.clearQueue()
                playerService.stopPlaybackAndClearQueueIfRunning()
                return@withContext
            }
            val newStartIndex = when {
                removedIndex < currentIndex -> currentIndex - 1
                removedIndex == currentIndex -> minOf(currentIndex, newQueue.size - 1)
                else -> currentIndex
            }.coerceIn(0, newQueue.lastIndex)
            playerService.startServiceIfNotRunning(
                songs = newQueue,
                startPlayingFromPosition = newStartIndex,
                startPositionMs = 0L,
                autoPlay = true,
            )
        }
    }

    fun onFolderBlacklist(folder: Directory){
        viewModelScope.launch {
            try {
                blacklistService.blacklistFolders(listOf(folder.absolutePath))
                showMessage(messageStore.getString(R.string.done))
            } catch (_: Exception){
                showMessage(messageStore.getString(R.string.some_error_occurred))
            }
        }
    }

    fun onPlaylistCreate(playlistName: String) {
        viewModelScope.launch {
            playlistService.createPlaylist(playlistName)
        }
    }

    fun deletePlaylist(playlistWithSongCount: PlaylistWithSongCount) {
        viewModelScope.launch {
            try {
                playlistService.deletePlaylist(playlistWithSongCount.playlistId)
                showMessage(messageStore.getString(R.string.done))
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(messageStore.getString(R.string.some_error_occurred))
            }
        }
    }

    /**
     * Adds a song to the end of queue
     */
    fun addToQueue(song: Song) {
        crashReporter.logData("HomeViewModel.addToQueue(Song) isQueueEmpty:${queue.isEmpty()}")
        if (queue.isEmpty()) {
            viewModelScope.launch {
                playerService.startServiceIfNotRunning(listOf(song), 0)
                showMessage(messageStore.getString(R.string.playing))
            }
        } else {
            val result = queueService.append(song)
            if (result) {
                showMessage(messageStore.getString(R.string.added_to_queue, song.title))
            } else {
                showMessage(messageStore.getString(R.string.song_already_in_queue))
            }
        }
    }

    fun addToQueue(song: MiniSong) {
        val resolvedSong = songExtractor.resolveSong(song.location) ?: return
        addToQueue(resolvedSong)
    }

    /**
     * Create and set a new queue in exoplayer.
     * Old queue is discarded.
     * Playing starts immediately
     * @param songs queue items
     * @param startPlayingFromIndex index of song from which playing should start
     */
    fun setQueue(songs: List<Song>?, startPlayingFromIndex: Int = 0) {
        if (songs == null) return
        crashReporter.logData("HomeViewModel.setQueue()")
        viewModelScope.launch {
            playerService.startServiceIfNotRunning(songs, startPlayingFromIndex)
        }
        showMessage(messageStore.getString(R.string.playing))
    }

    /**
     * Toggle the favourite value of a song
     */
    fun changeFavouriteValue(song: Song? = currentSong.value) {
        if (song == null) return
        val updatedSong = song.copy(favourite = !song.favourite)
        viewModelScope.launch(Dispatchers.IO) {
            queueService.update(updatedSong)
            songService.updateSong(updatedSong)
        }
    }

    fun onSongDrag(fromIndex: Int, toIndex: Int) = queueService.moveSong(fromIndex, toIndex)


    private val _filesInCurrentDestination = MutableStateFlow(DirectoryContents())
    val filesInCurrentDestination = _filesInCurrentDestination
        .combine(prefs.folderSortOrder){ files, sortOrder ->
            when(sortOrder){
                SortOptions.NameASC.ordinal -> {
                    DirectoryContents(
                        directories = files.directories.sortedWith(compareBy(NaturalOrder.stringComparator) { it.name }),
                        songs = files.songs.sortedWith(compareBy(NaturalOrder.stringComparator) { it.title })
                    )
                }
                SortOptions.NameDSC.ordinal -> {
                    DirectoryContents(
                        directories = files.directories.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.name }),
                        songs = files.songs.sortedWith(compareByDescending(NaturalOrder.stringComparator) { it.title })
                    )
                }
                else -> files
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DirectoryContents()
        )

    private val explorer = MusicFileExplorer(songExtractor)

    private val _isExplorerAtRoot = MutableStateFlow(true)
    val isExplorerAtRoot = _isExplorerAtRoot.asStateFlow()

    private val directoryChangeListener = object : MusicFileExplorer.DirectoryChangeListener {
        override fun onDirectoryChanged(path: String, files: DirectoryContents) {
            _filesInCurrentDestination.update { files }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            explorer.addListener(directoryChangeListener)
        }
    }

    fun onFileClicked(songIndex: Int){
        viewModelScope.launch(Dispatchers.IO) {
            if(songIndex < 0 || songIndex >= filesInCurrentDestination.value.songs.size) return@launch
            val song = songExtractor.resolveSong(filesInCurrentDestination.value.songs[songIndex].location)
            song?.let {
                setQueue(listOf(song))
            }
        }
    }

    fun onFileClicked(file: Directory){
        viewModelScope.launch(Dispatchers.IO) {
            explorer.moveInsideDirectory(file.absolutePath)
            _isExplorerAtRoot.update { explorer.isRoot }
        }
    }

    fun moveToParent() {
        viewModelScope.launch(Dispatchers.IO) {
            explorer.moveToParent()
            _isExplorerAtRoot.update { explorer.isRoot }
        }
    }

    private val _switchToFoldersTab = MutableStateFlow(false)
    val switchToFoldersTab = _switchToFoldersTab.asStateFlow()

    fun consumeSwitchToFoldersTab() {
        _switchToFoldersTab.value = false
    }

    /**
     * Opens the file browser at [absolutePath] and requests the Home UI to switch to the Folders tab.
     * Used from global search when the user picks a folder result.
     */
    fun navigateToFolderInExplorer(absolutePath: String) {
        _switchToFoldersTab.value = true
        viewModelScope.launch(Dispatchers.IO) {
            explorer.moveInsideDirectory(absolutePath)
            _isExplorerAtRoot.update { explorer.isRoot }
        }
    }
}