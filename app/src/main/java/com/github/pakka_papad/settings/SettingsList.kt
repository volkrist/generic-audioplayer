package com.github.pakka_papad.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.pakka_papad.BuildConfig
import com.github.pakka_papad.R
import com.github.pakka_papad.Screens
import com.github.pakka_papad.data.UserPreferences
import com.github.pakka_papad.data.UserPreferences.Accent
import com.github.pakka_papad.data.music.ScanStatus
import com.github.pakka_papad.nowplaying.DraggableItem
import com.github.pakka_papad.nowplaying.dragContainer
import com.github.pakka_papad.nowplaying.rememberDragDropState
import com.github.pakka_papad.ui.theme.ThemePreference
import com.github.pakka_papad.ui.theme.getSeedColor
import timber.log.Timber

@Composable
fun SettingsList(
    paddingValues: PaddingValues,
    isAppUpdateAvailable: Boolean,
    onAppUpdateClicked: () -> Unit,
    themePreference: ThemePreference,
    onThemePreferenceChanged: (ThemePreference) -> Unit,
    scanStatus: ScanStatus,
    onScanClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    disabledCrashlytics: Boolean,
    onAutoReportCrashClicked: (Boolean) -> Unit,
    onWhatsNewClicked: () -> Unit,
    onRestoreFoldersClicked: () -> Unit,
    tabsSelection: List<Pair<Screens,Boolean>>,
    onTabsSelectChange: (Screens, Boolean) -> Unit,
    onTabsOrderChanged: (fromIdx: Int, toIdx: Int) -> Unit,
    onTabsOrderConfirmed: () -> Unit,
    crossfadeEnabled: Boolean,
    onCrossfadeChanged: (Boolean) -> Unit,
    gaplessEnabled: Boolean,
    onGaplessChanged: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    showOnLockScreen: Boolean,
    onShowOnLockScreenChanged: (Boolean) -> Unit,
    pauseOnHeadsetDisconnect: Boolean,
    onPauseOnHeadsetChanged: (Boolean) -> Unit,
    onEqualizerClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    onGraphicThemeClicked: () -> Unit,
    onHiddenMusicClicked: () -> Unit,
    onBackupRestoreClicked: () -> Unit,
    onFaqClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onRateUsClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    onLanguageClicked: () -> Unit,
    onPremiumClicked: () -> Unit,
    appVersionDisplay: String,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isAppUpdateAvailable) {
            item {
                UpdateAvailable(onClick = onAppUpdateClicked)
            }
        }
        item {
            GroupTitle(title = stringResource(R.string.settings_section_features))
        }
        item {
            FeaturesToolsSection(
                onEqualizerClicked = onEqualizerClicked,
                onSleepTimerClicked = onSleepTimerClicked,
                onGraphicThemeClicked = onGraphicThemeClicked,
            )
        }
        item {
            GroupTitle(title = stringResource(R.string.music_library))
        }
        item {
            MusicLibrarySettings(
                scanStatus = scanStatus,
                onScanClicked = onScanClicked,
                onRestoreClicked = onRestoreClicked,
                onRestoreFoldersClicked = onRestoreFoldersClicked,
                onBackupRestoreClicked = onBackupRestoreClicked,
                onHiddenMusicClicked = onHiddenMusicClicked,
            )
        }
        item {
            GroupTitle(title = stringResource(R.string.settings_section_playback))
        }
        item {
            PlaybackTogglesSection(
                crossfadeEnabled = crossfadeEnabled,
                onCrossfadeChanged = onCrossfadeChanged,
                gaplessEnabled = gaplessEnabled,
                onGaplessChanged = onGaplessChanged,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChanged = onKeepScreenOnChanged,
                showOnLockScreen = showOnLockScreen,
                onShowOnLockScreenChanged = onShowOnLockScreenChanged,
                pauseOnHeadsetDisconnect = pauseOnHeadsetDisconnect,
                onPauseOnHeadsetChanged = onPauseOnHeadsetChanged,
            )
        }
        item {
            GroupTitle(title = stringResource(R.string.look_and_feel))
        }
        item {
            LookAndFeelSettings(
                themePreference = themePreference,
                onPreferenceChanged = onThemePreferenceChanged,
                tabsSelection = tabsSelection,
                onTabsSelectChange = onTabsSelectChange,
                onTabsOrderChanged = onTabsOrderChanged,
                onTabsOrderConfirmed = onTabsOrderConfirmed,
            )
        }
        item {
            GroupTitle(title = stringResource(R.string.settings_section_help))
        }
        item {
            HelpAboutSection(
                onFaqClicked = onFaqClicked,
                onFeedbackClicked = onFeedbackClicked,
                onRateUsClicked = onRateUsClicked,
                onPrivacyPolicyClicked = onPrivacyPolicyClicked,
                onTermsClicked = onTermsClicked,
                onLanguageClicked = onLanguageClicked,
                appVersionDisplay = appVersionDisplay,
            )
        }
        item {
            GroupTitle(title = stringResource(R.string.report_bug))
        }
        item {
            ReportBug(
                disabledCrashlytics = disabledCrashlytics,
                onAutoReportCrashClicked = onAutoReportCrashClicked,
            )
        }
        item {
            PremiumAndAdsSection(onPremiumClicked = onPremiumClicked)
        }
        item {
            MadeBy(
                onWhatsNewClicked = onWhatsNewClicked
            )
        }
    }
}

