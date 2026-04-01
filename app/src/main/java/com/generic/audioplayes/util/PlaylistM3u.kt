package com.generic.audioplayes.util

import com.generic.audioplayes.data.music.Song
import java.io.File

object PlaylistM3u {

    fun parsePaths(content: String): List<String> {
        val out = mutableListOf<String>()
        for (line in content.lineSequence()) {
            val t = line.trim()
            if (t.isEmpty() || t.startsWith("#")) continue
            out.add(t)
        }
        return out
    }

    fun buildM3u(songs: List<Song>): String = buildString {
        appendLine("#EXTM3U")
        for (song in songs) {
            val sec = (song.durationMillis / 1000).coerceAtLeast(0)
            val title = "${song.title} - ${song.artist}"
            appendLine("#EXTINF:$sec,$title")
            appendLine(song.location)
        }
    }

    suspend fun resolveLocation(
        getByLocation: suspend (String) -> Song?,
        rawPath: String,
    ): Song? {
        getByLocation(rawPath)?.let { return it }
        return try {
            val c = File(rawPath).canonicalPath
            getByLocation(c)
        } catch (_: Exception) {
            null
        }
    }
}
