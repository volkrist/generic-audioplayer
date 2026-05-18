package com.generic.audioplayes.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import com.generic.audioplayes.R
import com.generic.audioplayes.components.ScreenErrorState
import com.generic.audioplayes.components.ScreenLoadingState
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.ui.theme.HomeLibraryTokens

@Composable
fun TagEditorScreen(
    state: TagEditorUiState,
    onBack: () -> Unit,
    onDraftChange: (TagEditDraft) -> Unit,
    onPickCoverClick: () -> Unit,
    onClearCover: () -> Unit,
    onSave: () -> Unit,
) {
    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HomeLibraryTokens.libraryShellGradient),
    ) {
        TopBarWithBackArrow(
            onBackArrowPressed = onBack,
            title = stringResource(R.string.tag_editor_title),
            actions = {},
            backgroundColor = Color.Transparent,
        )
        when {
            state.loading -> {
                ScreenLoadingState()
            }
            state.loadError != null -> {
                ScreenErrorState(message = state.loadError)
            }
            state.saving -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.tag_editor_saving))
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    CoverArtBlock(
                        pickedBytes = state.pickedCoverBytes,
                        embeddedBytes = state.draft.embeddedCoverBytes,
                        clearCover = state.clearCover,
                        onPickCoverClick = onPickCoverClick,
                        onClearCover = onClearCover,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    state.errorMsg?.let {
                        Text(
                            text = stringResource(R.string.tag_editor_error, it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    LabeledField(
                        label = stringResource(R.string.tag_editor_field_title),
                        value = state.draft.title,
                        onChange = { onDraftChange(state.draft.copy(title = it)) },
                    )
                    LabeledField(
                        label = stringResource(R.string.tag_editor_field_album),
                        value = state.draft.album,
                        onChange = { onDraftChange(state.draft.copy(album = it)) },
                    )
                    LabeledField(
                        label = stringResource(R.string.tag_editor_field_artist),
                        value = state.draft.artist,
                        onChange = { onDraftChange(state.draft.copy(artist = it)) },
                    )
                    LabeledField(
                        label = stringResource(R.string.tag_editor_field_genre),
                        value = state.draft.genre,
                        onChange = { onDraftChange(state.draft.copy(genre = it)) },
                    )
                    LabeledField(
                        label = stringResource(R.string.tag_editor_field_track_number),
                        value = state.draft.trackNumber,
                        onChange = { newValue ->
                            // Track number is a positive integer; keep digits only so the file
                            // can round-trip "2", "02", etc. without storing garbage tags.
                            val digitsOnly = newValue.filter { it.isDigit() }.take(4)
                            onDraftChange(state.draft.copy(trackNumber = digitsOnly))
                        },
                        keyboardType = KeyboardType.Number,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.tag_editor_save))
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun CoverArtBlock(
    pickedBytes: ByteArray?,
    embeddedBytes: ByteArray?,
    clearCover: Boolean,
    onPickCoverClick: () -> Unit,
    onClearCover: () -> Unit,
) {
    val showBytes: ByteArray? = when {
        clearCover && pickedBytes == null -> null
        pickedBytes != null -> pickedBytes
        else -> embeddedBytes
    }
    val placeholder: Painter = painterResource(R.drawable.ic_baseline_music_note_40)
    val coverShape = RoundedCornerShape(28.dp)
    val coverBg = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
    val onCoverBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(200.dp)
                .clip(coverShape),
            shape = coverShape,
            color = coverBg,
            tonalElevation = 2.dp,
        ) {
            if (showBytes != null && showBytes.isNotEmpty()) {
                AsyncImage(
                    model = showBytes,
                    contentDescription = stringResource(R.string.song_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(coverBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = placeholder,
                        contentDescription = null,
                        tint = onCoverBg,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onPickCoverClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = stringResource(R.string.tag_editor_pick_cover),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
        }
        if ((showBytes != null && showBytes.isNotEmpty()) || pickedBytes != null) {
            androidx.compose.material3.TextButton(onClick = onClearCover) {
                Text(stringResource(R.string.tag_editor_remove_cover))
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Start,
            ),
            color = labelColor,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        TextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}
