package com.generic.audioplayes.data

import android.os.Build
import androidx.datastore.core.Serializer
import com.generic.audioplayes.Screens
import com.generic.audioplayes.components.SortOptions
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class UserPreferencesSerializer @Inject constructor() : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences
        get() = UserPreferences.getDefaultInstance().copy {
            useMaterialYouTheme = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            chosenTheme = UserPreferences.Theme.DARK_MODE
            chosenAccent = UserPreferences.Accent.Elm
            onBoardingComplete = false
            crashlyticsDisabled = false
            playbackParams = UserPreferences.PlaybackParams
                .getDefaultInstance().copy {
                    playbackSpeed = 100
                    playbackPitch = 100
                }
            selectedTabs.apply {
                clear()
                addAll(listOf(0, 1, 2, 3, 4, Screens.Folders.ordinal))
            }
            chosenSortOrder.apply {
                clear()
                put(Screens.Songs.ordinal, SortOptions.TitleASC.ordinal)
                put(Screens.Albums.ordinal, SortOptions.TitleASC.ordinal)
                put(Screens.Artists.ordinal, SortOptions.NameASC.ordinal)
                put(Screens.Genres.ordinal, SortOptions.NameASC.ordinal)
                put(Screens.Playlists.ordinal, SortOptions.NameASC.ordinal)
                put(Screens.Folders.ordinal, SortOptions.Default.ordinal)
            }
            volumeBoosterPercent = 100
            equalizerPreset = UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL
            equalizerCustomBandMb.clear()
            bassBoostStrength = 0
            virtualizerStrength = 0
            equalizerEnabled = true
            equalizerPrefsSeedVersion = 1
            crossfadeEnabled = false
            gaplessPlaybackEnabled = true
            keepScreenOn = false
            showOnLockScreen = true
            pauseOnHeadsetDisconnect = true
            playbackPrefsSeedVersion = 1
            lastBackupExportEpochMs = 0L
        }

    override suspend fun readFrom(input: InputStream): UserPreferences =
        try {
            UserPreferences.parseFrom(input).let { parsed ->
                var next = if (parsed.equalizerPrefsSeedVersion < 1) {
                    parsed.copy {
                        equalizerEnabled = true
                        equalizerPrefsSeedVersion = 1
                    }
                } else {
                    parsed
                }
                // Legacy default was five tabs (0–4) without Folders; add Папки for existing installs.
                if (next.selectedTabsCount == 5 &&
                    next.selectedTabsList.toSet() == setOf(0, 1, 2, 3, 4)
                ) {
                    next = next.copy {
                        selectedTabs.clear()
                        selectedTabs.addAll(listOf(0, 1, 2, 3, 4, Screens.Folders.ordinal))
                    }
                }
                next
            }
        } catch (exception: Exception) {
            defaultValue
        }


    override suspend fun writeTo(t: UserPreferences, output: OutputStream) = t.writeTo(output)
}