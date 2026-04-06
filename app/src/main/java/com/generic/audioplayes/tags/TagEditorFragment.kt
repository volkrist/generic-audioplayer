package com.generic.audioplayes.tags

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@AndroidEntryPoint
class TagEditorFragment : Fragment() {

    private val viewModel: TagEditorViewModel by viewModels()

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                val systemUiController = rememberSystemUiController()
                val state by viewModel.ui.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val pickCover = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia(),
                ) { uri: Uri? ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    runCatching {
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                            ByteArrayOutputStream().apply { input.copyTo(this) }.toByteArray()
                        } ?: return@runCatching
                        viewModel.setPickedCover(bytes, mime)
                    }
                }
                AudioPlayerTheme(themePreference, systemUiController) {
                    TagEditorScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onDraftChange = { newDraft ->
                            viewModel.updateDraft { _ -> newDraft }
                        },
                        onPickCoverClick = {
                            pickCover.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onClearCover = { viewModel.clearCover() },
                        onSave = { viewModel.save() },
                    )
                }
            }
        }
    }
}
