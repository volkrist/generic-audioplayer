package com.generic.audioplayes.data

import androidx.datastore.core.DataStore
import com.generic.audioplayes.Screens
import com.generic.audioplayes.components.SortOptions
import com.generic.audioplayes.data.UserPreferences.EqualizerPreset
import com.generic.audioplayes.data.UserPreferences.PlaybackParams
import com.generic.audioplayes.equalizer.EqualizerPresetHelper
import com.generic.audioplayes.ui.theme.ThemePreference
import com.generic.audioplayes.widgets.WidgetStyle
import com.generic.audioplayes.widgets.toProto
import com.generic.audioplayes.widgets.toUiWidgetStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

data class EqualizerSettings(
    val enabled: Boolean,
    val preset: EqualizerPreset,
    val customBandsMb: List<Int>,
    val bassStrength: Int,
    val virtualizerStrength: Int,
    /** UI band count: 5 or 10 */
    val uiBandCount: Int,
    /** [android.media.audiofx.PresetReverb] preset constant */
    val reverbPreset: Int,
)

class AudioPlayerPreferenceProvider @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
    private val coroutineScope: CoroutineScope,
    private val crashReporter: AudioPlayerCrashReporter,
) {

    val theme = userPreferences.data
        .map {
            ThemePreference(
                useMaterialYou = it.useMaterialYouTheme,
                theme = it.chosenTheme,
                accent = it.chosenAccent,
                graphicWallpaperPreset = it.graphicWallpaperPreset,
                graphicWallpaperCustomUri = it.graphicWallpaperCustomUri,
                graphicColorSlot = it.graphicThemeColorSlot,
            )
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference(
                theme = UserPreferences.Theme.DARK_MODE,
                accent = UserPreferences.Accent.Elm,
            ),
        )

    fun updateTheme(newThemePreference: ThemePreference) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    useMaterialYouTheme = newThemePreference.useMaterialYou
                    chosenTheme = newThemePreference.theme
                    chosenAccent = newThemePreference.accent
                    graphicWallpaperPreset = newThemePreference.graphicWallpaperPreset
                    graphicWallpaperCustomUri = newThemePreference.graphicWallpaperCustomUri
                    graphicThemeColorSlot = newThemePreference.graphicColorSlot
                }
            }
        }
    }

    val lastBackupExportEpochMs = userPreferences.data
        .map { it.lastBackupExportEpochMs }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = 0L,
        )

    suspend fun setLastBackupExportEpochMs(epochMs: Long) {
        userPreferences.updateData {
            it.copy { lastBackupExportEpochMs = epochMs }
        }
    }

    val isOnBoardingComplete = userPreferences.data
        .map {
            it.onBoardingComplete
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    /** One-shot read for splash navigation — avoids waiting on StateFlow null. */
    suspend fun readOnBoardingComplete(): Boolean =
        userPreferences.data.first().onBoardingComplete

    fun setOnBoardingComplete() {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    onBoardingComplete = true
                }
            }
        }
    }

    val isCrashlyticsDisabled = userPreferences.data
        .map {
            it.crashlyticsDisabled
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun toggleCrashlytics(autoReportCrash: Boolean) {
        coroutineScope.launch {
            crashReporter.sendCrashData(autoReportCrash)
            userPreferences.updateData {
                it.copy {
                    crashlyticsDisabled = !autoReportCrash
                }
            }
        }
    }

    val playbackParams = userPreferences.data
        .map {
            it.playbackParams
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlaybackParams
                .getDefaultInstance().copy {
                    playbackSpeed = 100
                    playbackPitch = 100
                }
        )

    val volumeBoosterPercent = userPreferences.data
        .map { prefs ->
            val p = prefs.volumeBoosterPercent
            if (p in 100..200) p else 100
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = 100,
        )

    fun updateVolumeBoosterPercent(percent: Int) {
        val clamped = percent.coerceIn(100, 200)
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    volumeBoosterPercent = clamped
                }
            }
        }
    }

    val equalizerSettings: StateFlow<EqualizerSettings> = userPreferences.data
        .map { prefs ->
            val uiBands = prefs.equalizerUiBandCount.let { n -> if (n == 5 || n == 10) n else 5 }
            EqualizerSettings(
                enabled = prefs.equalizerEnabled,
                preset = prefs.equalizerPreset,
                customBandsMb = prefs.equalizerCustomBandMbList.map { it.toInt() },
                bassStrength = prefs.bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = prefs.virtualizerStrength.coerceIn(0, 1000),
                uiBandCount = uiBands,
                reverbPreset = prefs.reverbPreset.coerceIn(0, 6),
            )
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = EqualizerSettings(
                enabled = true,
                preset = EqualizerPreset.EQUALIZER_PRESET_NORMAL,
                customBandsMb = emptyList(),
                bassStrength = 0,
                virtualizerStrength = 0,
                uiBandCount = 5,
                reverbPreset = 0,
            ),
        )

    fun updateEqualizerEnabled(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy { equalizerEnabled = enabled }
            }
        }
    }

    fun updateEqualizerPreset(preset: EqualizerPreset) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    equalizerPreset = preset
                    if (preset != EqualizerPreset.EQUALIZER_PRESET_CUSTOM) {
                        equalizerCustomBandMb.clear()
                    }
                }
            }
        }
    }

    fun updateEqualizerCustomBands(bandsMb: List<Int>) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    equalizerPreset = EqualizerPreset.EQUALIZER_PRESET_CUSTOM
                    equalizerCustomBandMb.clear()
                    equalizerCustomBandMb.addAll(bandsMb)
                }
            }
        }
    }

    fun updateBassBoostStrength(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy { bassBoostStrength = s }
            }
        }
    }

    fun updateVirtualizerStrength(strength: Int) {
        val s = strength.coerceIn(0, 1000)
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy { virtualizerStrength = s }
            }
        }
    }

    fun updateReverbPreset(presetId: Int) {
        val p = presetId.coerceIn(0, 6)
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy { reverbPreset = p }
            }
        }
    }

    fun updateEqualizerUiBandCount(newCount: Int) {
        val c = if (newCount == 10) 10 else 5
        coroutineScope.launch {
            userPreferences.updateData { p ->
                val old = p.equalizerUiBandCount.let { n -> if (n == 5 || n == 10) n else 5 }
                if (old == c) return@updateData p
                val settings = EqualizerSettings(
                    enabled = p.equalizerEnabled,
                    preset = p.equalizerPreset,
                    customBandsMb = p.equalizerCustomBandMbList.map { it.toInt() },
                    bassStrength = p.bassBoostStrength,
                    virtualizerStrength = p.virtualizerStrength,
                    uiBandCount = old,
                    reverbPreset = p.reverbPreset,
                )
                p.copy {
                    equalizerUiBandCount = c
                    if (settings.preset == EqualizerPreset.EQUALIZER_PRESET_CUSTOM &&
                        settings.customBandsMb.isNotEmpty()
                    ) {
                        equalizerCustomBandMb.clear()
                        equalizerCustomBandMb.addAll(
                            EqualizerPresetHelper.resizeBands(settings.customBandsMb, c).toList(),
                        )
                    }
                }
            }
        }
    }

    val crossfadeEnabled: StateFlow<Boolean> = userPreferences.data
        .map { it.crossfadeEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val gaplessPlaybackEnabled: StateFlow<Boolean> = userPreferences.data
        .map { it.gaplessPlaybackEnabled }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    val keepScreenOn: StateFlow<Boolean> = userPreferences.data
        .map { it.keepScreenOn }
        .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    val showOnLockScreen: StateFlow<Boolean> = userPreferences.data
        .map { it.showOnLockScreen }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    /** In-app mini player, media notification palette, and home widget visual style. */
    val widgetStyle: StateFlow<WidgetStyle> = userPreferences.data
        .map { it.chosenWidgetStyle.toUiWidgetStyle() }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = WidgetStyle.CLASSIC,
        )

    fun updateWidgetStyle(style: WidgetStyle) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    chosenWidgetStyle = style.toProto()
                }
            }
        }
    }

    val pauseOnHeadsetDisconnect: StateFlow<Boolean> = userPreferences.data
        .map { it.pauseOnHeadsetDisconnect }
        .stateIn(coroutineScope, SharingStarted.Eagerly, true)

    fun updateCrossfadeEnabled(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData { it.copy { crossfadeEnabled = enabled } }
        }
    }

    fun updateGaplessPlaybackEnabled(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData { it.copy { gaplessPlaybackEnabled = enabled } }
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData { it.copy { keepScreenOn = enabled } }
        }
    }

    fun updateShowOnLockScreen(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData { it.copy { showOnLockScreen = enabled } }
        }
    }

    fun updatePauseOnHeadsetDisconnect(enabled: Boolean) {
        coroutineScope.launch {
            userPreferences.updateData { it.copy { pauseOnHeadsetDisconnect = enabled } }
        }
    }

    private suspend fun seedPlaybackToggleDefaultsIfNeeded() {
        userPreferences.updateData { p ->
            if (p.playbackPrefsSeedVersion >= 1) return@updateData p
            p.copy {
                crossfadeEnabled = false
                gaplessPlaybackEnabled = true
                keepScreenOn = false
                showOnLockScreen = true
                pauseOnHeadsetDisconnect = true
                playbackPrefsSeedVersion = 1
            }
        }
    }

    fun resetEqualizerToDefaults() {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    equalizerEnabled = true
                    equalizerPreset = EqualizerPreset.EQUALIZER_PRESET_NORMAL
                    equalizerCustomBandMb.clear()
                    bassBoostStrength = 0
                    virtualizerStrength = 0
                    equalizerUiBandCount = 5
                    reverbPreset = 0
                }
            }
        }
    }

    fun updatePlaybackParams(speed: Int, pitch: Int){
        coroutineScope.launch {
            val correctedParams = PlaybackParams.getDefaultInstance().copy{
                playbackSpeed = if (speed < 1 || speed > 200) 100 else speed
                playbackPitch = if (pitch < 1 || pitch > 200) 100 else pitch
            }
            userPreferences.updateData {
                it.copy {
                    playbackParams = correctedParams
                }
            }
        }
    }

    val selectedTabs = userPreferences.data
        .map {
            if (it.selectedTabsCount == 0) {
                listOf(0, 1, 2, 3, 4, Screens.Folders.ordinal)
            } else {
                it.selectedTabsList.toList()
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun updateSelectedTabs(tabsList: List<Int>){
        if (tabsList.isEmpty()) return
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    selectedTabs.apply {
                        clear()
                        addAll(tabsList)
                    }
                }
            }
        }
    }

    val songSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Songs.ordinal, SortOptions.TitleASC.ordinal)
        }

    val albumSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Albums.ordinal, SortOptions.TitleASC.ordinal)
        }

    val artistSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Artists.ordinal, SortOptions.NameASC.ordinal)
        }

    val playlistSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Playlists.ordinal, SortOptions.NameASC.ordinal)
        }

    val genreSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Genres.ordinal, SortOptions.NameASC.ordinal)
        }

    val folderSortOrder = userPreferences.data
        .map {
            it.getChosenSortOrderOrDefault(Screens.Folders.ordinal, SortOptions.Default.ordinal)
        }

    val sortOrder = userPreferences.data
        .map {
            it.chosenSortOrderMap
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = mapOf(),
        )

    fun updateSortOrder(screen: Int, order: Int) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    chosenSortOrder[screen] = order
                }
            }
        }
    }

    init {
        coroutineScope.launch {
            try {
                seedPlaybackToggleDefaultsIfNeeded()
            } catch (e: Exception) {
                crashReporter.logException(e)
            }
        }
        val initJob = coroutineScope.launch {
            launch { theme.collect { } }
            launch { isOnBoardingComplete.collect { } }
            launch { isCrashlyticsDisabled.collect { } }
            launch { playbackParams.collect { } }
            launch { selectedTabs.collect{  } }
            launch { songSortOrder.collect {  } }
            launch { albumSortOrder.collect {  } }
            launch { artistSortOrder.collect {  } }
            launch { playlistSortOrder.collect {  } }
            launch { genreSortOrder.collect {  } }
            launch { folderSortOrder.collect {  } }
            launch { sortOrder.collect {  } }
            launch { widgetStyle.collect { } }
        }
        coroutineScope.launch {
            delay(1.minutes)
            initJob.cancel()
        }
    }
}