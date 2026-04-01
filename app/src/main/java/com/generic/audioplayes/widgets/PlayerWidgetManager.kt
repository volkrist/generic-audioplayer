package com.generic.audioplayes.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.generic.audioplayes.data.music.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends explicit broadcasts to all player widget providers so home-screen widgets stay in sync with playback.
 */
@Singleton
class PlayerWidgetManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    private val providers = listOf(
        PlayerWidgetProvider::class.java,
        PlayerWidgetSmallProvider::class.java,
    )

    private fun broadcastToAll(config: Intent.() -> Unit) {
        providers.forEach { clazz ->
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = ComponentName(appContext, clazz)
                config()
            }
            appContext.sendBroadcast(intent)
        }
    }

    fun notifySongChanged(song: Song) {
        broadcastToAll {
            putExtra(WidgetBroadcast.WIDGET_BROADCAST, WidgetBroadcast.SONG_CHANGED)
            putExtra("imageUri", song.artUri)
            putExtra("title", song.title)
            putExtra("artist", song.artist)
            putExtra("album", song.album)
        }
    }

    fun notifyIsPlayingChanged(isPlaying: Boolean) {
        broadcastToAll {
            putExtra(WidgetBroadcast.WIDGET_BROADCAST, WidgetBroadcast.IS_PLAYING_CHANGED)
            putExtra("isPlaying", isPlaying)
        }
    }

    /**
     * Pushes current [song] and [isPlaying] to all widget instances (e.g. when the app resumes).
     */
    fun syncWidgetState(song: Song?, isPlaying: Boolean) {
        val s = song ?: return
        notifySongChanged(s)
        notifyIsPlayingChanged(isPlaying)
    }
}
