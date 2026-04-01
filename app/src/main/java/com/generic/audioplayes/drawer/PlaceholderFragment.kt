package com.generic.audioplayes.drawer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.ZenPreferenceProvider
import com.generic.audioplayes.ui.theme.ZenTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlaceholderFragment : Fragment() {

    private val args: PlaceholderFragmentArgs by navArgs()

    @Inject lateinit var preferenceProvider: ZenPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val systemUiController = rememberSystemUiController()
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                ZenTheme(themePreference, systemUiController) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopBarWithBackArrow(
                                onBackArrowPressed = { findNavController().navigateUp() },
                                title = args.screenTitle,
                                actions = {},
                            )
                        },
                    ) { padding ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = args.screenTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
