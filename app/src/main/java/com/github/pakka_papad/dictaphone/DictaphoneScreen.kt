package com.github.pakka_papad.dictaphone

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.pakka_papad.R
import com.github.pakka_papad.toMS

private fun formatRecordingElapsed(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictaphoneScreen(
    viewModel: DictaphoneViewModel,
    modifier: Modifier = Modifier,
    microphoneGranted: Boolean,
    onRequestMicrophonePermission: () -> Unit,
) {
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val elapsedMs by viewModel.recordingElapsedMs.collectAsStateWithLifecycle()
    val playingPath by viewModel.currentlyPlayingPath.collectAsStateWithLifecycle()
    val isPlayerPlaying by viewModel.isPlayerPlaying.collectAsStateWithLifecycle()
    val renameTarget by viewModel.renameTarget.collectAsStateWithLifecycle()
    val messageRes by viewModel.userMessageRes.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(messageRes) {
        val id = messageRes ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(context.getString(id))
        viewModel.consumeMessage()
    }

    var renameText by remember { mutableStateOf("") }
    LaunchedEffect(renameTarget) {
        renameText = renameTarget?.nameWithoutExtension.orEmpty()
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRename,
            title = { Text(stringResource(R.string.dictaphone_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.dictaphone_rename_hint)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRename(renameText) },
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRename) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!microphoneGranted) {
                Text(
                    text = stringResource(R.string.dictaphone_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onRequestMicrophonePermission) {
                    Text(stringResource(R.string.dictaphone_grant_permission))
                }
            }

            Text(
                text = stringResource(R.string.dictaphone_folder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = if (isRecording) formatRecordingElapsed(elapsedMs) else "0:00",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Button(
                    onClick = { viewModel.startRecording() },
                    enabled = microphoneGranted && !isRecording,
                ) {
                    Text(stringResource(R.string.dictaphone_record))
                }
                Button(
                    onClick = { viewModel.stopRecording() },
                    enabled = isRecording,
                ) {
                    Text(stringResource(R.string.dictaphone_stop))
                }
            }

            Text(
                text = stringResource(R.string.dictaphone_recordings_list),
                style = MaterialTheme.typography.titleMedium,
            )

            if (recordings.isEmpty()) {
                Text(
                    text = stringResource(R.string.dictaphone_no_recordings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(recordings, key = { it.file.absolutePath }) { item ->
                        RecordingRow(
                            item = item,
                            isPlaying = playingPath == item.file.absolutePath && isPlayerPlaying,
                            onPlayClick = { viewModel.playRecording(item.file) },
                            onRenameClick = { viewModel.requestRename(item.file) },
                            onDeleteClick = { viewModel.deleteRecording(item.file) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    item: RecordingListItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                )
                val dur = item.durationMs
                if (dur != null) {
                    Text(
                        text = dur.toMS(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onPlayClick) {
                Text(
                    if (isPlaying) stringResource(R.string.dictaphone_pause)
                    else stringResource(R.string.dictaphone_play),
                )
            }
            TextButton(onClick = onRenameClick) {
                Text(stringResource(R.string.dictaphone_rename))
            }
            TextButton(onClick = onDeleteClick) {
                Text(stringResource(R.string.dictaphone_delete))
            }
        }
    }
}
