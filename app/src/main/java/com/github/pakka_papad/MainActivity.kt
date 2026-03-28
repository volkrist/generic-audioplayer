package com.github.pakka_papad

import android.content.ComponentName
import android.graphics.Color
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.await
import com.github.pakka_papad.data.ZenCrashReporter
import com.github.pakka_papad.data.services.QueueService
import com.github.pakka_papad.databinding.ActivityMainBinding
import com.github.pakka_papad.player.ZenPlayer
import com.github.pakka_papad.widgets.PlayerWidgetManager
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject lateinit var appUpdateManager: AppUpdateManager

    @Inject lateinit var crashReporter: ZenCrashReporter

    @Inject lateinit var queueService: QueueService

    @Inject lateinit var playerWidgetManager: PlayerWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window,false)
        window.statusBarColor = Color.TRANSPARENT
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()

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