package com.github.pakka_papad.equalizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R
import com.github.pakka_papad.data.UserPreferences
import kotlin.math.roundToInt

private val SelectablePresets = listOf(
    UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL,
    UserPreferences.EqualizerPreset.EQUALIZER_PRESET_BASS,
    UserPreferences.EqualizerPreset.EQUALIZER_PRESET_ROCK,
    UserPreferences.EqualizerPreset.EQUALIZER_PRESET_POP,
    UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CLASSICAL,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.equalizerSettings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!ui.effectsAttached) {
            Text(
                text = stringResource(R.string.equalizer_no_audio_session_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.equalizer_master_enable),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = ui.equalizerEnabled,
                onCheckedChange = viewModel::onEqualizerMasterEnabled,
            )
        }

        Text(
            text = stringResource(R.string.equalizer_preset_label),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SelectablePresets.forEach { preset ->
                FilterChip(
                    selected = settings.preset == preset,
                    onClick = { viewModel.onPresetSelected(preset) },
                    enabled = ui.equalizerEnabled,
                    label = { Text(presetTitle(preset)) },
                )
            }
            if (settings.preset == UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text(stringResource(R.string.equalizer_preset_custom)) },
                )
            }
        }

        Text(
            text = stringResource(R.string.equalizer_bands_label),
            style = MaterialTheme.typography.titleSmall,
        )
        val bandCount = ui.centerFreqHz.size
        for (index in 0 until bandCount) {
            val hz = ui.centerFreqHz[index]
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatCenterFreq(hz),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = ui.levelsMb.getOrElse(index) { 0 }.toFloat(),
                    onValueChange = { viewModel.onBandLevelChange(index, it.roundToInt()) },
                    valueRange = ui.levelMinMb.toFloat()..ui.levelMaxMb.toFloat(),
                    enabled = ui.equalizerEnabled,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Text(
            text = stringResource(R.string.equalizer_bass_boost),
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = ui.bassStrength.toFloat(),
            onValueChange = { viewModel.onBassChange(it.roundToInt()) },
            valueRange = 0f..1000f,
            steps = 99,
            enabled = ui.equalizerEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.equalizer_bass_value, ui.bassStrength),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.equalizer_virtualizer),
            style = MaterialTheme.typography.titleSmall,
        )
        if (ui.virtualizerSupported) {
            Slider(
                value = ui.virtualizerStrength.toFloat(),
                onValueChange = { viewModel.onVirtualizerChange(it.roundToInt()) },
                valueRange = 0f..1000f,
                steps = 99,
                enabled = ui.equalizerEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.equalizer_virtualizer_value, ui.virtualizerStrength),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.equalizer_virtualizer_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.equalizer_reset))
        }
    }
}

@Composable
private fun presetTitle(preset: UserPreferences.EqualizerPreset): String {
    return when (preset) {
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL ->
            stringResource(R.string.equalizer_preset_normal)
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_BASS ->
            stringResource(R.string.equalizer_preset_bass)
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_ROCK ->
            stringResource(R.string.equalizer_preset_rock)
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_POP ->
            stringResource(R.string.equalizer_preset_pop)
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CLASSICAL ->
            stringResource(R.string.equalizer_preset_classical)
        UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM ->
            stringResource(R.string.equalizer_preset_custom)
        UserPreferences.EqualizerPreset.UNRECOGNIZED ->
            stringResource(R.string.equalizer_preset_normal)
    }
}

private fun formatCenterFreq(hz: Float): String {
    return if (hz >= 1000f) {
        "%.1f kHz".format(hz / 1000f)
    } else {
        "%.0f Hz".format(hz)
    }
}