@Composable
private fun PlaybackTogglesSection(
    crossfadeEnabled: Boolean,
    onCrossfadeChanged: (Boolean) -> Unit,
    gaplessEnabled: Boolean,
    onGaplessChanged: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    showOnLockScreen: Boolean,
    onShowOnLockScreenChanged: (Boolean) -> Unit,
    pauseOnHeadsetDisconnect: Boolean,
    onPauseOnHeadsetChanged: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.group()) {
        Setting(
            title = stringResource(R.string.settings_crossfade),
            icon = R.drawable.baseline_colorize_40,
            description = stringResource(R.string.settings_crossfade_desc),
            isChecked = crossfadeEnabled,
            onCheckedChanged = onCrossfadeChanged,
        )
        Setting(
            title = stringResource(R.string.settings_gapless),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.settings_gapless_desc),
            isChecked = gaplessEnabled,
            onCheckedChanged = onGaplessChanged,
        )
        Setting(
            title = stringResource(R.string.settings_keep_screen_on),
            icon = R.drawable.baseline_light_mode_40,
            description = stringResource(R.string.settings_keep_screen_on_desc),
            isChecked = keepScreenOn,
            onCheckedChanged = onKeepScreenOnChanged,
        )
        Setting(
            title = stringResource(R.string.settings_show_lock_screen),
            icon = R.drawable.baseline_dark_mode_40,
            description = stringResource(R.string.settings_show_lock_screen_desc),
            isChecked = showOnLockScreen,
            onCheckedChanged = onShowOnLockScreenChanged,
        )
        Setting(
            title = stringResource(R.string.settings_pause_headset),
            icon = R.drawable.baseline_send_40,
            description = stringResource(R.string.settings_pause_headset_desc),
            isChecked = pauseOnHeadsetDisconnect,
            onCheckedChanged = onPauseOnHeadsetChanged,
        )
    }
}

@Composable
private fun FeaturesToolsSection(
    onEqualizerClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    onGraphicThemeClicked: () -> Unit,
) {
    Column(modifier = Modifier.group()) {
        Setting(
            title = stringResource(R.string.drawer_equalizer),
            icon = R.drawable.baseline_colorize_40,
            description = null,
            onClick = onEqualizerClicked,
        )
        Setting(
            title = stringResource(R.string.sleep_timer),
            icon = R.drawable.baseline_send_40,
            description = stringResource(R.string.sleep_timer_status_hint),
            onClick = onSleepTimerClicked,
        )
        Setting(
            title = stringResource(R.string.drawer_graphic_theme),
            icon = R.drawable.baseline_palette_40,
            description = stringResource(R.string.theme_appearance_section),
            onClick = onGraphicThemeClicked,
        )
    }
}

