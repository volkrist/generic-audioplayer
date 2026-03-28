package com.github.pakka_papad.sleeptimer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.github.pakka_papad.components.TopBarWithBackArrow
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.ui.theme.ZenTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.material3.Scaffold
import androidx.compose.ui.res.stringResource
import com.github.pakka_papad.R
import javax.inject.Inject

@AndroidEntryPoint
class SleepTimerFragment : Fragment() {

    private val viewModel: SleepTimerViewModel by viewModels()

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
                                title = stringResource(R.string.sleep_timer),
                                actions = {},
                            )
                        },
                    ) { padding ->
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            SleepTimerScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(padding),
                            )
                        }
                    }
                }
            }
        }
    }
}
