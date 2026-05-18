package com.generic.audioplayes.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R

@Composable
fun ScreenLoadingState(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
fun ScreenEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    ScreenPlaceholderState(
        message = message,
        modifier = modifier,
        paddingValues = paddingValues,
        contentAlpha = 0.45f,
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun ScreenErrorState(
    message: String,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(),
) {
    ScreenPlaceholderState(
        message = message,
        modifier = modifier,
        paddingValues = paddingValues,
        contentAlpha = 0.55f,
        textColor = MaterialTheme.colorScheme.error,
    )
}

@Composable
private fun ScreenPlaceholderState(
    message: String,
    modifier: Modifier,
    paddingValues: PaddingValues,
    contentAlpha: Float,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .sizeIn(minWidth = 200.dp, minHeight = 160.dp)
            .alpha(contentAlpha),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_baseline_sentiment_very_dissatisfied_40),
            contentDescription = stringResource(R.string.sad_face_image),
            modifier = Modifier.size(56.dp),
            tint = textColor,
        )
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = textColor,
        )
    }
}
