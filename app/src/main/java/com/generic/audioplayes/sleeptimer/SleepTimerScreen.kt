package com.generic.audioplayes.sleeptimer

import android.annotation.SuppressLint
import android.widget.NumberPicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R

/** Bottom sheet: slightly translucent; a bit more opaque than before for readability. */
private val SheetBg = Color(0xFF15151C).copy(alpha = 0.70f)
/** Dim over the previous screen — unchanged so the background visibility stays the same. */
private val DimScrim = Color.Black.copy(alpha = 0.42f)
private val PresetBg = Color(0xFF2C2C2C).copy(alpha = 0.88f)
private val AccentOrange = Color(0xFFD48824)
private val WheelBoxBg = Color(0xFF2A2A32).copy(alpha = 0.82f)
@Composable
fun SleepTimerScreen(
    viewModel: SleepTimerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRunning by viewModel.sleepTimerService.isRunning.collectAsStateWithLifecycle()
    val timeLeft by viewModel.sleepTimerService.timeLeft.collectAsStateWithLifecycle()
    val stopAfterTrack by viewModel.sleepTimerService.isStopAfterCurrentTrack.collectAsStateWithLifecycle()

    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }

    val totalSeconds = hours * 3600 + minutes * 60 + seconds

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DimScrim)
            .clickable(onClick = onDismiss),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = SheetBg,
            shadowElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                if (isRunning && (timeLeft > 0 || stopAfterTrack)) {
                    SleepTimerActiveBanner(
                        timeLeft = timeLeft,
                        stopAfterTrack = stopAfterTrack,
                        onCancelTimer = { viewModel.cancelTimer() },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(R.string.sleep_timer_stop_music_after_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WheelColumn(
                        value = hours,
                        range = 0..23,
                        onValueChange = { hours = it },
                    )
                    Text(
                        text = ":",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    WheelColumn(
                        value = minutes,
                        range = 0..59,
                        onValueChange = { minutes = it },
                    )
                    Text(
                        text = ":",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                    WheelColumn(
                        value = seconds,
                        range = 0..59,
                        onValueChange = { seconds = it },
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    listOf(15, 30, 45, 60).forEach { presetMin ->
                        PresetChip(
                            minutes = presetMin,
                            onClick = {
                                if (presetMin == 60) {
                                    hours = 1
                                    minutes = 0
                                    seconds = 0
                                } else {
                                    hours = 0
                                    minutes = presetMin
                                    seconds = 0
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .clickable(onClick = onDismiss),
                        color = PresetBg,
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = stringResource(R.string.sleep_timer_sheet_cancel),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .clickable(
                                enabled = totalSeconds > 0,
                                onClick = {
                                    viewModel.startCustomTotalSeconds(totalSeconds)
                                    onDismiss()
                                },
                            ),
                        color = if (totalSeconds > 0) AccentOrange else AccentOrange.copy(alpha = 0.4f),
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = stringResource(R.string.sleep_timer_sheet_start),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SleepTimerActiveBanner(
    timeLeft: Int,
    stopAfterTrack: Boolean,
    onCancelTimer: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2520))
            .padding(12.dp),
    ) {
        if (stopAfterTrack) {
            Text(
                text = stringResource(R.string.sleep_timer_after_current_track_active),
                color = AccentOrange,
                fontSize = 14.sp,
            )
        } else {
            val h = timeLeft / 3600
            val m = (timeLeft % 3600) / 60
            val s = timeLeft % 60
            val label = "%02d:%02d:%02d".format(h, m, s)
            Text(
                text = stringResource(R.string.stopping_in, label),
                color = Color.White,
                fontSize = 15.sp,
            )
        }
        TextButton(onClick = onCancelTimer) {
            Text(
                text = stringResource(R.string.cancel_sleep_timer),
                color = AccentOrange,
            )
        }
    }
}

@Composable
private fun PresetChip(
    minutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PresetBg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = stringResource(R.string.sleep_timer_preset_minutes_short, minutes),
            color = Color.White,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@SuppressLint("SetTextI18n")
@Composable
private fun WheelColumn(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = range.last - range.first + 1
    val labels = remember(range) {
        Array(count) { i -> String.format("%02d", range.first + i) }
    }
    val textColor = Color.White.toArgb()

    AndroidView(
        factory = { ctx ->
            NumberPicker(ctx).apply {
                minValue = 0
                maxValue = count - 1
                displayedValues = labels
                wrapSelectorWheel = false
                setOnValueChangedListener { _, _, newIdx ->
                    onValueChange(range.first + newIdx)
                }
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                try {
                    val field = NumberPicker::class.java.getDeclaredField("mInputText")
                    field.isAccessible = true
                    val input = field.get(this) as? android.widget.EditText
                    input?.setTextColor(textColor)
                } catch (_: Exception) {
                }
            }
        },
        update = { picker ->
            val idx = (value - range.first).coerceIn(0, count - 1)
            if (picker.value != idx) {
                picker.value = idx
            }
        },
        modifier = modifier
            .width(76.dp)
            .height(204.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WheelBoxBg),
    )
}
