package com.generic.audioplayes.volume

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import kotlin.math.roundToInt

@Composable
fun VolumeBoosterScreen(
    viewModel: VolumeBoosterViewModel,
    modifier: Modifier = Modifier,
) {
    val percent by viewModel.percent.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // This screen sits on the dark library gradient — force white / off‑white text so it
        // stays legible on any theme. Earlier revision relied on `onSurface` / `onSurfaceVariant`
        // which resolve to near‑black on some Material You palettes, making "200%" and the
        // hint invisible against the violet background.
        Text(
            text = stringResource(R.string.volume_booster_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFE5E7FF),
        )
        Text(
            text = stringResource(R.string.volume_booster_current, percent),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { viewModel.setPercent(it.roundToInt()) },
            valueRange = 100f..200f,
            steps = 99,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF22D3EE),
                activeTrackColor = Color(0xFFA855F7),
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.volume_booster_reset),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
