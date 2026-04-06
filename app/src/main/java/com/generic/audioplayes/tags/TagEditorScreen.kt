package com.generic.audioplayes.tags

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import com.generic.audioplayes.components.TopBarWithBackArrow

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

    Column(modifier = Modifier.fillMaxSize()) {
        TopBarWithBackArrow(
            onBackArrowPressed = onBack,
            title = stringResource(R.string.tag_editor_title),
            actions = {},
        )
        when {
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = state.fileLabel,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    state.errorMsg?.let {
                        Text(
                            text = stringResource(R.string.tag_editor_error, it),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Field(stringResource(R.string.tag_editor_field_title), state.draft.title) {
                        onDraftChange(state.draft.copy(title = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_artist), state.draft.artist) {
                        onDraftChange(state.draft.copy(artist = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_album), state.draft.album) {
                        onDraftChange(state.draft.copy(album = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_album_artist), state.draft.albumArtist) {
                        onDraftChange(state.draft.copy(albumArtist = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_year), state.draft.year) {
                        onDraftChange(state.draft.copy(year = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_genre), state.draft.genre) {
                        onDraftChange(state.draft.copy(genre = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_lyricist), state.draft.lyricist) {
                        onDraftChange(state.draft.copy(lyricist = it))
                    }
                    Field(stringResource(R.string.tag_editor_field_comment), state.draft.comment, singleLine = false) {
                        onDraftChange(state.draft.copy(comment = it))
                    }
                    TextButton(onClick = onPickCoverClick) {
                        Text(stringResource(R.string.tag_editor_pick_cover))
                    }
                    TextButton(onClick = onClearCover) {
                        Text(stringResource(R.string.tag_editor_remove_cover))
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.tag_editor_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
    )
}
