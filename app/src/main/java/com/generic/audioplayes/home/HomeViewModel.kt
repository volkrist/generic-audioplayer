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
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.library.LibraryRepository
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
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
import com.generic.audioplayes.player.AudioPlayerService
import com.generic.audioplayes.player.toMediaItem
import com.generic.audioplayes.util.AudioFileActions
import com.generic.audioplayes.util.MediaDeleteResult
import com.generic.audioplayes.util.MessageStore
import com.generic.audioplayes.util.NaturalOrder
import com.generic.audioplayes.util.sortedByFolderPlaybackOrder
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import java.io.File
import java.util.LinkedHashMap
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val messageStore: MessageStore,
    private val exoPlayer: ExoPlayer,
    private val songExtractor: SongExtractor,
    private val libraryRepository: LibraryRepository,
    private val prefs: AudioPlayerPreferenceProvider,
    private val playlistService: PlaylistService,
    private val blacklistService: BlacklistService,
    private val songService: SongService,
    private val queueService: QueueService,
    private val playerService: PlayerService,
    private val queueStateProvider: QueueStateProvider,
    private val crashReporter: AudioPlayerCrashReporter,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val deleteConfirmationSenderInternal = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    val deleteConfirmationSender = deleteConfirmationSenderInternal.asSharedFlow()

    @Volatile
    private var pendingDeleteSong: Song? = null

    @Volatile
    private var pendingDeleteUri: Uri? = null

    /** When non-null, [onDeleteConfirmedByUser] continues [deleteFolderFromDevice] after a recoverable delete. */
    @Volatile
    private var pendingFolderDelete: Directory? = null

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

        override fun onInsert(atIndex: Int, songs: List<Song>) {
            viewModelScope.launch(Dispatchers.Main.immediate) {
                queue.addAll(atIndex, songs)
            }
        }

        override fun onUpdate(updatedSong: Song, position: Int) {
            if (position < 0 || position >= queue.size) return
            viewModelScope.launch(Dispatchers.Main.immediate) { queue[position] = updatedSong }
        }

        override fun onMove(from: Int, to: Int) {
            if (from < 0 || to < 0 || from >= queue.size || to >= queue.size) return
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.apply { add(to, removeAt(from)) } }
        }

        override fun onRemoveAt(index: Int) {
            if (index < 0 || index >= queue.size) return
            viewModelScope.launch(Dispatchers.Main.immediate) { queue.removeAt(index) }
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
            syncExoPlayerWithPersistedQueueIfServiceStopped()
            libraryRepository.updateLibraryFromMediaStore()
        }
        _currentSongPlaying.update { exoPlayer.isPlaying }
        exoPlayer.addListener(exoPlayerListener)
    }

    fun onMiniPlayerPlayPause() {
        viewModelScope.launch {
            if (queueService.queue.isEmpty()) return@launch
            if (AudioPlayerService.isRunning.get()) {
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
                when (val result = AudioFileActions.deleteAudioFileFromDeviceWithFallback(appContext, song.location)) {
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
                        val folder = pendingFolderDelete
                        if (folder != null) {
                            deleteFolderFromDevice(folder, fromContinuation = true)
                        }
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
        pendingFolderDelete = null
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

    suspend fun getSongsInFolderRecursive(directory: Directory): List<Song> =
        withContext(Dispatchers.IO) {
            songExtractor.extractAllSongsUnderFolderRecursive(directory.absolutePath)
        }

    fun playAllInFolder(folder: Directory) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = songExtractor.extractAllSongsUnderFolderRecursive(folder.absolutePath)
            if (songs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.folder_empty_or_inaccessible))
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                setQueue(songs, 0)
            }
        }
    }

    fun playFolderNext(folder: Directory) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = songExtractor.extractAllSongsUnderFolderRecursive(folder.absolutePath)
            if (songs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.folder_empty_or_inaccessible))
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (queueService.queue.isEmpty()) {
                    setQueue(songs, 0)
                } else {
                    queueService.insertSongsAfterCurrent(songs)
                    showMessage(messageStore.getString(R.string.folder_queued_after_current))
                }
            }
        }
    }

    fun addFolderToQueue(folder: Directory) {
        viewModelScope.launch(Dispatchers.IO) {
            val songs = songExtractor.extractAllSongsUnderFolderRecursive(folder.absolutePath)
            if (songs.isEmpty()) {
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.folder_empty_or_inaccessible))
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                if (queueService.queue.isEmpty()) {
                    setQueue(songs, 0)
                } else {
                    queueService.append(songs)
                    showMessage(messageStore.getString(R.string.folder_added_to_queue))
                }
            }
        }
    }

    /**
     * After cold start / sleep timer, the queue is restored in [QueueService] but the shared [ExoPlayer]
     * was cleared when the service stopped — load media + seek so the UI shows the saved position.
     */
    private suspend fun syncExoPlayerWithPersistedQueueIfServiceStopped() {
        if (AudioPlayerService.isRunning.get()) return
        if (queueService.queue.isEmpty()) return
        val idx = queueService.currentQueueIndex()
        val pos = queueStateProvider.readStartPositionMs()
        withContext(Dispatchers.Main) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItems(queueService.queue.map { it.toMediaItem() })
            exoPlayer.prepare()
            exoPlayer.seekTo(idx, pos)
            exoPlayer.pause()
        }
    }

    private fun deleteDirectoryRecursiveBestEffort(dir: File): Boolean {
        if (!dir.exists()) return true
        return try {
            if (dir.isDirectory) {
                dir.listFiles()?.forEach { child ->
                    if (child.isDirectory) {
                        deleteDirectoryRecursiveBestEffort(child)
                    } else {
                        child.delete()
                    }
                }
            }
            dir.delete()
        } catch (e: Exception) {
            Timber.e(e, "deleteDirectoryRecursiveBestEffort")
            try {
                dir.deleteRecursively()
            } catch (e2: Exception) {
                Timber.e(e2)
                false
            }
        }
    }

    /**
     * Prefer Room paths (same as single-track delete), merge with MediaStore scan for files not yet indexed.
     */
    private suspend fun resolveFolderSongsForDeletion(folder: Directory): List<Song> {
        val raw = folder.absolutePath.trimEnd('/')
        val candidates = buildList {
            add(raw)
            try {
                add(File(raw).canonicalFile.absolutePath.trimEnd('/'))
            } catch (_: Exception) {
            }
        }.distinct()
        val merged = LinkedHashMap<String, Song>()
        for (p in candidates) {
            songService.getSongsUnderFolderPath(p).forEach { merged[it.location] = it }
        }
        val primary = candidates.firstOrNull() ?: return merged.values.toList()
        songExtractor.extractAllSongsUnderFolderRecursive(primary).forEach { song ->
            merged.putIfAbsent(song.location, song)
        }
        return merged.values.toList()
    }

    fun deleteFolderFromDevice(folder: Directory, fromContinuation: Boolean = false) {
        if (!fromContinuation) pendingFolderDelete = folder
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(folder.absolutePath)
                if (!dir.exists()) {
                    pendingFolderDelete = null
                    withContext(Dispatchers.Main) {
                        showMessage(messageStore.getString(R.string.folder_delete_failed))
                    }
                    return@launch
                }
                val songs = resolveFolderSongsForDeletion(folder)
                if (songs.isEmpty()) {
                    val ok = deleteDirectoryRecursiveBestEffort(dir)
                    withContext(Dispatchers.Main) {
                        pendingFolderDelete = null
                        if (ok) {
                            libraryRepository.updateLibraryFromMediaStore()
                            showMessage(messageStore.getString(R.string.folder_delete_ok))
                        } else {
                            showMessage(messageStore.getString(R.string.folder_delete_failed))
                        }
                    }
                    return@launch
                }
                var deleted = 0
                for (song in songs) {
                    when (
                        val result = AudioFileActions.deleteAudioFileFromDeviceWithFallback(
                            appContext,
                            song.location,
                        )
                    ) {
                        MediaDeleteResult.Success -> {
                            songService.removeSongFromLibraryMetadata(song)
                            withContext(Dispatchers.Main) {
                                refreshQueueAfterRemovingSong(song)
                            }
                            deleted++
                        }
                        is MediaDeleteResult.Recoverable -> {
                            pendingDeleteSong = song
                            pendingDeleteUri = result.uri
                            pendingFolderDelete = folder
                            deleteConfirmationSenderInternal.emit(result.pendingIntent)
                            return@launch
                        }
                        MediaDeleteResult.Failed -> Unit
                    }
                }
                libraryRepository.updateLibraryFromMediaStore()
                try {
                    dir.deleteRecursively()
                } catch (_: Exception) {
                    deleteDirectoryRecursiveBestEffort(dir)
                }
                withContext(Dispatchers.Main) {
                    pendingFolderDelete = null
                    if (deleted > 0) {
                        showMessage(messageStore.getString(R.string.folder_delete_ok))
                    } else {
                        showMessage(messageStore.getString(R.string.folder_delete_failed))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
                pendingFolderDelete = null
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.folder_delete_failed))
                }
            }
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

    fun addSongsToPlaylistFromPlayer(songLocations: List<String>, playlistId: Long) {
        viewModelScope.launch {
            try {
                val blacklisted = blacklistService.blacklistedSongs.first()
                    .map { it.location }
                    .toSet()
                val validSongs = songLocations.filter { !blacklisted.contains(it) }
                val anyBlacklisted = songLocations.any { blacklisted.contains(it) }
                if (validSongs.isEmpty()) {
                    showMessage(messageStore.getString(R.string.blacklisted_songs_have_not_been_added_to_playlist))
                    return@launch
                }
                playlistService.addSongsToPlaylist(validSongs, playlistId)
                val msg = messageStore.getString(R.string.done) +
                    if (anyBlacklisted) {
                        ". " + messageStore.getString(R.string.blacklisted_songs_have_not_been_added_to_playlist)
                    } else {
                        ""
                    }
                showMessage(msg)
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(messageStore.getString(R.string.some_error_occurred))
            }
        }
    }

    fun createPlaylistAndAddSongsFromPlayer(playlistName: String, songLocations: List<String>) {
        viewModelScope.launch {
            try {
                if (!playlistService.createPlaylist(playlistName)) {
                    showMessage(messageStore.getString(R.string.some_error_occurred))
                    return@launch
                }
                val playlists = playlistService.playlists.first()
                val trimmed = playlistName.trim()
                val playlist = playlists
                    .filter { it.playlistName == trimmed }
                    .maxByOrNull { it.createdAt }
                    ?: playlists.maxByOrNull { it.createdAt }
                if (playlist == null) {
                    showMessage(messageStore.getString(R.string.some_error_occurred))
                    return@launch
                }
                val blacklisted = blacklistService.blacklistedSongs.first()
                    .map { it.location }
                    .toSet()
                val validSongs = songLocations.filter { !blacklisted.contains(it) }
                val anyBlacklisted = songLocations.any { blacklisted.contains(it) }
                if (validSongs.isEmpty()) {
                    showMessage(messageStore.getString(R.string.blacklisted_songs_have_not_been_added_to_playlist))
                    return@launch
                }
                playlistService.addSongsToPlaylist(validSongs, playlist.playlistId)
                val msg = messageStore.getString(R.string.done) +
                    if (anyBlacklisted) {
                        ". " + messageStore.getString(R.string.blacklisted_songs_have_not_been_added_to_playlist)
                    } else {
                        ""
                    }
                showMessage(msg)
            } catch (e: Exception) {
                Timber.e(e)
                showMessage(messageStore.getString(R.string.some_error_occurred))
            }
        }
    }

    fun addSongsToFavouritesFromPlayer(songLocations: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                songLocations.forEach { loc ->
                    val song = songExtractor.resolveSong(loc) ?: return@forEach
                    if (!song.favourite) {
                        val updated = song.copy(favourite = true)
                        queueService.update(updated)
                        songService.updateSong(updated)
                    }
                }
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.done))
                }
            } catch (e: Exception) {
                Timber.e(e)
                withContext(Dispatchers.Main) {
                    showMessage(messageStore.getString(R.string.some_error_occurred))
                }
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

    fun removeSongFromQueue(song: Song) {
        val idx = queue.indexOfFirst { it.location == song.location }
        if (idx >= 0) queueService.removeSongAt(idx)
    }

    /** Puts this track immediately after the current one in the queue (no-op if already playing or already next). */
    fun moveQueueSongToPlayNext(song: Song) {
        val q = queueService.queue
        val from = q.indexOfFirst { it.location == song.location }
        if (from < 0) return
        val cur = queueService.currentQueueIndex()
        if (from == cur) return
        val insertAfter = cur + 1
        if (from == insertAfter) return
        val to = if (from < cur) cur else cur + 1
        queueService.moveSong(from, to.coerceAtMost(q.size - 1))
    }

    /** From library lists: insert after current if not in queue, else reorder like [moveQueueSongToPlayNext]. */
    fun playLibrarySongNext(song: Song) {
        if (queueService.insertSongsAfterCurrent(listOf(song))) {
            showMessage(messageStore.getString(R.string.added_to_queue, song.title))
            return
        }
        moveQueueSongToPlayNext(song)
    }

    private val _filesInCurrentDestination = MutableStateFlow(DirectoryContents())
    val filesInCurrentDestination = _filesInCurrentDestination
        .combine(prefs.folderSortOrder){ files, sortOrder ->
            val dirAsc = Comparator<Directory> { a, b -> NaturalOrder.compareNatural(a.name, b.name) }
            val dirDsc = dirAsc.reversed()
            when (sortOrder) {
                SortOptions.NameDSC.ordinal -> DirectoryContents(
                    directories = files.directories.sortedWith(dirDsc),
                    songs = files.songs.sortedByFolderPlaybackOrder(ascending = false),
                )
                else -> DirectoryContents(
                    directories = files.directories.sortedWith(dirAsc),
                    songs = files.songs.sortedByFolderPlaybackOrder(ascending = true),
                )
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

    fun onFileClicked(songIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val minis = filesInCurrentDestination.value.songs
            if (songIndex < 0 || songIndex >= minis.size) return@launch
            val clickedLocation = minis[songIndex].location
            val songs = ArrayList<Song>(minis.size)
            for (mini in minis) {
                songExtractor.resolveSong(mini.location)?.let { songs.add(it) }
            }
            if (songs.isEmpty()) return@launch
            val startIndex = songs.indexOfFirst { it.location == clickedLocation }.takeIf { it >= 0 } ?: 0
            setQueue(songs, startIndex)
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

    fun renameFolder(folder: Directory, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmed = newName.trim().replace(Regex("[/\\\\]"), "")
            if (trimmed.isEmpty()) {
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val f = File(folder.absolutePath)
            if (!f.isDirectory) {
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val parent = f.parentFile ?: run {
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val dest = File(parent, trimmed)
            if (dest.exists()) {
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            if (!f.renameTo(dest)) {
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            explorer.adjustCurrentPathAfterFolderRename(folder.absolutePath, dest.absolutePath)
            explorer.refreshCurrentDirectory()
            _isExplorerAtRoot.update { explorer.isRoot }
            runCatching {
                libraryRepository.updateLibraryFromMediaStore()
            }.onFailure { e ->
                crashReporter.logException(e as? Exception ?: Exception(e))
            }
            showMessage(messageStore.getString(R.string.done))
        }
    }

    fun saveFolderNote(folder: Directory, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val noteFile = File(folder.absolutePath, ".zen_folder_note.txt")
            val result = runCatching {
                if (text.isBlank()) {
                    if (noteFile.exists()) noteFile.delete()
                } else {
                    noteFile.writeText(text.trim())
                }
            }
            result.onFailure { e ->
                crashReporter.logException(e as? Exception ?: Exception(e))
                showMessage(messageStore.getString(R.string.folder_rename_error))
            }
        }
    }
}