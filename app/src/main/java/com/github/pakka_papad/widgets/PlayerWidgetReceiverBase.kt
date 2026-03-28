package com.github.pakka_papad.widgets

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Shared broadcast handling for all player Glance widgets (same DataStore keys).
 */
abstract class PlayerWidgetReceiverBase : GlanceAppWidgetReceiver() {

    private val scope = CoroutineScope(SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.extras?.getString(WidgetBroadcast.WIDGET_BROADCAST) ?: return
        scope.launch {
            GlanceAppWidgetManager(context)
                .getGlanceIds(glanceAppWidget.javaClass)
                .forEach { glanceId ->
                    when (action) {
                        WidgetBroadcast.SONG_CHANGED -> {
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs[imageUriKey] = intent.getStringExtra("imageUri") ?: ""
                                prefs[albumKey] = intent.getStringExtra("album") ?: ""
                                prefs[titleKey] = intent.getStringExtra("title") ?: ""
                                prefs[artistKey] = intent.getStringExtra("artist") ?: ""
                            }
                        }
                        WidgetBroadcast.IS_PLAYING_CHANGED -> {
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs[isPlayingKey] = intent.getBooleanExtra("isPlaying", false)
                            }
                        }
                    }
                    glanceAppWidget.update(context, glanceId)
                }
        }
    }
}