@Composable
private fun HelpAboutSection(
    onFaqClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onRateUsClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    onLanguageClicked: () -> Unit,
    appVersionDisplay: String,
) {
    Column(modifier = Modifier.group()) {
        Setting(
            title = stringResource(R.string.settings_faq),
            icon = Icons.Outlined.Search,
            description = null,
            onClick = onFaqClicked,
        )
        Setting(
            title = stringResource(R.string.settings_feedback),
            icon = R.drawable.baseline_send_40,
            description = null,
            onClick = onFeedbackClicked,
        )
        Setting(
            title = stringResource(R.string.settings_rate_us),
            icon = R.drawable.baseline_bug_report_40,
            description = null,
            onClick = onRateUsClicked,
        )
        Setting(
            title = stringResource(R.string.settings_privacy_policy),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = null,
            onClick = onPrivacyPolicyClicked,
        )
        Setting(
            title = stringResource(R.string.settings_terms_of_use),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = null,
            onClick = onTermsClicked,
        )
        Setting(
            title = stringResource(R.string.settings_language),
            icon = R.drawable.baseline_palette_40,
            description = stringResource(R.string.settings_language_desc),
            onClick = onLanguageClicked,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_version),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = appVersionDisplay,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PremiumAndAdsSection(
    onPremiumClicked: () -> Unit,
) {
    Column(modifier = Modifier.group()) {
        Setting(
            title = stringResource(R.string.settings_premium),
            icon = R.drawable.baseline_palette_40,
            description = stringResource(R.string.settings_premium_desc),
            onClick = onPremiumClicked,
        )
        Setting(
            title = stringResource(R.string.settings_ads_monetization),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.settings_ads_monetization_desc),
            onClick = onPremiumClicked,
        )
    }
}

@Composable
private fun UpdateAvailable(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .group()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.new_version_available),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .weight(1f),
        )
        Button(
            onClick = onClick,
            modifier = Modifier.padding(end = 6.dp),
        ) {
            Text(
                text = stringResource(R.string.update),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun GroupTitle(
    title: String,
){
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(start = 8.dp, top = 8.dp)
    )
}

@Composable
private fun LookAndFeelSettings(
    themePreference: ThemePreference,
    onPreferenceChanged: (ThemePreference) -> Unit,
    tabsSelection: List<Pair<Screens, Boolean>>,
    onTabsSelectChange: (Screens, Boolean) -> Unit,
    onTabsOrderChanged: (fromIdx: Int, toIdx: Int) -> Unit,
    onTabsOrderConfirmed: () -> Unit,
) {
    val seedColor by remember(themePreference.accent){
        derivedStateOf {
            themePreference.accent.getSeedColor()
        }
    }
    var showAccentSelector by remember { mutableStateOf(false) }
    var showSelectorDialog by remember { mutableStateOf(false) }
    val isSystemInDarkMode = isSystemInDarkTheme()
    val icon by remember(themePreference.theme) { derivedStateOf {
        when (themePreference.theme) {
            UserPreferences.Theme.LIGHT_MODE, UserPreferences.Theme.UNRECOGNIZED -> R.drawable.baseline_light_mode_40
            UserPreferences.Theme.DARK_MODE, UserPreferences.Theme.AMOLED_MODE -> R.drawable.baseline_dark_mode_40
            UserPreferences.Theme.USE_SYSTEM_MODE -> {
                if (isSystemInDarkMode){
                    R.drawable.baseline_dark_mode_40
                } else {
                    R.drawable.baseline_light_mode_40
                }
            }
        }
    } }
    var showRearrangeTabsDialog by remember{ mutableStateOf(false) }

    Column(
        modifier = Modifier.group()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Setting(
                title = "Material You",
                icon = R.drawable.baseline_palette_40,
                description = stringResource(R.string.use_a_theme_generated_from_your_device_wallpaper),
                isChecked = themePreference.useMaterialYou,
                onCheckedChanged = {
                    onPreferenceChanged(themePreference.copy(useMaterialYou = it))
                },
            )
        }
        AccentSetting(
            onClick = {
                if (!themePreference.useMaterialYou) {
                    showAccentSelector = true
                }
            },
            seedColor = seedColor,
            modifier = Modifier
                .alpha(if (themePreference.useMaterialYou) 0.5f else 1f),
        )
        Setting(
            title = stringResource(R.string.theme_mode),
            icon = icon,
            onClick = { showSelectorDialog = true },
            description = stringResource(R.string.choose_a_theme_mode)
        )
        Setting(
            title = stringResource(R.string.tabs_arrangement),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.select_and_reorder_the_tabs_shown),
            onClick = { showRearrangeTabsDialog = true }
        )
    }
    if (showAccentSelector){
        AccentSelectorDialog(
            themePreference = themePreference,
            onPreferenceChanged = onPreferenceChanged,
            onDismissRequest = { showAccentSelector = false },
        )
    }
    if (showSelectorDialog) {
        ThemeSelectorDialog(
            themePreference = themePreference,
            onPreferenceChanged = onPreferenceChanged,
            onDismissRequest = { showSelectorDialog = false }
        )
    }
    if (showRearrangeTabsDialog){
        RearrangeTabsDialog(
            tabsSelection = tabsSelection,
            onDismissRequest = { showRearrangeTabsDialog = false },
            onSelectChange = onTabsSelectChange,
            onTabsOrderChanged = onTabsOrderChanged,
            onTabsOrderConfirmed = {
                showRearrangeTabsDialog = false
                onTabsOrderConfirmed()
            }
        )
    }
}

