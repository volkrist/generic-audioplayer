package com.github.pakka_papad.sleeptimer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R
import kotlin.math.roundToInt

@Composable
fun SleepTimerScreen(
    viewModel: SleepTimerViewModel,
    modifier: Modifier = Modifier,
) {
    val isRunning by viewModel.sleepTimerService.isRunning.collectAsStateWithLifecycle()
    val timeLeft by viewModel.sleepTimerService.timeLeft.collectAsStateWithLifecycle()
    val stopAfterTrack by viewModel.sleepTimerService.isStopAfterCurrentTrack.collectAsStateWithLifecycle()

    var pickerHours by remember { mutableStateOf(0) }
    var pickerMinutes by remember { mutableStateOf(0) }
    var pickerSeconds by remember { mutableStateOf(0) }
    var customMinutesSlider by remember { mutableStateOf(30f) }

    val timeLabel by remember(timeLeft) {
        derivedStateOf {
            val mins = timeLeft / 60
            val secs = timeLeft % 60
            val m = if (mins < 10) "0$mins" else mins.toString()
            val s = if (secs < 10) "0$secs" else secs.toString()
            "$m:$s"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.sleep_timer_status_hint),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            stopAfterTrack && isRunning -> {
                Text(
                    text = stringResource(R.string.sleep_timer_after_current_track_active),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
            isRunning && timeLeft > 0 -> {
                Text(
                    text = stringResource(R.string.stopping_in, timeLabel),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.padding(8.dp))
        Text(
            text = stringResource(R.string.sleep_timer_custom_duration),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                R.string.sleep_timer_picker_value,
                pickerHours,
                pickerMinutes,
                pickerSeconds,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = pickerHours.toFloat(),
            onValueChange = { pickerHours = it.roundToInt().coerceIn(0, 10) },
            valueRange = 0f..10f,
            steps = 9,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.sleep_timer_picker_hours), style = MaterialTheme.typography.labelSmall)
        Slider(
            value = pickerMinutes.toFloat(),
            onValueChange = { pickerMinutes = it.roundToInt().coerceIn(0, 59) },
            valueRange = 0f..59f,
            steps = 58,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.sleep_timer_picker_minutes), style = MaterialTheme.typography.labelSmall)
        Slider(
            value = pickerSeconds.toFloat(),
            onValueChange = { pickerSeconds = it.roundToInt().coerceIn(0, 59) },
            valueRange = 0f..59f,
            steps = 58,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.sleep_timer_picker_seconds), style = MaterialTheme.typography.labelSmall)
        OutlinedButton(
            onClick = {
                val total = pickerHours * 3600 + pickerMinutes * 60 + pickerSeconds
                if (total > 0) viewModel.startCustomTotalSeconds(total)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pickerHours + pickerMinutes + pickerSeconds > 0,
        ) {
            Text(stringResource(R.string.sleep_timer_start_picker))
        }
        Text(
            text = stringResource(R.string.sleep_timer_quick_presets),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val presets = listOf(15, 30, 45, 60)
        for (m in presets) {
            OutlinedButton(
                onClick = { viewModel.startMinutes(m) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sleep_timer_minutes_format, m))
            }
        }
        Text(
            text = stringResource(R.string.sleep_timer_custom_minutes_slider),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = customMinutesSlider,
            onValueChange = { customMinutesSlider = it },
            valueRange = 1f..180f,
            steps = 178,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { viewModel.startMinutes(customMinutesSlider.roundToInt()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    R.string.sleep_timer_start_minutes,
                    customMinutesSlider.roundToInt(),
                ),
            )
        }
        Button(
            onClick = { viewModel.startStopAfterCurrentTrack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text(stringResource(R.string.sleep_timer_stop_after_current_track))
        }
        Button(
            onClick = { viewModel.cancelTimer() },
            modifier = Modifier.fillMaxWidth(),
            enabled = isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text(stringResource(R.string.cancel_sleep_timer))
        }
    }
}
