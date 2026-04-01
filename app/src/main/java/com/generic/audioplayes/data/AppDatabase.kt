package com.generic.audioplayes.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.generic.audioplayes.data.analytics.PlayHistory
import com.generic.audioplayes.data.analytics.PlayHistoryDao
import com.generic.audioplayes.data.daos.AlbumArtistDao
import com.generic.audioplayes.data.daos.AlbumDao
import com.generic.audioplayes.data.daos.ArtistDao
import com.generic.audioplayes.data.daos.BlacklistDao
import com.generic.audioplayes.data.daos.BlacklistedFolderDao
import com.generic.audioplayes.data.daos.ComposerDao
import com.generic.audioplayes.data.daos.GenreDao
import com.generic.audioplayes.data.daos.LyricistDao
import com.generic.audioplayes.data.daos.PlaylistDao
import com.generic.audioplayes.data.daos.SongDao
import com.generic.audioplayes.data.music.Album
import com.generic.audioplayes.data.music.AlbumArtist
import com.generic.audioplayes.data.music.Artist
import com.generic.audioplayes.data.music.BlacklistedFolder
import com.generic.audioplayes.data.music.BlacklistedSong
import com.generic.audioplayes.data.music.Composer
import com.generic.audioplayes.data.music.Genre
import com.generic.audioplayes.data.music.Lyricist
import com.generic.audioplayes.data.music.Playlist
import com.generic.audioplayes.data.music.PlaylistSongCrossRef
import com.generic.audioplayes.data.music.Song
import com.generic.audioplayes.data.thumbnails.Thumbnail
import com.generic.audioplayes.data.thumbnails.ThumbnailDao

@Database(entities = [
        Song::class,
        Album::class,
        Artist::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        Genre::class,
        AlbumArtist::class,
        Composer::class,
        Lyricist::class,
        BlacklistedSong::class,
        BlacklistedFolder::class,
        PlayHistory::class,
        Thumbnail::class,
    ],
    version = 7, exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
    ]
)
abstract class AppDatabase: RoomDatabase() {

    abstract fun songDao(): SongDao

    abstract fun albumDao(): AlbumDao

    abstract fun artistDao(): ArtistDao

    abstract fun albumArtistDao(): AlbumArtistDao

    abstract fun composerDao(): ComposerDao

    abstract fun lyricistDao(): LyricistDao

    abstract fun genreDao(): GenreDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun blacklistDao(): BlacklistDao

    abstract fun blacklistedFolderDao(): BlacklistedFolderDao

    abstract fun playHistoryDao(): PlayHistoryDao

    abstract fun thumbnailDao(): ThumbnailDao
}