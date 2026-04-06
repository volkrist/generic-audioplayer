package com.generic.audioplayes.util

import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPathSortTest {

    @Test
    fun naturalOrder_paths_chapter2BeforeChapter10() {
        val paths = listOf(
            "/storage/book/Chapter 10.m4a",
            "/storage/book/Chapter 2.m4a",
            "/storage/book/Chapter 1.m4a",
        )
        val sorted = paths.sortedWith(compareBy(NaturalOrder.stringComparator) { it })
        assertEquals(
            listOf(
                "/storage/book/Chapter 1.m4a",
                "/storage/book/Chapter 2.m4a",
                "/storage/book/Chapter 10.m4a",
            ),
            sorted,
        )
    }

    @Test
    fun naturalOrder_numericBasenames() {
        val paths = listOf(
            "/a/12.mp3",
            "/a/2.mp3",
            "/a/1.mp3",
        )
        val sorted = paths.sortedWith(compareBy(NaturalOrder.stringComparator) { it })
        assertEquals(listOf("/a/1.mp3", "/a/2.mp3", "/a/12.mp3"), sorted)
    }

    @Test
    fun sortedByFolderPlaybackOrder_miniSong_matchesPathOrder() {
        val songs = listOf(
            mini("/x/10.mp3"),
            mini("/x/2.mp3"),
        )
        assertEquals(
            listOf(mini("/x/2.mp3"), mini("/x/10.mp3")),
            songs.sortedByFolderPlaybackOrder(),
        )
    }

    private fun mini(path: String) = MiniSong(
        title = "t",
        location = path,
        artist = "a",
        artUri = "",
    )

    @Test
    fun sortedByFolderPlaybackOrder_song_matchesPathOrder() {
        val songs = listOf(
            song("/root/a/02.mp3"),
            song("/root/a/01.mp3"),
        )
        assertEquals(
            listOf(song("/root/a/01.mp3"), song("/root/a/02.mp3")),
            songs.sortedByFolderPlaybackOrder(),
        )
    }

    private fun song(path: String) = Song(
        location = path,
        title = "",
        size = "",
        addedDate = "",
        modifiedDate = "",
        artist = "",
        albumArtist = "",
        composer = "",
        lyricist = "",
        genre = "",
        year = 0,
        durationMillis = 0L,
        durationFormatted = "",
        bitrate = 0f,
        sampleRate = 0f,
    )
}
