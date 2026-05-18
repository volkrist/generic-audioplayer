package com.generic.audioplayes.settings

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
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
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
import com.generic.audioplayes.BuildConfig
import com.generic.audioplayes.R
import com.generic.audioplayes.Screens
import com.generic.audioplayes.data.UserPreferences
import com.generic.audioplayes.data.UserPreferences.Accent
import com.generic.audioplayes.data.music.ScanStatus
import com.generic.audioplayes.nowplaying.DraggableItem
import com.generic.audioplayes.nowplaying.dragContainer
import com.generic.audioplayes.nowplaying.rememberDragDropState
import com.generic.audioplayes.ui.theme.ThemePreference
import com.generic.audioplayes.ui.theme.getSeedColor
import timber.log.Timber
import java.text.DateFormat
import java.util.Date

@Composable
fun SettingsList(
    paddingValues: PaddingValues,
    lastBackupEpochMs: Long,
    crossfadeEnabled: Boolean,
    onCrossfadeChanged: (Boolean) -> Unit,
    gaplessPlaybackEnabled: Boolean,
    onGaplessChanged: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onHiddenMusicClicked: () -> Unit,
    onBackupRestoreClicked: () -> Unit,
    showOnLockScreen: Boolean,
    onShowOnLockScreenChanged: (Boolean) -> Unit,
    pauseOnHeadsetDisconnect: Boolean,
    onPauseOnHeadsetChanged: (Boolean) -> Unit,
    onLanguageClicked: () -> Unit,
    onFaqClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onRateUsClicked: () -> Unit,
    onPremiumClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    appVersionDisplay: String,
    stage4BuildMarker: String? = null,
) {
    val settingsGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0B1028),
            Color(0xFF12102A),
            Color(0xFF1A0F2E),
            Color(0xFF2D0D45),
            Color(0xFF32104A),
        ),
    )
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(settingsGradient)
            .padding(horizontal = 14.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            ReferenceStyleMainSettingsBlock(
                lastBackupEpochMs = lastBackupEpochMs,
                crossfadeEnabled = crossfadeEnabled,
                onCrossfadeChanged = onCrossfadeChanged,
                gaplessPlaybackEnabled = gaplessPlaybackEnabled,
                onGaplessChanged = onGaplessChanged,
                keepScreenOn = keepScreenOn,
                onKeepScreenOnChanged = onKeepScreenOnChanged,
                onHiddenMusicClicked = onHiddenMusicClicked,
                onBackupRestoreClicked = onBackupRestoreClicked,
                showOnLockScreen = showOnLockScreen,
                onShowOnLockScreenChanged = onShowOnLockScreenChanged,
                pauseOnHeadsetDisconnect = pauseOnHeadsetDisconnect,
                onPauseOnHeadsetChanged = onPauseOnHeadsetChanged,
                onLanguageClicked = onLanguageClicked,
            )
        }
        item {
            ReferenceHelpSection(
                onFaqClicked = onFaqClicked,
                onFeedbackClicked = onFeedbackClicked,
                onRateUsClicked = onRateUsClicked,
                onPremiumClicked = onPremiumClicked,
                onPrivacyPolicyClicked = onPrivacyPolicyClicked,
                onTermsClicked = onTermsClicked,
                appVersionDisplay = appVersionDisplay,
                stage4BuildMarker = stage4BuildMarker,
            )
        }
    }
}

