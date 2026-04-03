package com.generic.audioplayes.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.getValue

@AndroidEntryPoint
class WidgetsFragment : Fragment() {

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

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
                AudioPlayerTheme(themePreference, systemUiController) {
                    WidgetsScreen(
                        onBack = { findNavController().navigateUp() },
                        onRequestPinWidget = { useSmall -> requestPinWidget(useSmall) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    private fun requestPinWidget(useSmallProvider: Boolean) {
        val ctx = requireContext()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Toast.makeText(ctx, R.string.widgets_pin_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        val awm = ctx.getSystemService(AppWidgetManager::class.java) ?: return
        if (!awm.isRequestPinAppWidgetSupported) {
            Toast.makeText(ctx, R.string.widgets_pin_launcher_unsupported, Toast.LENGTH_LONG).show()
            return
        }
        val clazz = if (useSmallProvider) {
            PlayerWidgetSmallProvider::class.java
        } else {
            PlayerWidgetProvider::class.java
        }
        val component = ComponentName(ctx, clazz)
        val ok = awm.requestPinAppWidget(component, null, null)
        if (!ok) {
            Toast.makeText(ctx, R.string.widgets_pin_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
