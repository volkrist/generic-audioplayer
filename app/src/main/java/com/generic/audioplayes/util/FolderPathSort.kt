package com.generic.audioplayes.util

import com.generic.audioplayes.data.music.MiniSong
import com.generic.audioplayes.data.music.Song
import kotlin.jvm.JvmName

/**
 * Playback / folder order: compare full file paths with [NaturalOrder] so numeric segments
 * in paths and filenames sort as numbers ("…/Chapter 2.mp3" before "…/Chapter 10.mp3").
 * Uses [Song.location] / [MiniSong.location], not embedded title tags (often wrong for audiobooks).
 */
@JvmName("sortedSongsByFolderPlaybackOrder")
fun List<Song>.sortedByFolderPlaybackOrder(ascending: Boolean = true): List<Song> {
    val cmp = Comparator<Song> { a, b -> NaturalOrder.compareNatural(a.location, b.location) }
    return sortedWith(if (ascending) cmp else cmp.reversedCompat())
}

@JvmName("sortedMiniSongsByFolderPlaybackOrder")
fun List<MiniSong>.sortedByFolderPlaybackOrder(ascending: Boolean = true): List<MiniSong> {
    val cmp = Comparator<MiniSong> { a, b -> NaturalOrder.compareNatural(a.location, b.location) }
    return sortedWith(if (ascending) cmp else cmp.reversedCompat())
}
