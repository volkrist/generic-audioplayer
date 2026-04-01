package com.generic.audioplayes.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.components.BlockingProgressIndicator
import com.generic.audioplayes.components.Snackbar
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.ZenPreferenceProvider
import com.generic.audioplayes.ui.theme.ZenTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BackupRestoreFragment : Fragment() {

    private val viewModel: BackupRestoreViewModel by viewModels()

    @Inject
    lateinit var preferenceProvider: ZenPreferenceProvider

    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    viewModel.performExport(out)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    private val openBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    viewModel.performImport(input)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val systemUiController = rememberSystemUiController()
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                val lastBackup by preferenceProvider.lastBackupExportEpochMs.collectAsStateWithLifecycle()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState) {
                    when (val s = uiState) {
                        is BackupRestoreUiState.Success -> {
                            snackbarHostState.showSnackbar(s.message)
                            viewModel.consumeMessage()
                        }
                        is BackupRestoreUiState.Error -> {
                            snackbarHostState.showSnackbar(s.message)
                            viewModel.consumeMessage()
                        }
                        else -> {}
                    }
                }

                ZenTheme(themePreference, systemUiController) {
                    Scaffold(
                        topBar = {
                            TopBarWithBackArrow(
                                onBackArrowPressed = { navController.navigateUp() },
                                title = stringResource(R.string.settings_backup_restore_title),
                                actions = {},
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = { Snackbar(it) },
                            )
                        },
                    ) { padding ->
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                BackupRestoreScreen(
                                    lastBackupEpochMs = lastBackup,
                                    onExportClick = {
                                        createBackupLauncher.launch("zen_backup.json")
                                    },
                                    onImportClick = {
                                        openBackupLauncher.launch(
                                            arrayOf("application/json", "application/*", "*/*"),
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (uiState is BackupRestoreUiState.Loading) {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        BlockingProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