@Composable
private fun ReferenceStyleMainSettingsBlock(
    lastBackupEpochMs: Long,
    crossfadeEnabled: Boolean,
    onCrossfadeChanged: (Boolean) -> Unit,
    gaplessPlaybackEnabled: Boolean,
    onGaplessChanged: (Boolean) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onHiddenMusicClicked: () -> Unit,
    onBackupRestoreClicked: () -> Unit,
    showOnLockScreen: Boolean,
    onShowOnLockScreenChanged: (Boolean) -> Unit,
    pauseOnHeadsetDisconnect: Boolean,
    onPauseOnHeadsetChanged: (Boolean) -> Unit,
    onLanguageClicked: () -> Unit,
) {
    val context = LocalContext.current
    val backupSubtitle = remember(lastBackupEpochMs) {
        val label = if (lastBackupEpochMs > 0L) {
            DateFormat.getDateTimeInstance().format(Date(lastBackupEpochMs))
        } else {
            context.getString(R.string.backup_never_exported)
        }
        context.getString(R.string.backup_last_export, label)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Setting(
            title = stringResource(R.string.settings_hidden_music),
            icon = Icons.Outlined.VisibilityOff,
            description = stringResource(R.string.settings_hidden_music_desc),
            onClick = onHiddenMusicClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_backup_restore_title),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = backupSubtitle,
            onClick = onBackupRestoreClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_crossfade),
            icon = Icons.Outlined.BlurOn,
            description = stringResource(R.string.settings_crossfade_desc),
            isChecked = crossfadeEnabled,
            onCheckedChanged = onCrossfadeChanged,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_gapless),
            icon = Icons.Outlined.PlayCircle,
            description = stringResource(R.string.settings_gapless_desc),
            isChecked = gaplessPlaybackEnabled,
            onCheckedChanged = onGaplessChanged,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_keep_screen_on),
            icon = Icons.Outlined.WbSunny,
            description = stringResource(R.string.settings_keep_screen_on_desc),
            isChecked = keepScreenOn,
            onCheckedChanged = onKeepScreenOnChanged,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_show_lock_screen),
            icon = Icons.Outlined.Lock,
            description = stringResource(R.string.settings_show_lock_screen_desc),
            isChecked = showOnLockScreen,
            onCheckedChanged = onShowOnLockScreenChanged,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_pause_headset),
            icon = Icons.Outlined.PauseCircle,
            description = stringResource(R.string.settings_pause_headset_desc),
            isChecked = pauseOnHeadsetDisconnect,
            onCheckedChanged = onPauseOnHeadsetChanged,
            minimalStyle = true,
            emphasisSwitch = true,
        )
        Setting(
            title = stringResource(R.string.settings_language),
            icon = Icons.Outlined.Language,
            description = stringResource(R.string.settings_language_desc),
            onClick = onLanguageClicked,
            minimalStyle = true,
        )
    }
}

@Composable
private fun HelpSectionTitle(
    title: String,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFFFD54F),
        modifier = Modifier
            .padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ReferenceHelpSection(
    onFaqClicked: () -> Unit,
    onFeedbackClicked: () -> Unit,
    onRateUsClicked: () -> Unit,
    onPremiumClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onTermsClicked: () -> Unit,
    appVersionDisplay: String,
    stage4BuildMarker: String? = null,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HelpSectionTitle(title = stringResource(R.string.settings_section_help))
        Setting(
            title = stringResource(R.string.settings_faq),
            icon = Icons.Outlined.HelpOutline,
            description = stringResource(R.string.settings_faq_desc),
            onClick = onFaqClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_feedback),
            icon = Icons.Outlined.MailOutline,
            description = stringResource(R.string.settings_feedback_desc),
            onClick = onFeedbackClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_rate_us),
            icon = Icons.Outlined.ThumbUp,
            description = stringResource(R.string.settings_rate_us_desc),
            onClick = onRateUsClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_premium),
            icon = Icons.Outlined.Verified,
            description = null,
            onClick = onPremiumClicked,
            minimalStyle = true,
            trailingDot = true,
        )
        Setting(
            title = stringResource(R.string.settings_privacy_policy),
            icon = Icons.Outlined.Security,
            onClick = onPrivacyPolicyClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.settings_terms_of_use),
            icon = Icons.Outlined.Description,
            onClick = onTermsClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.app_version),
            icon = Icons.Outlined.Info,
            description = appVersionDisplay,
            minimalStyle = true,
        )
        stage4BuildMarker?.takeIf { it.isNotBlank() }?.let { marker ->
            Setting(
                title = marker,
                icon = Icons.Outlined.Build,
                description = null,
                minimalStyle = true,
            )
        }
    }
}

@Composable
private fun FeaturesToolsSection(
    onEqualizerClicked: () -> Unit,
    onSleepTimerClicked: () -> Unit,
    onGraphicThemeClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Setting(
            title = stringResource(R.string.drawer_equalizer),
            icon = R.drawable.baseline_colorize_40,
            description = null,
            onClick = onEqualizerClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.sleep_timer),
            icon = R.drawable.baseline_send_40,
            description = stringResource(R.string.sleep_timer_status_hint),
            onClick = onSleepTimerClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.drawer_graphic_theme),
            icon = R.drawable.baseline_palette_40,
            description = stringResource(R.string.theme_appearance_section),
            onClick = onGraphicThemeClicked,
            minimalStyle = true,
        )
    }
}

@Composable
private fun AdsOnlySection(
    onPremiumClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Setting(
            title = stringResource(R.string.settings_ads_monetization),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.settings_ads_monetization_desc),
            onClick = onPremiumClicked,
            minimalStyle = true,
        )
    }
}

