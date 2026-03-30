package com.github.pakka_papad.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.github.pakka_papad.R
import com.github.pakka_papad.components.Snackbar
import com.github.pakka_papad.components.TopBarWithBackArrow
import com.github.pakka_papad.data.ZenCrashReporter
import com.github.pakka_papad.data.ZenPreferenceProvider
import com.github.pakka_papad.ui.theme.ZenTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel :SettingsViewModel by viewModels()

    private lateinit var navController: NavController

    @Inject lateinit var preferenceProvider: ZenPreferenceProvider

    @Inject lateinit var appUpdateManager: AppUpdateManager

    @Inject lateinit var crashReporter: ZenCrashReporter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        navController = findNavController()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themePreference by preferenceProvider.theme.collectAsStateWithLifecycle()
                val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
                val isCrashlyticsDisabled by preferenceProvider.isCrashlyticsDisabled.collectAsStateWithLifecycle()

                val tabsSelection by viewModel.tabsSelection.collectAsStateWithLifecycle()

                val isAppUpdateAvailable by remember { derivedStateOf {
                    viewModel.appUpdateInfo.value != null
                } }

                val crossfadeEnabled by preferenceProvider.crossfadeEnabled.collectAsStateWithLifecycle()
                val gaplessEnabled by preferenceProvider.gaplessPlaybackEnabled.collectAsStateWithLifecycle()
                val keepScreenOn by preferenceProvider.keepScreenOn.collectAsStateWithLifecycle()
                val showOnLockScreen by preferenceProvider.showOnLockScreen.collectAsStateWithLifecycle()
                val pauseOnHeadset by preferenceProvider.pauseOnHeadsetDisconnect.collectAsStateWithLifecycle()

                val hiddenMusicTitle = stringResource(R.string.settings_hidden_music)
                val privacyPolicyUrl = stringResource(R.string.settings_privacy_policy_url)
                val faqUrl = stringResource(R.string.settings_faq_url)
                val termsUrl = stringResource(R.string.settings_terms_url)
                val appVersionDisplay = stringResource(R.string.app_version_name)

                val restoreClicked = remember{ {
                    if (navController.currentDestination?.id == R.id.settingsFragment){
                        navController.navigate(R.id.action_settingsFragment_to_restoreFragment)
                    }
                } }
                val whatsNewClicked = remember{ {
                    if (navController.currentDestination?.id == R.id.settingsFragment){
                        navController.navigate(R.id.action_settingsFragment_to_whatsNewFragment)
                    }
                } }
                val onRestoreFoldersClicked = remember{ {
                    if (navController.currentDestination?.id == R.id.settingsFragment){
                        navController.navigate(R.id.action_settingsFragment_to_restoreFolderFragment)
                    }
                } }

                val onBackupRestoreClicked = remember {
                    {
                        if (navController.currentDestination?.id == R.id.settingsFragment) {
                            navController.navigate(R.id.action_settingsFragment_to_backupRestoreFragment)
                        }
                    }
                }
                val onEqualizerClicked = remember {
                    {
                        if (navController.currentDestination?.id == R.id.settingsFragment) {
                            navController.navigate(R.id.action_settingsFragment_to_equalizerFragment)
                        }
                    }
                }
                val onSleepTimerClicked = remember {
                    {
                        if (navController.currentDestination?.id == R.id.settingsFragment) {
                            navController.navigate(R.id.action_settingsFragment_to_sleepTimerFragment)
                        }
                    }
                }
                val onGraphicThemeClicked = remember {
                    {
                        if (navController.currentDestination?.id == R.id.settingsFragment) {
                            navController.navigate(R.id.action_settingsFragment_to_themeFragment)
                        }
                    }
                }
                val onHiddenMusicClicked = remember(hiddenMusicTitle) {
                    {
                        if (navController.currentDestination?.id == R.id.settingsFragment) {
                            navController.navigate(
                                R.id.action_settingsFragment_to_placeholderFragment,
                                bundleOf("screenTitle" to hiddenMusicTitle),
                            )
                        }
                    }
                }
                val onFaqClicked = remember(faqUrl) {
                    {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(faqUrl)))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                            if (navController.currentDestination?.id == R.id.settingsFragment) {
                                navController.navigate(
                                    R.id.action_settingsFragment_to_placeholderFragment,
                                    bundleOf("screenTitle" to getString(R.string.settings_faq)),
                                )
                            }
                        }
                    }
                }
                val onTermsClicked = remember(termsUrl) {
                    {
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
                        } catch (e: Exception) {
                            crashReporter.logException(e)
                            if (navController.currentDestination?.id == R.id.settingsFragment) {
                                navController.navigate(
                                    R.id.action_settingsFragment_to_placeholderFragment,
                                    bundleOf("screenTitle" to getString(R.string.settings_terms_of_use)),
                                )
                            }
                        }
                    }
                }
                val onFeedbackClicked = remember {
                    {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:")
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("music.zen@outlook.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Zen Music | Feedback")
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
                val onPremiumClicked = remember { { viewModel.showPremiumPlaceholder() } }

                val snackbarHostState = remember { SnackbarHostState() }

                val message by viewModel.message.collectAsStateWithLifecycle()
                LaunchedEffect(key1 = message){
                    if (message.isEmpty()) return@LaunchedEffect
                    snackbarHostState.showSnackbar(message)
                }

                ZenTheme(themePreference) {
                    Scaffold(
                        topBar = {
                            TopBarWithBackArrow(
                                onBackArrowPressed = navController::popBackStack,
                                title = stringResource(R.string.settings),
                                actions = { }
                            )
                        },
                        content = { paddingValues ->
                            SettingsList(
                                paddingValues = paddingValues,
                                isAppUpdateAvailable = isAppUpdateAvailable,
                                onAppUpdateClicked = ::onAppUpdateClicked,
                                themePreference = themePreference,
                                onThemePreferenceChanged = preferenceProvider::updateTheme,
                                scanStatus = scanStatus,
                                onScanClicked = viewModel::scanForMusic,
                                onRestoreClicked = restoreClicked,
                                disabledCrashlytics = isCrashlyticsDisabled,
                                onAutoReportCrashClicked = preferenceProvider::toggleCrashlytics,
                                onWhatsNewClicked = whatsNewClicked,
                                onRestoreFoldersClicked = onRestoreFoldersClicked,
                                tabsSelection = tabsSelection,
                                onTabsSelectChange = viewModel::onTabsSelectChanged,
                                onTabsOrderChanged = viewModel::onTabsOrderChanged,
                                onTabsOrderConfirmed = viewModel::saveTabsOrder,
                                crossfadeEnabled = crossfadeEnabled,
                                onCrossfadeChanged = preferenceProvider::updateCrossfadeEnabled,
                                gaplessEnabled = gaplessEnabled,
                                onGaplessChanged = preferenceProvider::updateGaplessPlaybackEnabled,
                                keepScreenOn = keepScreenOn,
                                onKeepScreenOnChanged = preferenceProvider::updateKeepScreenOn,
                                showOnLockScreen = showOnLockScreen,
                                onShowOnLockScreenChanged = preferenceProvider::updateShowOnLockScreen,
                                pauseOnHeadsetDisconnect = pauseOnHeadset,
                                onPauseOnHeadsetChanged = preferenceProvider::updatePauseOnHeadsetDisconnect,
                                onEqualizerClicked = onEqualizerClicked,
                                onSleepTimerClicked = onSleepTimerClicked,
                                onGraphicThemeClicked = onGraphicThemeClicked,
                                onHiddenMusicClicked = onHiddenMusicClicked,
                                onBackupRestoreClicked = onBackupRestoreClicked,
                                onFaqClicked = onFaqClicked,
                                onFeedbackClicked = onFeedbackClicked,
                                onRateUsClicked = onRateUsClicked,
                                onPrivacyPolicyClicked = onPrivacyPolicyClicked,
                                onTermsClicked = onTermsClicked,
                                onLanguageClicked = onLanguageClicked,
                                onPremiumClicked = onPremiumClicked,
                                appVersionDisplay = appVersionDisplay,
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(
                                hostState = snackbarHostState,
                                snackbar = {
                                    Snackbar(it)
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    private fun onAppUpdateClicked() {
        val appUpdateInfo = viewModel.appUpdateInfo.value
        viewModel.consumeAppUpdateInfo()
        try {
            appUpdateInfo?.let {
                appUpdateManager.startUpdateFlow(
                    it,
                    requireActivity(),
                    AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE)
                )
            }
        } catch (e: Exception) {
            crashReporter.logException(e)
        }
    }
}