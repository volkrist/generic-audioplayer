package com.generic.audioplayes.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.generic.audioplayes.R
import java.text.DateFormat
import java.util.Date

@Composable
fun BackupRestoreScreen(
    lastBackupEpochMs: Long,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastLabel = if (lastBackupEpochMs > 0L) {
        DateFormat.getDateTimeInstance().format(Date(lastBackupEpochMs))
    } else {
        stringResource(R.string.backup_never_exported)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.backup_restore_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.backup_last_export, lastLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(4.dp))
        Button(
            onClick = onExportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_export_json))
        }
        Button(
            onClick = onImportClick,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.backup_import_json))
        }
        Text(
            text = stringResource(R.string.backup_queue_not_included),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
