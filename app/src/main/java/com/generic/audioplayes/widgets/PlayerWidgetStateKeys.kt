package com.generic.audioplayes.widgets

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val imageUriKey = stringPreferencesKey("image_uri")
val albumKey = stringPreferencesKey("album")
val titleKey = stringPreferencesKey("title")
val artistKey = stringPreferencesKey("artist")
val isPlayingKey = booleanPreferencesKey("is_playing")
