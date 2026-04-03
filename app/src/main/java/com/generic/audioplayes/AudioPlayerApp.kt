package com.generic.audioplayes

import android.app.Application
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cat.ereza.customactivityoncrash.config.CaocConfig
import cat.ereza.customactivityoncrash.config.CaocConfig.BACKGROUND_MODE_SILENT
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.generic.audioplayes.BuildConfig
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.services.SleepTimerService
import com.generic.audioplayes.equalizer.EqualizerManager
import com.generic.audioplayes.volume.VolumeBoosterManager
import com.generic.audioplayes.workers.ThumbnailWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AudioPlayerApp: Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory
    @Inject lateinit var sleepTimerService: SleepTimerService

    @Inject lateinit var equalizerManager: EqualizerManager

    @Inject lateinit var volumeBoosterManager: VolumeBoosterManager

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreate() {
        super.onCreate()

        // Use system locale until the user picks an in-app language (LocaleListCompat.empty = follow OS).
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())

        // Theme is loaded from DataStore via AudioPlayerPreferenceProvider.theme (Eagerly); touch ensures early read.
        preferenceProvider.theme.value

        sleepTimerService.restoreIfNeeded()
        with(equalizerManager) { }
        with(volumeBoosterManager) { }

        if (!BuildConfig.DEBUG) {
            FirebaseApp.initializeApp(this)
        }

        if (BuildConfig.DEBUG){
            Timber.plant(Timber.DebugTree())
        }

        CaocConfig.Builder.create().apply {
            restartActivity(MainActivity::class.java)
            errorActivity(CrashActivity::class.java)
            backgroundMode(BACKGROUND_MODE_SILENT)
            apply()
        }

        WorkManager.getInstance(this)
            .enqueue(OneTimeWorkRequestBuilder<ThumbnailWorker>().build())
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).apply {
            allowRgb565(true)
            bitmapConfig(Bitmap.Config.RGB_565)
            error(R.drawable.error)
        }.build()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
    }
}