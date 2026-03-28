package com.github.pakka_papad.data

import androidx.datastore.core.DataStore
import com.github.pakka_papad.Screens
import com.github.pakka_papad.components.SortOptions
import com.github.pakka_papad.data.UserPreferences.EqualizerPreset
import com.github.pakka_papad.data.UserPreferences.PlaybackParams
import com.github.pakka_papad.ui.theme.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

data class EqualizerSettings(
    val preset: EqualizerPreset,
    val customBandsMb: List<Int>,
    val bassStrength: Int,
    val virtualizerStrength: Int,
)

class ZenPreferenceProvider @Inject constructor(
    private val userPreferences: DataStore<UserPreferences>,
    private val coroutineScope: CoroutineScope,
    private val crashReporter: ZenCrashReporter,
) {

    val theme = userPreferences.data
        .map {
            ThemePreference(
                useMaterialYou = it.useMaterialYouTheme,
                theme = it.chosenTheme,
                accent = it.chosenAccent,
            )
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference()
        )

    fun updateTheme(newThemePreference: ThemePreference) {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    useMaterialYouTheme = newThemePreference.useMaterialYou
                    chosenTheme = newThemePreference.theme
                    chosenAccent = newThemePreference.accent
                }
            }
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
            EqualizerSettings(
                preset = prefs.equalizerPreset,
                customBandsMb = prefs.equalizerCustomBandMbList.map { it.toInt() },
                bassStrength = prefs.bassBoostStrength.coerceIn(0, 1000),
                virtualizerStrength = prefs.virtualizerStrength.coerceIn(0, 1000),
            )
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = EqualizerSettings(
                preset = EqualizerPreset.EQUALIZER_PRESET_NORMAL,
                customBandsMb = emptyList(),
                bassStrength = 0,
                virtualizerStrength = 0,
            ),
        )

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

    fun resetEqualizerToDefaults() {
        coroutineScope.launch {
            userPreferences.updateData {
                it.copy {
                    equalizerPreset = EqualizerPreset.EQUALIZER_PRESET_NORMAL
                    equalizerCustomBandMb.clear()
                    bassBoostStrength = 0
                    virtualizerStrength = 0
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
            if (it.selectedTabsCount == 0){
                listOf(0,1,2,3,4)
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
        val initJob = coroutineScope.launch {
            launch { theme.collect { } }
            launch { isOnBoardingComplete.collect { } }
            launch { isCrashlyticsDisabled.collect { } }
            launch { playbackParams.collect { updatePlaybackParams(it.playbackSpeed,it.playbackPitch) } }
            launch { selectedTabs.collect{  } }
            launch { songSortOrder.collect {  } }
            launch { albumSortOrder.collect {  } }
            launch { artistSortOrder.collect {  } }
            launch { playlistSortOrder.collect {  } }
            launch { genreSortOrder.collect {  } }
            launch { folderSortOrder.collect {  } }
            launch { sortOrder.collect {  } }
        }
        coroutineScope.launch {
            delay(1.minutes)
            initJob.cancel()
        }
    }
}