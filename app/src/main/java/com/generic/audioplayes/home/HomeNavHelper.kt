package com.generic.audioplayes.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.navigation.NavController
import com.generic.audioplayes.R
import com.generic.audioplayes.collection.CollectionFragmentDirections
import com.generic.audioplayes.collection.CollectionType
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.AlbumArtistWithSongCount
import com.generic.audioplayes.data.music.ArtistWithSongCount
import com.generic.audioplayes.data.music.ComposerWithSongCount
import com.generic.audioplayes.data.music.GenreWithSongCount
import com.generic.audioplayes.data.music.LyricistWithSongCount
import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.PersonWithSongCount
import com.generic.audioplayes.data.music.Song
import kotlinx.coroutines.launch

@Stable
class HomeNavHelper(
    private val navController: NavController,
    private val lifecycle: Lifecycle,
) {
    private fun navigateToCollection(collectionType: CollectionType) {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToCollectionFragment(collectionType),
            )
            R.id.collectionFragment -> navController.navigate(
                CollectionFragmentDirections.actionCollectionFragmentToCollectionFragment(collectionType),
            )
            else -> {}
        }
    }

    fun navigateToSettings() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_settingsFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_settingsFragment)
            else -> {}
        }
    }

    fun navigateToPlaceholder(screenTitle: String) {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToPlaceholderFragment(screenTitle),
            )
            R.id.collectionFragment -> navController.navigate(
                CollectionFragmentDirections.actionCollectionFragmentToPlaceholderFragment(screenTitle),
            )
            else -> {}
        }
    }

    fun navigateToSleepTimer() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_sleepTimerFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_sleepTimerFragment)
            else -> {}
        }
    }

    fun navigateToVolumeBooster() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_volumeBoosterFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_volumeBoosterFragment)
            else -> {}
        }
    }

    fun navigateToEqualizer() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_equalizerFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_equalizerFragment)
            else -> {}
        }
    }

    fun navigateToDictaphone() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_dictaphoneFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_dictaphoneFragment)
            else -> {}
        }
    }

    fun navigateToTheme() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_themeFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_themeFragment)
            else -> {}
        }
    }

    fun navigateToWidgets() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_widgetsFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_widgetsFragment)
            else -> {}
        }
    }

    fun navigateToSearch() {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(R.id.action_homeFragment_to_searchFragment)
            R.id.collectionFragment -> navController.navigate(R.id.action_collectionFragment_to_searchFragment)
            else -> {}
        }
    }

    fun navigateToViewDetails(album: Album) {
        navigateToCollection(CollectionType(CollectionType.AlbumType, album.name))
    }

    fun navigateToViewDetails(personWithSongCount: PersonWithSongCount) {
        when (personWithSongCount) {
            is ArtistWithSongCount -> navigateToCollection(
                CollectionType(CollectionType.ArtistType, personWithSongCount.name),
            )
            is AlbumArtistWithSongCount -> navigateToCollection(
                CollectionType(CollectionType.AlbumArtistType, personWithSongCount.name),
            )
            is ComposerWithSongCount -> navigateToCollection(
                CollectionType(CollectionType.ComposerType, personWithSongCount.name),
            )
            is LyricistWithSongCount -> navigateToCollection(
                CollectionType(CollectionType.LyricistType, personWithSongCount.name),
            )
        }
    }

    fun navigateToViewDetails(playlistId: Long) {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToPlaylistEditorFragment(playlistId),
            )
            R.id.collectionFragment -> navController.navigate(
                CollectionFragmentDirections.actionCollectionFragmentToPlaylistEditorFragment(playlistId),
            )
            else -> {}
        }
    }

    fun navigateToViewDetails(genreWithSongCount: GenreWithSongCount) {
        navigateToCollection(CollectionType(CollectionType.GenreType, genreWithSongCount.genreName))
    }

    fun navigateToViewDetails() {
        navigateToCollection(CollectionType(CollectionType.FavouritesType))
    }

    fun navigateToRecentlyAddedCollection() {
        navigateToCollection(CollectionType(CollectionType.RecentlyAddedType))
    }

    fun navigateToRecentlyPlayedCollection() {
        navigateToCollection(CollectionType(CollectionType.RecentlyPlayedType))
    }

    fun navigateToTopTracksCollection() {
        navigateToCollection(CollectionType(CollectionType.TopTracksType))
    }

    fun navigateToAlbumByName(albumName: String) {
        if (albumName.isBlank() || albumName == "Unknown") return
        navigateToCollection(CollectionType(CollectionType.AlbumType, albumName))
    }

    fun navigateToArtistByName(artistName: String) {
        if (artistName.isBlank() || artistName == "Unknown") return
        navigateToCollection(CollectionType(CollectionType.ArtistType, artistName))
    }

    private fun navigateToChoosePlaylists(locations: List<String>) {
        when (navController.currentDestination?.id) {
            R.id.homeFragment -> navController.navigate(
                HomeFragmentDirections.actionHomeFragmentToSelectPlaylistFragment(
                    locations.toTypedArray(),
                ),
            )
            R.id.collectionFragment -> navController.navigate(
                CollectionFragmentDirections.actionCollectionFragmentToSelectPlaylistFragment(
                    locations.toTypedArray(),
                ),
            )
            else -> {}
        }
    }

    fun navigateToChoosePlaylist(song: Song) {
        navigateToChoosePlaylists(listOf(song.location))
    }

    fun navigateToChoosePlaylist(songs: List<Song>) {
        lifecycle.coroutineScope.launch {
            navigateToChoosePlaylists(songs.map { it.location })
        }
    }

    fun navigateToChoosePlaylist(song: MiniSong) {
        navigateToChoosePlaylists(listOf(song.location))
    }
}
