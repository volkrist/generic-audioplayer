package com.github.pakka_papad.volume

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R
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
        Text(
            text = stringResource(R.string.volume_booster_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.volume_booster_current, percent),
            style = MaterialTheme.typography.headlineSmall,
        )
        Slider(
            value = percent.toFloat(),
            onValueChange = { viewModel.setPercent(it.roundToInt()) },
            valueRange = 100f..200f,
            steps = 99,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.reset() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.volume_booster_reset))
        }
    }
}
