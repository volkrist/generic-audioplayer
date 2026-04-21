package com.generic.audioplayes.dictaphone

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generic.audioplayes.R
import com.generic.audioplayes.toMS

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
            containerColor = Color(0xFF1E1B4B),
            iconContentColor = Color.White,
            titleContentColor = Color.White,
            textContentColor = Color.White,
            title = { Text(stringResource(R.string.dictaphone_rename), color = Color.White) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(R.string.dictaphone_rename_hint),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF22D3EE),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = Color(0xFF22D3EE),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmRename(renameText) },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF22D3EE)),
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = viewModel::dismissRename,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Colours forced to white / cyan / off‑white across the whole screen: the
            // DictaphoneFragment sits on top of the dark library gradient, and the Material You
            // defaults (onSurface / onSurfaceVariant) frequently resolve to dark grey which
            // became unreadable — the "Recording" button and "0:00" timer were basically
            // invisible before this pass.
            if (!microphoneGranted) {
                Text(
                    text = stringResource(R.string.dictaphone_permission_required),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE5E7FF),
                )
                Button(
                    onClick = onRequestMicrophonePermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22D3EE),
                        contentColor = Color(0xFF0B1028),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.dictaphone_grant_permission),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = stringResource(R.string.dictaphone_folder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB4B8D6),
            )

            Text(
                text = if (isRecording) formatRecordingElapsed(elapsedMs) else "0:00",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                Button(
                    onClick = { viewModel.startRecording() },
                    enabled = microphoneGranted && !isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFEF4444).copy(alpha = 0.35f),
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.dictaphone_record),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = { viewModel.stopRecording() },
                    enabled = isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF22D3EE),
                        contentColor = Color(0xFF0B1028),
                        disabledContainerColor = Color(0xFF22D3EE).copy(alpha = 0.25f),
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.dictaphone_stop),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = stringResource(R.string.dictaphone_recordings_list),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )

            if (recordings.isEmpty()) {
                Text(
                    text = stringResource(R.string.dictaphone_no_recordings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB4B8D6),
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
        colors = CardDefaults.cardColors(
            // Translucent violet chip on the library gradient: clearly visible, never clashes
            // with the background, and keeps the body text readable in pure white.
            containerColor = Color(0xFF1E1B4B).copy(alpha = 0.65f),
            contentColor = Color.White,
        ),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 2,
                )
                val dur = item.durationMs
                if (dur != null) {
                    Text(
                        text = dur.toMS(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB4B8D6),
                    )
                }
            }
            TextButton(
                onClick = onPlayClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF22D3EE)),
            ) {
                Text(
                    if (isPlaying) stringResource(R.string.dictaphone_pause)
                    else stringResource(R.string.dictaphone_play),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(
                onClick = onRenameClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Text(stringResource(R.string.dictaphone_rename))
            }
            TextButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
            ) {
                Text(stringResource(R.string.dictaphone_delete))
            }
        }
    }
}
