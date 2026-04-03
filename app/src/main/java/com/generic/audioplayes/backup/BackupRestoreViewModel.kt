package com.generic.audioplayes.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import com.generic.audioplayes.R
import com.generic.audioplayes.data.backup.BackupImportResult
import com.generic.audioplayes.data.backup.AudioPlayerBackupService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed interface BackupRestoreUiState {
    object Idle : BackupRestoreUiState
    object Loading : BackupRestoreUiState
    data class Success(val message: String) : BackupRestoreUiState
    data class Error(val message: String) : BackupRestoreUiState
}

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupService: AudioPlayerBackupService,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BackupRestoreUiState>(BackupRestoreUiState.Idle)
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    fun consumeMessage() {
        _uiState.update { BackupRestoreUiState.Idle }
    }

    /**
     * Caller must open [out] (e.g. from a document URI) and close it after this returns.
     * Runs on IO; must be called from a coroutine (e.g. [viewModelScope] or [lifecycleScope]).
     */
    suspend fun performExport(out: OutputStream) {
        _uiState.value = BackupRestoreUiState.Loading
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val json = backupService.exportToJsonString()
                out.write(json.toByteArray(Charsets.UTF_8))
                backupService.markExportSuccess()
            }
        }
        _uiState.value = result.fold(
            onSuccess = {
                BackupRestoreUiState.Success(context.getString(R.string.backup_export_success))
            },
            onFailure = { e ->
                BackupRestoreUiState.Error(e.message ?: context.getString(R.string.backup_error_generic))
            },
        )
    }

    suspend fun performImport(input: InputStream) {
        _uiState.value = BackupRestoreUiState.Loading
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = input.readBytes()
                backupService.importFromJsonBytes(bytes).getOrThrow()
            }
        }
        _uiState.value = result.fold(
            onSuccess = { stats ->
                BackupRestoreUiState.Success(formatImportSuccess(stats))
            },
            onFailure = { e ->
                BackupRestoreUiState.Error(e.message ?: context.getString(R.string.backup_error_generic))
            },
        )
    }

    private fun formatImportSuccess(stats: BackupImportResult): String {
        return context.getString(
            R.string.backup_import_success_detail,
            if (stats.preferencesApplied) {
                context.getString(R.string.backup_yes)
            } else {
                context.getString(R.string.backup_no)
            },
            stats.playlistsImported,
            stats.tracksAdded,
            stats.tracksSkippedMissing,
        )
    }
}
