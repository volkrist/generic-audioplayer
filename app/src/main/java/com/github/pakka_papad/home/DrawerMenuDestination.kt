package com.github.pakka_papad.home

import androidx.annotation.StringRes
import com.github.pakka_papad.R

/**
 * Side navigation drawer entries (order matches product spec).
 */
enum class DrawerMenuDestination(@StringRes val titleRes: Int) {
    Library(R.string.drawer_library),
    Settings(R.string.settings),
    Equalizer(R.string.drawer_equalizer),
    SleepTimer(R.string.drawer_sleep_timer),
    GraphicTheme(R.string.drawer_graphic_theme),
    Widgets(R.string.drawer_widgets),
    VolumeBooster(R.string.drawer_volume_booster),
    Dictaphone(R.string.drawer_dictaphone),
}