@Composable
private fun UpdateAvailable(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
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
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        modifier = Modifier
            .padding(start = 4.dp, top = 14.dp, bottom = 4.dp),
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
        modifier = Modifier.fillMaxWidth(),
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
                minimalStyle = true,
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
            description = stringResource(R.string.choose_a_theme_mode),
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.tabs_arrangement),
            icon = R.drawable.ic_baseline_library_music_40,
            description = stringResource(R.string.select_and_reorder_the_tabs_shown),
            onClick = { showRearrangeTabsDialog = true },
            minimalStyle = true,
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
private fun RescanAndBlacklistSection(
    scanStatus: ScanStatus,
    onScanClicked: () -> Unit,
    onRestoreClicked: () -> Unit,
    onRestoreFoldersClicked: () -> Unit,
) {
    var progress by remember { mutableStateOf(0f) }
    LaunchedEffect(key1 = scanStatus) {
        if (scanStatus is ScanStatus.ScanComplete) {
            progress = 1f
        } else if (scanStatus is ScanStatus.ScanProgress) {
            progress = if (scanStatus.total == 0) {
                1f
            } else {
                scanStatus.parsed.toFloat() / scanStatus.total.toFloat()
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
    ) {
        Setting(
            title = stringResource(R.string.rescan_for_music),
            icon = Icons.Outlined.Search,
            description = stringResource(R.string.search_for_all_the_songs_on_this_device_and_update_the_library),
            onClick = {
                if (scanStatus is ScanStatus.ScanNotRunning) {
                    onScanClicked()
                }
            },
            minimalStyle = true,
        )
        if (scanStatus is ScanStatus.ScanProgress) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                progress = progress,
            )
        } else if (scanStatus is ScanStatus.ScanComplete) {
            Text(
                text = stringResource(R.string.done),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Setting(
            title = stringResource(R.string.restore_blacklisted_songs),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = stringResource(R.string.add_songs_back_to_the_library),
            onClick = onRestoreClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.restore_blacklisted_folders),
            icon = R.drawable.baseline_settings_backup_restore_40,
            description = stringResource(R.string.add_folders_back_to_the_library),
            onClick = onRestoreFoldersClicked,
            minimalStyle = true,
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
        modifier = Modifier.fillMaxWidth(),
    ) {
        Setting(
            title = stringResource(R.string.auto_crash_reporting),
            icon = R.drawable.baseline_send_40,
            description = stringResource(R.string.enable_this_to_automatically_send_crash_reports_to_the_developer),
            isChecked = !disabledCrashlytics,
            onCheckedChanged = onAutoReportCrashClicked,
            minimalStyle = true,
        )
        Setting(
            title = stringResource(R.string.report_any_bugs_crashes),
            icon = R.drawable.baseline_bug_report_40,
            description = stringResource(R.string.manually_report_any_bugs_or_crashes_you_faced),
            onClick = {
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.apply {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf("audioplayer.app@outlook.com"))
                    putExtra(Intent.EXTRA_SUBJECT, "AudioPlayer | Bug Report")
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
            },
            minimalStyle = true,
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
    minimalStyle: Boolean = false,
    trailingDot: Boolean = false,
    emphasisSwitch: Boolean = false,
) {
    val accentSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color(0xFFFFB74D),
        checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.45f),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = if (minimalStyle) 12.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.setting_icon, title),
            modifier = Modifier
                .size(if (minimalStyle) 28.dp else 40.dp)
                .then(
                    if (minimalStyle) {
                        Modifier
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(6.dp)
                    }
                ),
            tint = if (minimalStyle) {
                Color.White.copy(alpha = 0.92f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (minimalStyle) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (minimalStyle) {
                        Color(0xFFB8B8C8)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    },
                )
            }
        }
        when {
            isChecked != null && onCheckedChanged != null -> {
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChanged,
                    colors = if (emphasisSwitch) accentSwitchColors else SwitchDefaults.colors(),
                )
            }
            trailingDot -> {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            }
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
    minimalStyle: Boolean = false,
    trailingDot: Boolean = false,
    emphasisSwitch: Boolean = false,
) {
    val accentSwitchColors = SwitchDefaults.colors(
        checkedThumbColor = Color(0xFFFFB74D),
        checkedTrackColor = Color(0xFFFF9800).copy(alpha = 0.45f),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = if (minimalStyle) 12.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = stringResource(R.string.setting_icon, title),
            modifier = Modifier
                .size(if (minimalStyle) 28.dp else 40.dp)
                .then(
                    if (minimalStyle) {
                        Modifier
                    } else {
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(6.dp)
                    }
                ),
            tint = if (minimalStyle) {
                Color.White.copy(alpha = 0.92f)
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (minimalStyle) Color.White else MaterialTheme.colorScheme.onSurface,
            )
            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (minimalStyle) {
                        Color(0xFFB8B8C8)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    },
                )
            }
        }
        when {
            isChecked != null && onCheckedChanged != null -> {
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChanged,
                    colors = if (emphasisSwitch) accentSwitchColors else SwitchDefaults.colors(),
                )
            }
            trailingDot -> {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            }
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