@Composable
private fun AccentSelectorDialog(
    themePreference: ThemePreference,
    onPreferenceChanged: (ThemePreference) -> Unit,
    onDismissRequest: () -> Unit,
){
    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.accent_color),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = onDismissRequest
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ){
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DrawAccentCircle(
                        accentColour = Accent.Default.getSeedColor(),
                        isSelected = themePreference.accent == Accent.Default,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.Default))
                        }
                    )
                    DrawAccentCircle(
                        accentColour = Accent.Malibu.getSeedColor(),
                        isSelected = themePreference.accent == Accent.Malibu,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.Malibu))
                        }
                    )
                    DrawAccentCircle(
                        accentColour = Accent.Melrose.getSeedColor(),
                        isSelected = themePreference.accent == Accent.Melrose,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.Melrose))
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DrawAccentCircle(
                        accentColour = Accent.Elm.getSeedColor(),
                        isSelected = themePreference.accent == Accent.Elm,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.Elm))
                        }
                    )
                    DrawAccentCircle(
                        accentColour = Accent.Magenta.getSeedColor(),
                        isSelected = themePreference.accent == Accent.Magenta,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.Magenta))
                        }
                    )
                    DrawAccentCircle(
                        accentColour = Accent.JacksonsPurple.getSeedColor(),
                        isSelected = themePreference.accent == Accent.JacksonsPurple,
                        onClick = {
                            onPreferenceChanged(themePreference.copy(accent = Accent.JacksonsPurple))
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun DrawAccentCircle(
    accentColour: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColour = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .size(56.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
    ) {
        if (isSelected) {
            drawArc(
                color = borderColour,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                style = Stroke(width = 3.dp.toPx()),
            )
        }
        drawCircle(
            color = accentColour,
            radius = size.minDimension * 0.45f,
        )
    }
}

@Composable
private fun ThemeSelectorDialog(
    themePreference: ThemePreference,
    onPreferenceChanged: (ThemePreference) -> Unit,
    onDismissRequest: () -> Unit,
){
    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = onDismissRequest
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                ThemeMode(
                    isSelected = (themePreference.theme == UserPreferences.Theme.LIGHT_MODE),
                    text = stringResource(R.string.light_mode),
                    onClick = {
                        onPreferenceChanged(themePreference.copy(theme = UserPreferences.Theme.LIGHT_MODE))
                    }
                )
                ThemeMode(
                    isSelected = (themePreference.theme == UserPreferences.Theme.DARK_MODE),
                    text = stringResource(R.string.dark_mode),
                    onClick = {
                        onPreferenceChanged(themePreference.copy(theme = UserPreferences.Theme.DARK_MODE))
                    }
                )
                ThemeMode(
                    isSelected = (themePreference.theme == UserPreferences.Theme.USE_SYSTEM_MODE),
                    text = stringResource(R.string.system_mode),
                    onClick = {
                        onPreferenceChanged(themePreference.copy(theme = UserPreferences.Theme.USE_SYSTEM_MODE))
                    }
                )
                ThemeMode(
                    isSelected = (themePreference.theme == UserPreferences.Theme.AMOLED_MODE),
                    text = stringResource(R.string.theme_amoled),
                    onClick = {
                        onPreferenceChanged(themePreference.copy(theme = UserPreferences.Theme.AMOLED_MODE))
                    }
                )
            }
        }
    )
}

