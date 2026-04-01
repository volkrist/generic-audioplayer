package com.generic.audioplayes

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.await
import com.generic.audioplayes.data.ZenCrashReporter
import com.generic.audioplayes.data.ZenPreferenceProvider
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.databinding.ActivityMainBinding
import com.generic.audioplayes.player.ZenPlayer
import com.generic.audioplayes.widgets.PlayerWidgetManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        /** Avoid hammering MediaStore + DB when [onResume] fires in quick succession. */
        private const val MIN_LIBRARY_REFRESH_INTERVAL_MS = 20_000L
        /** -1 = never refreshed this process (avoid skipping first scan when elapsedRealtime is small). */
        @Volatile
        private var lastForegroundLibraryRefreshAtElapsedMs: Long = -1L
    }

    @Inject lateinit var appUpdateManager: AppUpdateManager

    @Inject lateinit var crashReporter: ZenCrashReporter

    @Inject lateinit var queueService: QueueService

    @Inject lateinit var playerWidgetManager: PlayerWidgetManager

    @Inject lateinit var preferenceProvider: ZenPreferenceProvider

    @Inject lateinit var exoPlayer: ExoPlayer

    @Inject lateinit var songExtractor: SongExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.statusBarColor = Color.TRANSPARENT

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    preferenceProvider.keepScreenOn.collectLatest { on ->
                        if (on) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
                }
                launch {
                    preferenceProvider.pauseOnHeadsetDisconnect.collectLatest { handle ->
                        exoPlayer.setHandleAudioBecomingNoisy(handle)
                    }
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()

        val now = SystemClock.elapsedRealtime()
        val due = lastForegroundLibraryRefreshAtElapsedMs < 0L ||
            now - lastForegroundLibraryRefreshAtElapsedMs >= MIN_LIBRARY_REFRESH_INTERVAL_MS
        if (due) {
            lastForegroundLibraryRefreshAtElapsedMs = now
            songExtractor.scanForMusic()
        }

        lifecycleScope.launch {
            try {
                if (!ZenPlayer.isRunning.get()) return@launch
                val song = queueService.currentSong.value ?: return@launch
                val controller = MediaController.Builder(
                    this@MainActivity,
                    SessionToken(this@MainActivity, ComponentName(this@MainActivity, ZenPlayer::class.java)),
                ).buildAsync().await()
                withContext(Dispatchers.Main) {
                    playerWidgetManager.syncWidgetState(song, controller.isPlaying)
                }
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
        }

        try {
            appUpdateManager.appUpdateInfo.addOnSuccessListener {
                if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS){
                    appUpdateManager.startUpdateFlow(
                        it,
                        this,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                    )
                }
            }
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
    }
}