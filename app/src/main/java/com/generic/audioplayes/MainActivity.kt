package com.generic.audioplayes

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.await
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.data.services.QueueService
import com.generic.audioplayes.databinding.ActivityMainBinding
import com.generic.audioplayes.player.AudioPlayerService
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

        /** One‑shot flag so the cinematic splash only runs on the first cold start per process. */
        @Volatile
        private var splashAlreadyShown: Boolean = false
    }

    @Inject lateinit var appUpdateManager: AppUpdateManager

    @Inject lateinit var crashReporter: AudioPlayerCrashReporter

    @Inject lateinit var queueService: QueueService

    @Inject lateinit var playerWidgetManager: PlayerWidgetManager

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    @Inject lateinit var exoPlayer: ExoPlayer

    @Inject lateinit var songExtractor: SongExtractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.statusBarColor = Color.TRANSPARENT

        setupBrandingSplashOverlay()

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
                if (!AudioPlayerService.isRunning.get()) return@launch
                val song = queueService.currentSong.value ?: return@launch
                val controller = MediaController.Builder(
                    this@MainActivity,
                    SessionToken(this@MainActivity, ComponentName(this@MainActivity, AudioPlayerService::class.java)),
                ).buildAsync().await()
                withContext(Dispatchers.Main) {
                    playerWidgetManager.syncWidgetState(
                        song,
                        controller.isPlaying,
                        preferenceProvider.widgetStyle.value,
                    )
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

    /**
     * Renders [BrandingSplashOverlay] on top of the [binding.root] layout (Android 12+ already
     * shows its short icon splash; our overlay kicks in once [setContentView] is up and fades
     * out automatically). We only show it on the first cold start per process so navigating
     * between app tasks does not stutter the player UI.
     */
    private fun setupBrandingSplashOverlay() {
        if (splashAlreadyShown) {
            binding.brandingSplash.isVisible = false
            return
        }
        splashAlreadyShown = true
        val finishedState = mutableStateOf(false)
        binding.brandingSplash.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                if (!finishedState.value) {
                    BrandingSplashOverlay(
                        onFinished = { finishedState.value = true },
                    )
                }
            }
        }
        lifecycleScope.launch {
            // Poll the finished flag so we can detach the ComposeView once the animation is over
            // (frees the Compose tree so it doesn't keep composing invisible frames forever).
            while (!finishedState.value) {
                kotlinx.coroutines.delay(100)
            }
            binding.brandingSplash.isVisible = false
            binding.brandingSplash.disposeComposition()
        }
    }
}