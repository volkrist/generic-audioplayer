package com.github.pakka_papad.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import com.github.pakka_papad.R
import com.github.pakka_papad.collection.CollectionType
import com.github.pakka_papad.data.music.Album
import com.github.pakka_papad.data.music.AlbumArtistWithSongCount
import com.github.pakka_papad.data.music.ArtistWithSongCount
import com.github.pakka_papad.data.music.ComposerWithSongCount
import com.github.pakka_papad.data.music.GenreWithSongCount
import com.github.pakka_papad.data.music.LyricistWithSongCount
import com.github.pakka_papad.data.music.MiniSong
import com.github.pakka_papad.data.music.PersonWithSongCount
import com.github.pakka_papad.data.music.Song
import kotlinx.coroutines.launch

@Stable
class HomeNavHelper(
    private val navController: NavController,
    private val lifecycle: Lifecycle,
) {
    private fun check(): Boolean {
        return navController.currentDestination?.id == R.id.homeFragment
    }

    fun navigateToSettings() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_settingsFragment)
    }

    fun navigateToPlaceholder(screenTitle: String) {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToPlaceholderFragment(screenTitle)
        )
    }

    fun navigateToSleepTimer() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_sleepTimerFragment)
    }

    fun navigateToVolumeBooster() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_volumeBoosterFragment)
    }

    fun navigateToEqualizer() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_equalizerFragment)
    }

    fun navigateToDictaphone() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_dictaphoneFragment)
    }

    fun navigateToTheme() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_themeFragment)
    }

    fun navigateToSearch() {
        if (!check()) return
        navController.navigate(R.id.action_homeFragment_to_searchFragment)
    }

    fun navigateToViewDetails(album: Album) {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.AlbumType, album.name)
            )
        )
    }

    fun navigateToViewDetails(personWithSongCount: PersonWithSongCount) {
        if (!check()) return
        when (personWithSongCount) {
            is ArtistWithSongCount -> {
                navController.navigate(
                    HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                        CollectionType(CollectionType.ArtistType, personWithSongCount.name)
                    )
                )
            }
            is AlbumArtistWithSongCount -> {
                navController.navigate(
                    HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                        CollectionType(CollectionType.AlbumArtistType, personWithSongCount.name)
                    )
                )
            }
            is ComposerWithSongCount -> {
                navController.navigate(
                    HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                        CollectionType(CollectionType.ComposerType, personWithSongCount.name)
                    )
                )
            }
            is LyricistWithSongCount -> {
                navController.navigate(
                    HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                        CollectionType(CollectionType.LyricistType, personWithSongCount.name)
                    )
                )
            }
        }
    }

    fun navigateToViewDetails(playlistId: Long) {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToPlaylistEditorFragment(playlistId)
        )
    }

    fun navigateToViewDetails(genreWithSongCount: GenreWithSongCount) {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.GenreType, genreWithSongCount.genreName)
            )
        )
    }

    fun navigateToViewDetails() {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.FavouritesType)
            )
        )
    }

    fun navigateToRecentlyAddedCollection() {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.RecentlyAddedType)
            )
        )
    }

    fun navigateToRecentlyPlayedCollection() {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.RecentlyPlayedType)
            )
        )
    }

    fun navigateToTopTracksCollection() {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.TopTracksType)
            )
        )
    }

    fun navigateToAlbumByName(albumName: String) {
        if (!check()) return
        if (albumName.isBlank() || albumName == "Unknown") return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.AlbumType, albumName)
            )
        )
    }

    fun navigateToArtistByName(artistName: String) {
        if (!check()) return
        if (artistName.isBlank() || artistName == "Unknown") return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToCollectionFragment(
                CollectionType(CollectionType.ArtistType, artistName)
            )
        )
    }

    private fun navigateToChoosePlaylists(locations: List<String>) {
        if (!check()) return
        navController.navigate(
            HomeFragmentDirections.actionHomeFragmentToSelectPlaylistFragment(
                locations.toTypedArray()
            )
        )
    }

    fun navigateToChoosePlaylist(song: Song) {
        if (!check()) return
        navigateToChoosePlaylists(listOf(song.location))
    }

    fun navigateToChoosePlaylist(songs: List<Song>) {
        if (!check()) return
        lifecycle.coroutineScope.launch {
            navigateToChoosePlaylists(songs.map { it.location })
        }
    }

    fun navigateToChoosePlaylist(song: MiniSong) {
        if (!check()) return
        navigateToChoosePlaylists(listOf(song.location))
    }
}
