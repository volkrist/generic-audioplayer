package com.generic.audioplayes.theme

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import com.generic.audioplayes.data.UserPreferences
import com.generic.audioplayes.data.UserPreferences.Accent
import com.generic.audioplayes.ui.theme.ThemePreference
import com.generic.audioplayes.ui.theme.getSeedColor

@Composable
fun ThemeScreen(
    viewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val accentEnabled =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || !themePreference.useMaterialYou

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeCurrentSummary(themePreference = themePreference)
        Text(
            text = stringResource(R.string.theme_appearance_section),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Column(Modifier.selectableGroup()) {
            ThemeModeRow(
                selected = themePreference.theme == UserPreferences.Theme.USE_SYSTEM_MODE,
                label = stringResource(R.string.system_mode),
                onClick = {
                    viewModel.updateTheme(themePreference.copy(theme = UserPreferences.Theme.USE_SYSTEM_MODE))
                },
            )
            ThemeModeRow(
                selected = themePreference.theme == UserPreferences.Theme.LIGHT_MODE,
                label = stringResource(R.string.light_mode),
                onClick = {
                    viewModel.updateTheme(themePreference.copy(theme = UserPreferences.Theme.LIGHT_MODE))
                },
            )
            ThemeModeRow(
                selected = themePreference.theme == UserPreferences.Theme.DARK_MODE,
                label = stringResource(R.string.dark_mode),
                onClick = {
                    viewModel.updateTheme(themePreference.copy(theme = UserPreferences.Theme.DARK_MODE))
                },
            )
            ThemeModeRow(
                selected = themePreference.theme == UserPreferences.Theme.AMOLED_MODE,
                label = stringResource(R.string.theme_amoled),
                onClick = {
                    viewModel.updateTheme(themePreference.copy(theme = UserPreferences.Theme.AMOLED_MODE))
                },
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Spacer(Modifier.height(8.dp))
            Divider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.material_you_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.use_a_theme_generated_from_your_device_wallpaper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = themePreference.useMaterialYou,
                    onCheckedChange = {
                        viewModel.updateTheme(themePreference.copy(useMaterialYou = it))
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Divider()
        Text(
            text = stringResource(R.string.accent_color),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Text(
            text = stringResource(R.string.theme_accent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        val accents = listOf(
            Triple(Accent.Default, R.string.accent_default, Accent.Default.getSeedColor()),
            Triple(Accent.Malibu, R.string.accent_blue, Accent.Malibu.getSeedColor()),
            Triple(Accent.Magenta, R.string.accent_red, Accent.Magenta.getSeedColor()),
            Triple(Accent.Elm, R.string.accent_green, Accent.Elm.getSeedColor()),
            Triple(Accent.Melrose, R.string.accent_orange, Accent.Melrose.getSeedColor()),
            Triple(Accent.JacksonsPurple, R.string.accent_purple, Accent.JacksonsPurple.getSeedColor()),
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            accents.chunked(3).forEach { rowAccents ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    rowAccents.forEach { (accent, labelRes, color) ->
                        AccentChoice(
                            label = stringResource(labelRes),
                            color = color,
                            selected = themePreference.accent == accent,
                            enabled = accentEnabled,
                            onClick = {
                                viewModel.updateTheme(themePreference.copy(accent = accent))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeCurrentSummary(themePreference: ThemePreference) {
    val modeLabel = when (themePreference.theme) {
        UserPreferences.Theme.USE_SYSTEM_MODE, UserPreferences.Theme.UNRECOGNIZED ->
            stringResource(R.string.system_mode)
        UserPreferences.Theme.LIGHT_MODE -> stringResource(R.string.light_mode)
        UserPreferences.Theme.DARK_MODE -> stringResource(R.string.dark_mode)
        UserPreferences.Theme.AMOLED_MODE -> stringResource(R.string.theme_amoled)
    }
    val accentLabel = when (themePreference.accent) {
        Accent.Default, Accent.UNRECOGNIZED -> stringResource(R.string.accent_default)
        Accent.Malibu -> stringResource(R.string.accent_blue)
        Accent.Melrose -> stringResource(R.string.accent_orange)
        Accent.Elm -> stringResource(R.string.accent_green)
        Accent.Magenta -> stringResource(R.string.accent_red)
        Accent.JacksonsPurple -> stringResource(R.string.accent_purple)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.theme_summary_mode, modeLabel),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Text(
                text = stringResource(
                    R.string.theme_summary_material_you,
                    stringResource(
                        if (themePreference.useMaterialYou) {
                            R.string.theme_state_on
                        } else {
                            R.string.theme_state_off
                        },
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.theme_summary_accent, accentLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeModeRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { onClick() }
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun AccentChoice(
    label: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val borderColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .size(56.dp)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        ) {
            if (selected) {
                drawArc(
                    color = borderColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = true,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
            drawCircle(
                color = color,
                radius = size.minDimension * 0.45f,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        )
    }
}
