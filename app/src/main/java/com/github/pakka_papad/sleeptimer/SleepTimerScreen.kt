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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R

@Composable
fun SleepTimerScreen(
    viewModel: SleepTimerViewModel,
    modifier: Modifier = Modifier,
) {
    val isRunning by viewModel.sleepTimerService.isRunning.collectAsStateWithLifecycle()
    val timeLeft by viewModel.sleepTimerService.timeLeft.collectAsStateWithLifecycle()
    val stopAfterTrack by viewModel.sleepTimerService.isStopAfterCurrentTrack.collectAsStateWithLifecycle()

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
        val presets = listOf(10, 15, 30, 45, 60)
        for (m in presets) {
            OutlinedButton(
                onClick = { viewModel.startMinutes(m) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sleep_timer_minutes_format, m))
            }
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
