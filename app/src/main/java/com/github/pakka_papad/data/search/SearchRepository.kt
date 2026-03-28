package com.github.pakka_papad.data.search

import com.github.pakka_papad.data.daos.AlbumDao
import com.github.pakka_papad.data.daos.ArtistDao
import com.github.pakka_papad.data.daos.PlaylistDao
import com.github.pakka_papad.data.daos.SongDao
import com.github.pakka_papad.search.FolderSearchResult
import com.github.pakka_papad.search.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
interface SearchRepository {
    suspend fun search(query: String): SearchResult
}

class SearchRepositoryImpl(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
) : SearchRepository {

    override suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isEmpty()) return@withContext SearchResult()

        coroutineScope {
            val songsDef = async { songDao.searchSongsByTitleExcludingDictaphone(q) }
            val albumsDef = async { albumDao.searchAlbums(q).take(50) }
            val artistsDef = async { artistDao.searchArtists(q).take(50) }
            val playlistsDef = async { playlistDao.searchPlaylists(q).take(50) }
            val dictaphoneDef = async { songDao.searchDictaphoneRecordingsByTitle(q) }
            val locationsDef = async { songDao.searchLocationsContainingForFolders(q) }

            val folders = buildFolderResults(locationsDef.await(), q)

            SearchResult(
                songs = songsDef.await(),
                albums = albumsDef.await(),
                artists = artistsDef.await(),
                folders = folders,
                playlists = playlistsDef.await(),
                dictaphoneRecordings = dictaphoneDef.await(),
            )
        }
    }

    private fun buildFolderResults(locations: List<String>, query: String): List<FolderSearchResult> {
        if (query.isEmpty()) return emptyList()
        val seen = LinkedHashSet<String>()
        val out = ArrayList<FolderSearchResult>()
        for (loc in locations) {
            var dir: File? = File(loc).parentFile ?: continue
            while (dir != null) {
                val d = dir
                if (d.name.contains(query, ignoreCase = true)) {
                    val path = d.absolutePath
                    if (seen.add(path)) {
                        out.add(FolderSearchResult(name = d.name, absolutePath = path))
                    }
                }
                dir = d.parentFile
            }
        }
        return out.take(40)
    }
}
