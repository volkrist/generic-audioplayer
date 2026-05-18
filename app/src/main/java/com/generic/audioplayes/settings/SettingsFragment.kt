package com.generic.audioplayes.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.generic.audioplayes.ui.scaffoldContentPaddingWithSystemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.components.Snackbar
import com.generic.audioplayes.components.TopBarWithBackArrow
import com.generic.audioplayes.data.AudioPlayerCrashReporter
import com.generic.audioplayes.BuildConfig
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.ui.theme.AudioPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var navController: NavController

    @Inject lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    @Inject lateinit var crashReporter: AudioPlayerCrashReporter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                val showOnLockScreen by preferenceProvider.showOnLockScreen.collectAsStateWithLifecycle()
                val pauseOnHeadset by preferenceProvider.pauseOnHeadsetDisconnect.collectAsStateWithLifecycle()
                val crossfadeEnabled by preferenceProvider.crossfadeEnabled.collectAsStateWithLifecycle()
                val gaplessEnabled by preferenceProvider.gaplessPlaybackEnabled.collectAsStateWithLifecycle()
                val keepScreenOn by preferenceProvider.keepScreenOn.collectAsStateWithLifecycle()
                val lastBackupEpochMs by preferenceProvider.lastBackupExportEpochMs.collectAsStateWithLifecycle()

                val settingsTopBarBrush = remember {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1028),
                            Color(0xFF12102A),
                        ),
                    )
                }

                val privacyPolicyUrl = stringResource(R.string.settings_privacy_policy_url)
                val faqUrl = stringResource(R.string.settings_faq_url)
                val termsUrl = stringResource(R.string.settings_terms_url)
                val appVersionDisplay = stringResource(R.string.app_version_name)
                val stage4BuildMarker = if (BuildConfig.DEBUG) {
                    "Debug build: Stage 4 fix active v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                } else {
                    null
                }

                val onFaqClicked = remember(faqUrl) {
                    {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(faqUrl)))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                        }
                    }
                }
                val onTermsClicked = remember(termsUrl) {
                    {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                        }
                    }
                }
                val onFeedbackClicked = remember {
                    {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("audioplayer.app@outlook.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "AudioPlayer | Feedback")
                        }
                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                        }
                    }
                }
                val onRateUsClicked = remember {
                    {
                        val ctx = requireContext()
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("market://details?id=${ctx.packageName}"),
                                ),
                            )
                        } catch (e: Exception) {
                            try {
                                startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store/apps/details?id=${ctx.packageName}"),
                                    ),
                                )
                            } catch (e2: Exception) {
                                crashReporter.logException(e2)
                            }
                        }
                    }
                }
                val onPrivacyPolicyClicked = remember(privacyPolicyUrl) {
                    {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl)))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                        }
                    }
                }
                val onLanguageClicked = remember {
                    {
                        try {
                            startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                        }
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }

                val message by viewModel.message.collectAsStateWithLifecycle()
                LaunchedEffect(key1 = message) {
                    if (message.isEmpty()) return@LaunchedEffect
                    snackbarHostState.showSnackbar(message)
                }

                val onHiddenMusicClicked = remember {
                    {
                        navController.navigate(R.id.action_settingsFragment_to_restoreFragment)
                    }
                }
                val onBackupRestoreClicked = remember {
                    {
                        navController.navigate(R.id.action_settingsFragment_to_backupRestoreFragment)
                    }
                }

                AudioPlayerTheme(themePreference) {
                    Scaffold(
                        containerColor = Color.Transparent,
                        topBar = {
                            TopBarWithBackArrow(
                                onBackArrowPressed = navController::popBackStack,
                                title = stringResource(R.string.settings),
                                actions = { },
                                onBackgroundColor = Color.White,
                                centerTitle = false,
                                showBottomDivider = false,
                                backgroundBrush = settingsTopBarBrush,
                            )
                        },
                        content = { paddingValues ->
                            SettingsList(
                                paddingValues = scaffoldContentPaddingWithSystemBars(paddingValues),
                                lastBackupEpochMs = lastBackupEpochMs,
                                crossfadeEnabled = crossfadeEnabled,
                                onCrossfadeChanged = preferenceProvider::updateCrossfadeEnabled,
                                gaplessPlaybackEnabled = gaplessEnabled,
                                onGaplessChanged = preferenceProvider::updateGaplessPlaybackEnabled,
                                keepScreenOn = keepScreenOn,
                                onKeepScreenOnChanged = preferenceProvider::updateKeepScreenOn,
                                onHiddenMusicClicked = onHiddenMusicClicked,
                                onBackupRestoreClicked = onBackupRestoreClicked,
                                showOnLockScreen = showOnLockScreen,
                                onShowOnLockScreenChanged = preferenceProvider::updateShowOnLockScreen,
                                pauseOnHeadsetDisconnect = pauseOnHeadset,
                                onPauseOnHeadsetChanged = preferenceProvider::updatePauseOnHeadsetDisconnect,
                                onLanguageClicked = onLanguageClicked,
                                onFaqClicked = onFaqClicked,
                                onFeedbackClicked = onFeedbackClicked,
                                onRateUsClicked = onRateUsClicked,
                                onPremiumClicked = { viewModel.showPremiumPlaceholder() },
                                onPrivacyPolicyClicked = onPrivacyPolicyClicked,
                                onTermsClicked = onTermsClicked,
                                appVersionDisplay = appVersionDisplay,
                                stage4BuildMarker = stage4BuildMarker,
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = {
                                    Snackbar(it)
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