@Composable
private fun ThemeMode(
    isSelected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RearrangeTabsDialog(
    tabsSelection: List<Pair<Screens,Boolean>>,
    onDismissRequest: () -> Unit,
    onSelectChange: (Screens, Boolean) -> Unit,
    onTabsOrderChanged: (fromIdx: Int, toIdx: Int) -> Unit,
    onTabsOrderConfirmed: () -> Unit,
){
    AlertDialog(
        title = {
            Text(
                text = stringResource(R.string.app_tabs),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = onTabsOrderConfirmed
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            val listState = rememberLazyListState()
            val dragDropState = rememberDragDropState(
                lazyListState = listState,
                onMove = onTabsOrderChanged,
            )
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .dragContainer(dragDropState)
            ){
                itemsIndexed(
                    items = tabsSelection,
                    key = { index, screenChoice -> screenChoice.first.ordinal }
                ){ index, screenChoice ->
                    DraggableItem(dragDropState, index) {
                        SelectableMovableScreen(
                            screen = screenChoice.first,
                            isSelected = screenChoice.second,
                            onSelectChange = { isSelected -> onSelectChange(screenChoice.first, isSelected) },
                        )
                    }
                }
            }
        },

    )
}


@Composable
private fun SelectableMovableScreen(
    screen: Screens,
    isSelected: Boolean,
    onSelectChange: (Boolean) -> Unit,
){
    val spaceModifier = Modifier.width(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        verticalAlignment = Alignment.CenterVertically,
    ){
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectChange,
        )
        Icon(
            painter = painterResource(id = screen.filledIcon),
            contentDescription = stringResource(R.string.screen_icon, screen.name),
            modifier = Modifier.size(35.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Spacer(spaceModifier)
        Text(
            text = screen.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.weight(1f)
        )
        Spacer(spaceModifier)
        Icon(
            painter = painterResource(id = R.drawable.baseline_drag_indicator_40),
            contentDescription = stringResource(R.string.drag_icon),
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun MusicLibrarySettings(
    scanStatus: ScanStatus,
    onScanClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onRestoreFoldersClicked: () -> Unit,
    onBackupRestoreClicked: () -> Unit,
    onHiddenMusicClicked: () -> Unit,
) {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(key1 = scanStatus){
        if (scanStatus is ScanStatus.ScanComplete) {
            progress = 1f
        } else if (scanStatus is ScanStatus.ScanProgress){
            progress = if (scanStatus.total == 0){
                1f
            } else {
                scanStatus.parsed.toFloat()/scanStatus.total.toFloat()
            }
        }
    }
    Column(
        modifier = Modifier
            .group()
            .animateContentSize(),
    ) {
        Setting(
            title = stringResource(R.string.settings_backup_restore_title),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = stringResource(R.string.settings_backup_restore_desc),
            onClick = onBackupRestoreClicked,
        )
        Setting(
            title = stringResource(R.string.rescan_for_music),
            icon = Icons.Outlined.Search,
            description = stringResource(R.string.search_for_all_the_songs_on_this_device_and_update_the_library),
            onClick = {
                if (scanStatus is ScanStatus.ScanNotRunning){
                    onScanClicked()
                }
            },
        )
        if (scanStatus is ScanStatus.ScanProgress){
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress
            )
        } else if (scanStatus is ScanStatus.ScanComplete){
            Text(
                text = stringResource(R.string.done),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Setting(
            title = stringResource(R.string.restore_blacklisted_songs),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = stringResource(R.string.add_songs_back_to_the_library),
            onClick = onRestoreClicked
        )
        Setting(
            title = stringResource(R.string.restore_blacklisted_folders),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = stringResource(R.string.add_folders_back_to_the_library),
            onClick = onRestoreFoldersClicked
        )
        Setting(
            title = stringResource(R.string.settings_hidden_music),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.settings_hidden_music_desc),
            onClick = onHiddenMusicClicked
        )
    }
}

private fun getSystemDetail(): String {
    return "Brand: ${Build.BRAND} \n" +
            "Model: ${Build.MODEL} \n" +
            "SDK: ${Build.VERSION.SDK_INT} \n" +
            "Manufacturer: ${Build.MANUFACTURER} \n" +
            "Version Code: ${Build.VERSION.RELEASE} \n" +
            "App Version Name: ${BuildConfig.VERSION_NAME}"
}

@Composable
private fun ReportBug(
    disabledCrashlytics: Boolean,
    onAutoReportCrashClicked: (Boolean) -> Unit,
){
    val context = LocalContext.current
    Column(
        modifier = Modifier.group()
    ) {
        Setting(
            title = stringResource(R.string.auto_crash_reporting),
            icon = R.drawable.baseline_send_40,
            description = stringResource(R.string.enable_this_to_automatically_send_crash_reports_to_the_developer),
            isChecked = !disabledCrashlytics,
            onCheckedChanged = onAutoReportCrashClicked,
        )
        Setting(
            title = stringResource(R.string.report_any_bugs_crashes),
            icon = R.drawable.baseline_bug_report_40,
            description = stringResource(R.string.manually_report_any_bugs_or_crashes_you_faced),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("music.zen@outlook.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "Zen Music | Bug Report")
                    putExtra(
                        Intent.EXTRA_TEXT,
                        getSystemDetail() + "\n\n[${context.getString(R.string.describe_the_bug_or_crash_here)}]\n"
                    )
                    data = Uri.parse("mailto:")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        )
    }
}

//@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MadeBy(
    onWhatsNewClicked: () -> Unit,
) {
//    val githubUrl = "https://github.com/pakka-papad"
//    val linkedinUrl = "https://www.linkedin.com/in/sumitzbera/"
//    val context = LocalContext.current
    val appVersion = stringResource(id = R.string.app_version_name)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val semiTransparentSpanStyle = SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(
            text = buildAnnotatedString {
                withStyle(semiTransparentSpanStyle) {
                    append("${stringResource(R.string.app_version)} ")
                }
                append(appVersion)
            },
            style = MaterialTheme.typography.titleSmall,
        )
//        Text(
//            text = stringResource(R.string.check_what_s_new),
//            modifier = Modifier
//                .alpha(0.5f)
//                .clickable(onClick = onWhatsNewClicked),
//            style = MaterialTheme.typography.titleSmall.copy(textDecoration = TextDecoration.Underline)
//        )
//        Spacer(Modifier.height(6.dp))
//        Text(
//            text = buildAnnotatedString {
//                withStyle(semiTransparentSpanStyle) {
//                    append("${stringResource(R.string.made_by)} ")
//                }
//                append("Sumit Bera")
//            },
//            style = MaterialTheme.typography.titleMedium,
//        )
//        val iconModifier = Modifier
//            .size(30.dp)
//            .alpha(0.5f)
//        Row(
//            horizontalArrangement = Arrangement.spacedBy(24.dp)
//        ) {
//            Image(
//                painter = painterResource(R.drawable.github_mark),
//                contentDescription = "github",
//                modifier = iconModifier
//                    .combinedClickable(
//                        onClick = {
//                            val intent = Intent(Intent.ACTION_VIEW)
//                            intent.data = Uri.parse(githubUrl)
//                            try {
//                                context.startActivity(intent)
//                            } catch (_: Exception){
//                                Toast.makeText(context,"Error opening url. Long press to copy.",Toast.LENGTH_SHORT).show()
//                            }
//                        },
//                        onLongClick = {
//                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
//                            if (clipboardManager == null){
//                                Toast.makeText(context,"Error",Toast.LENGTH_SHORT).show()
//                            } else {
//                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Github url",githubUrl))
//                                Toast.makeText(context,"Copied",Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    ),
//                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
//                contentScale = ContentScale.Inside,
//            )
//            Image(
//                painter = painterResource(R.drawable.linkedin),
//                contentDescription = "linkedin",
//                modifier = iconModifier
//                    .combinedClickable(
//                        onClick = {
//                            val intent = Intent(Intent.ACTION_VIEW)
//                            intent.data = Uri.parse(linkedinUrl)
//                            try {
//                                context.startActivity(intent)
//                            } catch (_: Exception){
//                                Toast.makeText(context,"Error opening url. Long press to copy.",Toast.LENGTH_SHORT).show()
//                            }
//                        },
//                        onLongClick = {
//                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
//                            if (clipboardManager == null){
//                                Toast.makeText(context,"Error",Toast.LENGTH_SHORT).show()
//                            } else {
//                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Linkedin url",linkedinUrl))
//                                Toast.makeText(context,"Copied",Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    ),
//                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
//                contentScale = ContentScale.Inside,
//            )
//        }
        Spacer(Modifier.height(36.dp))
    }
}

private fun Modifier.group() = composed {
    this
        .fillMaxWidth()
        .padding(8.dp)
        .clip(MaterialTheme.shapes.large)
        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
}


@Composable
private fun Setting(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    isChecked: Boolean? = null,
    onCheckedChanged: ((Boolean) -> Unit)? = null,
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.setting_icon, title),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(6.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ){
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            description?.let {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        isChecked?.let {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChanged,
            )
        }
    }
}


@Composable
private fun Setting(
    title: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    onClick: (() -> Unit)? = null,
    isChecked: Boolean? = null,
    onCheckedChanged: ((Boolean) -> Unit)? = null,
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(R.string.setting_icon, title),
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(6.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ){
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            description?.let {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }
        isChecked?.let {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChanged,
            )
        }
    }
}

@Composable
private fun AccentSetting(
    onClick: () -> Unit,
    seedColor: Color,
    modifier: Modifier = Modifier,
){
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_colorize_40),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(6.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ){
            Text(
                text = stringResource(R.string.accent_color),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.choose_an_accent_color),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
        Box(
            modifier = Modifier
                .height(32.dp)
                .width(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color = seedColor)
        )
    }
}