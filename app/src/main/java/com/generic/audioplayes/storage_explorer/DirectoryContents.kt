package com.generic.audioplayes.storage_explorer

import com.generic.audioplayes.data.music.MiniSong

data class DirectoryContents(
    val directories: List<Directory> = listOf(),
    val songs: List<MiniSong> = listOf()
)
