package com.generic.audioplayes.collection

import com.generic.audioplayes.data.music.Song

data class CollectionUi(
    val error: String? = null,
    val songs: List<Song> = listOf(),
    val topBarTitle: String = "",
    val topBarBackgroundImageUri: String = "",
)
