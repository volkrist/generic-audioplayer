package com.generic.audioplayes.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import com.generic.audioplayes.data.UserPreferences
import kotlin.math.abs
import kotlin.math.roundToInt

private val EqBg = Color(0xFF121212)
private val EqAccent = Color(0xFFD48824)
private val EqChipInactive = Color(0xFF2C2C2C)
private val EqMuted = Color(0xFFA0A0A0)
private val EqTrackDim = Color(0xFF3A3A3A)
private val EqSegmentBg = Color(0xFF1E1E1E)

private data class ReverbOption(val id: Int, val labelRes: Int)

@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.equalizerSettings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(EqBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        EqHeader(
            title = stringResource(R.string.drawer_equalizer),
            enabled = ui.equalizerEnabled,
            onBack = onBack,
            onEnabledChange = viewModel::onEqualizerMasterEnabled,
            showSessionWarning = !ui.effectsAttached,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithContent {
                    drawContent()
                    if (!ui.equalizerEnabled) {
                        drawRect(color = Color.Black.copy(alpha = 0.32f))
                    }
                },
        ) {
        if (!ui.effectsAttached) {
            Text(
                text = stringResource(R.string.equalizer_no_audio_session_hint),
                color = EqMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        PresetChipRow(
            presets = listOf(
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_CUSTOM to R.string.equalizer_preset_personalization,
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_POP to R.string.equalizer_preset_electronic,
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL to R.string.equalizer_preset_flat,
            ),
            selected = settings.preset,
            enabled = ui.equalizerEnabled,
            onSelect = viewModel::onPresetSelected,
        )
        PresetChipRow(
            presets = listOf(
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_BASS to R.string.equalizer_preset_bass_boost,
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL to R.string.equalizer_preset_usual,
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_ROCK to R.string.equalizer_preset_rock_short,
            ),
            selected = settings.preset,
            enabled = ui.equalizerEnabled,
            onSelect = viewModel::onPresetSelected,
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            val n = ui.levelsMb.size
            repeat(n) { index ->
                VerticalEqBand(
                    valueMb = ui.levelsMb[index],
                    minMb = ui.levelMinMb,
                    maxMb = ui.levelMaxMb,
                    freqLabel = formatFreqLabel(ui.centerFreqHz.getOrElse(index) { 0f }),
                    enabled = ui.equalizerEnabled,
                    onValueChange = { viewModel.onBandLevelChange(index, it) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        BandModeSegmented(
            fiveSelected = settings.uiBandCount == 5,
            enabled = ui.equalizerEnabled,
            onSelectFive = { viewModel.onUiBandCountChange(5) },
            onSelectTen = { viewModel.onUiBandCountChange(10) },
        )

        Spacer(Modifier.height(20.dp))

        val reverbOptions = listOf(
            ReverbOption(0, R.string.reverb_none),
            ReverbOption(1, R.string.reverb_small_room),
            ReverbOption(2, R.string.reverb_medium_room),
            ReverbOption(3, R.string.reverb_large_room),
            ReverbOption(4, R.string.reverb_medium_hall),
            ReverbOption(5, R.string.reverb_large_hall),
            ReverbOption(6, R.string.reverb_plate),
        )
        ReverbDropdown(
            options = reverbOptions,
            selectedId = ui.reverbPreset,
            enabled = ui.equalizerEnabled && ui.reverbSupported,
            onSelect = viewModel::onReverbPreset,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.equalizer_bass_boost),
            color = EqMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        SegmentedBarRow(
            value = ui.bassStrength,
            enabled = ui.equalizerEnabled,
            onValueChange = viewModel::onBassChange,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.equalizer_virtualizer),
            color = EqMuted,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (ui.virtualizerSupported) {
            SegmentedBarRow(
                value = ui.virtualizerStrength,
                enabled = ui.equalizerEnabled,
                onValueChange = viewModel::onVirtualizerChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            Text(
                text = stringResource(R.string.equalizer_virtualizer_unavailable),
                color = EqMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        }
    }
}

@Composable
private fun EqHeader(
    title: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    showSessionWarning: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ArrowBack,
            contentDescription = stringResource(R.string.back_button),
            tint = EqMuted,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(12.dp)
                .size(24.dp),
        )
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EqAccent,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFF404040),
                    uncheckedBorderColor = Color.Transparent,
                ),
            )
            if (showSessionWarning) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun PresetChipRow(
    presets: List<Pair<UserPreferences.EqualizerPreset, Int>>,
    selected: UserPreferences.EqualizerPreset,
    enabled: Boolean,
    onSelect: (UserPreferences.EqualizerPreset) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { (preset, labelRes) ->
            val isOn = when (preset) {
                UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL ->
                    selected == UserPreferences.EqualizerPreset.EQUALIZER_PRESET_NORMAL
                else -> selected == preset
            }
            PresetChip(
                label = stringResource(labelRes),
                selected = isOn,
                enabled = enabled,
                onClick = { onSelect(preset) },
            )
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) EqAccent.copy(alpha = 0.35f) else EqChipInactive
    val fg = if (selected) EqAccent else EqMuted
    Text(
        text = label,
        color = fg,
        fontSize = 13.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun VerticalEqBand(
    valueMb: Int,
    minMb: Int,
    maxMb: Int,
    freqLabel: String,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gainColor = if (valueMb == 0) EqMuted else EqAccent
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatGainMb(valueMb),
            color = gainColor,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        VerticalEqSlider(
            valueMb = valueMb,
            minMb = minMb,
            maxMb = maxMb,
            enabled = enabled,
            onValueChange = onValueChange,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = freqLabel,
            color = EqMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun VerticalEqSlider(
    valueMb: Int,
    minMb: Int,
    maxMb: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val range = (maxMb - minMb).coerceAtLeast(1)
    val density = LocalDensity.current
    val trackW = 4.dp
    val thumbR = 9.dp
    val h = 200.dp

    fun yToMb(y: Float, heightPx: Float): Int {
        val frac = 1f - (y / heightPx).coerceIn(0f, 1f)
        return (minMb + frac * range).roundToInt().coerceIn(minMb, maxMb)
    }

    Box(
        modifier = modifier
            .width(40.dp)
            .height(h)
            .pointerInput(enabled, minMb, maxMb, range) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { onValueChange(yToMb(it.y, size.height.toFloat())) },
                    onDrag = { change, _ ->
                        onValueChange(yToMb(change.position.y, size.height.toFloat()))
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val heightPx = size.height
            val cx = w / 2
            val frac = (valueMb - minMb) / range.toFloat()
            val yThumb = heightPx * (1f - frac)
            val zeroFrac = (0 - minMb) / range.toFloat()
            val yZero = heightPx * (1f - zeroFrac.coerceIn(0f, 1f))

            // Dim track (above thumb)
            drawLine(
                color = EqTrackDim,
                start = Offset(cx, 0f),
                end = Offset(cx, yThumb),
                strokeWidth = with(density) { trackW.toPx() },
            )
            // Accent fill from thumb to bottom
            drawLine(
                color = EqAccent,
                start = Offset(cx, yThumb),
                end = Offset(cx, heightPx),
                strokeWidth = with(density) { 5.dp.toPx() },
            )
            // Zero line
            drawLine(
                color = EqTrackDim.copy(alpha = 0.5f),
                start = Offset(cx - 10f, yZero),
                end = Offset(cx + 10f, yZero),
                strokeWidth = with(density) { 1.dp.toPx() },
            )
            drawCircle(
                color = EqAccent,
                radius = with(density) { thumbR.toPx() },
                center = Offset(cx, yThumb),
            )
        }
    }
}

@Composable
private fun BandModeSegmented(
    fiveSelected: Boolean,
    enabled: Boolean,
    onSelectFive: () -> Unit,
    onSelectTen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(EqSegmentBg)
            .border(1.dp, EqChipInactive, RoundedCornerShape(24.dp)),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (fiveSelected) EqAccent.copy(alpha = 0.4f) else Color.Transparent)
                .clickable(enabled = enabled, onClick = onSelectFive)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.equalizer_bands_5),
                color = if (fiveSelected) EqAccent else EqMuted,
                fontSize = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (!fiveSelected) EqAccent.copy(alpha = 0.4f) else Color.Transparent)
                .clickable(enabled = enabled, onClick = onSelectTen)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.equalizer_bands_10),
                    color = if (!fiveSelected) EqAccent else EqMuted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                )
            }
        }
    }
}

@Composable
private fun ReverbDropdown(
    options: List<ReverbOption>,
    selectedId: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = options.find { it.id == selectedId } ?: options.first()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.equalizer_reverb),
            color = Color.White,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled) { expanded = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(current.labelRes),
                    color = Color.White,
                    fontSize = 15.sp,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            DropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(stringResource(opt.labelRes)) },
                        onClick = {
                            onSelect(opt.id)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedBarRow(
    value: Int,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = 24
    val filled = (value * segments / 1000f).roundToInt().coerceIn(0, segments)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, segments) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x / size.width
                        onValueChange((x * 1000).roundToInt().coerceIn(0, 1000))
                    },
                    onDrag = { change, _ ->
                        val x = change.position.x / size.width
                        onValueChange((x * 1000).roundToInt().coerceIn(0, 1000))
                    },
                )
            },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(segments) { i ->
            val active = i < filled
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (active) EqAccent else EqChipInactive),
            )
        }
    }
}

private fun formatGainMb(mb: Int): String {
    val db = mb / 100.0
    if (abs(db) < 0.05) return "0.0"
    val sign = if (db > 0) "+" else ""
    return "%s%.1f".format(sign, db)
}

private fun formatFreqLabel(hz: Float): String {
    return if (hz >= 1000f) {
        val k = hz / 1000f
        if (k >= 10f) "%.0fkHz".format(k) else "%.1fkHz".format(k)
    } else {
        "%.0fHz".format(hz)
    }
}
