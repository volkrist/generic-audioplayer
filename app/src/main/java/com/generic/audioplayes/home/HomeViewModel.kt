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
import com.generic.audioplayes.util.reversedCompat
import com.generic.audioplayes.util.sortedByFolderPlaybackOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.generic.audioplayes.util.Stage4DebugLog
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

    /**
     * Delete confirmations are delivered exactly once via a [Channel] (not a SharedFlow): the
     * fragment that owns the activity-result launcher may briefly be detached during navigation
     * (Home -> Collection -> back), and a SharedFlow with no live collector silently drops the
     * intent so the system "Allow this app to delete?" dialog never shows. A buffered channel
     * holds emissions until the next collector resumes and then hands them off, fixing the
     * "tapped delete N times before it worked" bug.
     */
    private val deleteConfirmationChannel = Channel<PendingIntent>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val deleteConfirmationSender = deleteConfirmationChannel.receiveAsFlow()

    @Volatile
    private var pendingDeleteSong: Song? = null

    @Volatile
    private var pendingDeleteUri: Uri? = null

    /** When non-null, [onDeleteConfirmedByUser] continues [deleteFolderFromDevice] after a recoverable delete. */
    @Volatile
    private var pendingFolderDelete: Directory? = null

    /**
     * Songs queued for cleanup after a single batched [android.provider.MediaStore.createDeleteRequest]
     * consent (Android 11+). Prevents prompting the user once per file when deleting a folder.
     */
    @Volatile
    private var pendingBatchDeleteSongs: List<Song> = emptyList()

    /** Paths in a folder batch that had no MediaStore URI (handled after batch consent). */
    @Volatile
    private var pendingBatchUnresolvedPaths: List<String> = emptyList()

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
            val restored = try {
                queueStateProvider.restoreQueueIfPossible(songService, queueService)
            } catch (e: Exception) {
                crashReporter.logException(e)
                Timber.e(e, "HomeViewModel: restore queue failed (first attempt)")
                false
            }
            if (restored) {
                syncExoPlayerWithPersistedQueueIfServiceStopped()
            }
            libraryRepository.updateLibraryFromMediaStore()
            // Library DB may have been empty on cold start (e.g. fresh install of an upgrade or
            // user revoked storage permission once); retry the restore once the MediaStore sync
            // finishes so the last-played track is re-attached and never silently disappears.
            if (!restored) {
                try {
                    val secondTry = queueStateProvider.restoreQueueIfPossible(songService, queueService)
                    if (secondTry) {
                        syncExoPlayerWithPersistedQueueIfServiceStopped()
                    }
                } catch (e: Exception) {
                    crashReporter.logException(e)
                    Timber.e(e, "HomeViewModel: restore queue failed (post-sync retry)")
                }
            }
        }
        _currentSongPlaying.update { exoPlayer.isPlaying }
        exoPlayer.addListener(exoPlayerListener)
    }

    /**
     * Called from [HomeFragment] the moment the user grants READ_MEDIA_AUDIO /
     * READ_EXTERNAL_STORAGE. Forces a fresh MediaStore scan so tabs that looked empty during
     * the denied-permission state populate without waiting for the next cold start. Also retries
     * the persisted queue restoration in case the cold-start attempt returned an empty library.
     */
    fun onReadStoragePermissionGranted() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.updateLibraryFromMediaStore()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            try {
                val restored = queueStateProvider.restoreQueueIfPossible(songService, queueService)
                if (restored) syncExoPlayerWithPersistedQueueIfServiceStopped()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
            try {
                explorer.refreshCurrentDirectory()
            } catch (_: Exception) {
            }
        }
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
                        deleteConfirmationChannel.trySend(result.pendingIntent)
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
        // Batched folder delete (Android 11+): the system removed the whole batch atomically,
        // we just clean up DB + queue here.
        val batch = pendingBatchDeleteSongs
        if (batch.isNotEmpty()) {
            val folder = pendingFolderDelete
            val unresolvedPaths = pendingBatchUnresolvedPaths
            pendingBatchDeleteSongs = emptyList()
            pendingBatchUnresolvedPaths = emptyList()
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    var removed = 0
                    var failed = 0
                    for (s in batch) {
                        if (File(s.location).exists()) {
                            failed++
                            continue
                        }
                        try {
                            songService.removeSongFromLibraryMetadata(s)
                            removed++
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                            failed++
                        }
                        withContext(Dispatchers.Main) {
                            refreshQueueAfterRemovingSong(s)
                        }
                    }
                    for (path in unresolvedPaths) {
                        when (
                            AudioFileActions.deleteAudioFileFromDeviceWithFallback(
                                appContext,
                                path,
                            )
                        ) {
                            MediaDeleteResult.Success -> {
                                songService.getSongByLocation(path)?.let { song ->
                                    songService.removeSongFromLibraryMetadata(song)
                                    withContext(Dispatchers.Main) {
                                        refreshQueueAfterRemovingSong(song)
                                    }
                                }
                                removed++
                            }
                            is MediaDeleteResult.Recoverable -> {
                                failed++
                                Timber.w("folder batch: unresolved still recoverable %s", path)
                            }
                            MediaDeleteResult.Failed -> {
                                failed++
                                Timber.w("folder batch: unresolved delete failed %s", path)
                            }
                        }
                    }
                    libraryRepository.updateLibraryFromMediaStore()
                    if (folder != null) {
                        val dir = File(folder.absolutePath)
                        if (dir.exists()) {
                            if (!dir.deleteRecursively()) {
                                deleteDirectoryRecursiveBestEffort(dir)
                            }
                        }
                    }
                    pendingFolderDelete = null
                    refreshExplorerAfterFolderDelete(folder)
                    withContext(Dispatchers.Main) {
                        val msg = when {
                            folder == null -> messageStore.getString(R.string.player_delete_ok)
                            failed > 0 -> messageStore.getString(
                                R.string.folder_delete_partial,
                                removed,
                                batch.size + unresolvedPaths.size,
                                failed,
                            )
                            else -> messageStore.getString(R.string.folder_delete_ok)
                        }
                        showMessage(msg)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "onDeleteConfirmedByUser batch")
                    pendingFolderDelete = null
                    withContext(Dispatchers.Main) {
                        showMessage(messageStore.getString(R.string.some_error_occurred))
                    }
                }
            }
            return
        }
        val song = pendingDeleteSong ?: return
        val uri = pendingDeleteUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // On Android 11+ the system has already removed the file once the user granted
                // the createDeleteRequest consent — we just clean up our DB / queue here. On older
                // APIs we still call deleteAudioFileByUri so the legacy RecoverableSecurityException
                // path can finish.
                val result = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    AudioFileActions.confirmDeleteAudioFileByUri(appContext, uri)
                } else {
                    AudioFileActions.deleteAudioFileByUri(appContext, uri)
                }
                when (result) {
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
                        deleteConfirmationChannel.trySend(result.pendingIntent)
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
        pendingBatchDeleteSongs = emptyList()
        pendingBatchUnresolvedPaths = emptyList()
    }

    private suspend fun completeDeleteAfterFileRemoved(song: Song) {
        songService.removeSongFromLibraryMetadata(song)
        withContext(Dispatchers.Main) {
            refreshQueueAfterRemovingSong(song)
        }
        libraryRepository.updateLibraryFromMediaStore()
        // Kick the file explorer so the deleted track also disappears from the current
        // folder listing live (it reads directory contents on demand, not through a flow).
        runCatching { explorer.refreshCurrentDirectory() }
        withContext(Dispatchers.Main) {
            showMessage(messageStore.getString(R.string.player_delete_ok))
        }
    }

    /**
     * Forces the Folders tab to re-query its current directory after a delete. When the deleted
     * folder happens to be the one the user was browsing (or an ancestor of it), we step the
     * explorer up to the nearest still-existing ancestor so the UI doesn't freeze on a path
     * that no longer exists.
     */
    private fun refreshExplorerAfterFolderDelete(deleted: Directory?) {
        try {
            if (deleted != null) {
                val deletedPath = File(deleted.absolutePath).canonicalFile.absolutePath.trimEnd(File.separatorChar)
                var walker = File(deletedPath)
                while (!walker.exists() || !walker.isDirectory) {
                    val parent = walker.parentFile ?: break
                    walker = parent
                }
                // Always bounce into the parent listing: the deleted folder is obviously gone,
                // its parent is the screen the user should see after the delete.
                val parentOfDeleted = File(deletedPath).parentFile
                if (parentOfDeleted != null && parentOfDeleted.exists() && parentOfDeleted.isDirectory) {
                    explorer.moveInsideDirectory(parentOfDeleted.absolutePath)
                } else {
                    explorer.refreshCurrentDirectory()
                }
            } else {
                explorer.refreshCurrentDirectory()
            }
            _isExplorerAtRoot.update { explorer.isRoot }
        } catch (e: Exception) {
            Timber.e(e, "refreshExplorerAfterFolderDelete")
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
            if (!merged.containsKey(song.location)) {
                merged[song.location] = song
            }
        }
        discoverSongsOnDiskUnderFolder(primary).forEach { song ->
            if (!merged.containsKey(song.location)) {
                merged[song.location] = song
            }
        }
        Stage4DebugLog.i(
            "resolveFolderSongsForDeletion path=$primary merged=${merged.size}",
        )
        return merged.values.toList()
    }

    private suspend fun discoverSongsOnDiskUnderFolder(folderPath: String): List<Song> {
        val dir = File(folderPath.trimEnd('/'))
        if (!dir.isDirectory) return emptyList()
        val audioExt = setOf("mp3", "m4a", "mp4", "flac", "ogg", "wav", "aac", "opus", "wma", "m4b")
        val paths = try {
            dir.walkTopDown()
                .maxDepth(12)
                .filter { it.isFile && it.extension.lowercase() in audioExt }
                .map { it.absolutePath }
                .toList()
        } catch (e: Exception) {
            Timber.e(e, "discoverSongsOnDiskUnderFolder %s", folderPath)
            return emptyList()
        }
        val songs = LinkedHashMap<String, Song>()
        for (path in paths) {
            val song = songService.getSongByLocation(path) ?: songExtractor.resolveSong(path) ?: continue
            songs[song.location] = song
        }
        return songs.values.toList()
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
                val batchPreview = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    AudioFileActions.createBatchDeleteRequestOrNull(
                        appContext,
                        songs.map { it.location },
                    )
                } else {
                    null
                }
                Stage4DebugLog.i(
                    "folderPath=${folder.absolutePath} filesInFolder count=${songs.size} " +
                        "resolvedUris count=${batchPreview?.resolvedUris?.size ?: 0} " +
                        "unresolvedPaths count=${batchPreview?.unresolvedPaths?.size ?: 0}",
                )
                if (songs.isEmpty()) {
                    runCatching {
                        dir.walkBottomUp().forEach { entry ->
                            if (entry.isFile) {
                                if (!entry.delete()) {
                                    Timber.w("folder delete: could not delete file %s", entry.absolutePath)
                                }
                            }
                        }
                    }.onFailure { Timber.e(it, "folder delete walk") }
                    val ok = deleteDirectoryRecursiveBestEffort(dir)
                    if (ok) {
                        libraryRepository.updateLibraryFromMediaStore()
                        refreshExplorerAfterFolderDelete(folder)
                    }
                    Stage4DebugLog.i(
                        "delete result=${if (ok) "empty_folder_ok" else "empty_folder_fail"} " +
                            "room deleted count=n/a ui refresh triggered=$ok",
                    )
                    withContext(Dispatchers.Main) {
                        pendingFolderDelete = null
                        showMessage(
                            messageStore.getString(
                                if (ok) R.string.folder_delete_ok else R.string.folder_delete_failed
                            )
                        )
                    }
                    return@launch
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    val batch = batchPreview
                    if (batch != null) {
                        Stage4DebugLog.i(
                            "delete result=pending_system_dialog resolvedUris=${batch.resolvedUris.size} " +
                                "unresolvedPaths=${batch.unresolvedPaths.size}",
                        )
                        pendingBatchDeleteSongs = songs
                        pendingBatchUnresolvedPaths = batch.unresolvedPaths
                        pendingFolderDelete = folder
                        deleteConfirmationChannel.trySend(batch.pendingIntent)
                        return@launch
                    }
                    Timber.w("deleteFolderFromDevice: batch request unavailable, falling back per-file")
                }
                var deleted = 0
                var failed = 0
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
                            deleteConfirmationChannel.trySend(result.pendingIntent)
                            return@launch
                        }
                        MediaDeleteResult.Failed -> {
                            failed++
                            Timber.w("deleteFolderFromDevice failed %s", song.location)
                        }
                    }
                }
                libraryRepository.updateLibraryFromMediaStore()
                if (deleted > 0) {
                    try {
                        dir.deleteRecursively()
                    } catch (_: Exception) {
                        deleteDirectoryRecursiveBestEffort(dir)
                    }
                    refreshExplorerAfterFolderDelete(folder)
                }
                Stage4DebugLog.i(
                    "delete result=per_file deleted=$deleted failed=$failed total=${songs.size} " +
                        "room deleted count=$deleted ui refresh triggered=${deleted > 0}",
                )
                withContext(Dispatchers.Main) {
                    pendingFolderDelete = null
                    when {
                        deleted == songs.size -> showMessage(
                            messageStore.getString(R.string.folder_delete_ok),
                        )
                        deleted > 0 -> showMessage(
                            messageStore.getString(
                                R.string.folder_delete_partial,
                                deleted,
                                songs.size,
                                failed,
                            ),
                        )
                        else -> showMessage(messageStore.getString(R.string.folder_delete_failed))
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
            val dirDsc = dirAsc.reversedCompat()
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
                Stage4DebugLog.w("renameFolder not a directory path=${folder.absolutePath}")
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val parent = f.parentFile ?: run {
                Stage4DebugLog.w("renameFolder no parent path=${folder.absolutePath}")
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val dest = File(parent, trimmed)
            if (dest.exists()) {
                Stage4DebugLog.w("renameFolder dest exists path=${dest.absolutePath}")
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            val renamed = runCatching {
                java.nio.file.Files.move(
                    f.toPath(),
                    dest.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                )
                true
            }.getOrElse {
                Timber.w(it, "renameFolder Files.move failed, trying renameTo")
                f.renameTo(dest)
            }
            if (!renamed) {
                Stage4DebugLog.w(
                    "renameFolder failed from=${f.absolutePath} to=${dest.absolutePath} canWrite=${f.canWrite()}",
                )
                showMessage(messageStore.getString(R.string.folder_rename_error))
                return@launch
            }
            Stage4DebugLog.i("renameFolder ok to=${dest.absolutePath}")
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
            val dir = File(folder.absolutePath)
            val noteFile = File(folder.absolutePath, ".zen_folder_note.txt")
            Stage4DebugLog.i(
                "saveFolderNote path=${folder.absolutePath} dirWritable=${dir.canWrite()} textLen=${text.length}",
            )
            val result = runCatching {
                if (text.isBlank()) {
                    if (noteFile.exists()) noteFile.delete()
                } else {
                    if (!dir.exists() && !dir.mkdirs()) {
                        error("folder missing: ${dir.absolutePath}")
                    }
                    noteFile.writeText(text.trim())
                }
            }
            result.onSuccess {
                Stage4DebugLog.i("saveFolderNote ok path=${folder.absolutePath}")
                showMessage(messageStore.getString(R.string.done))
            }.onFailure { e ->
                Stage4DebugLog.e("saveFolderNote failed path=${folder.absolutePath}", e)
                crashReporter.logException(e as? Exception ?: Exception(e))
                showMessage(messageStore.getString(R.string.folder_rename_error))
            }
        }
    }
}