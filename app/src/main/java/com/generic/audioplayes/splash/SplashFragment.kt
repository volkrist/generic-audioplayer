package com.generic.audioplayes.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.generic.audioplayes.R
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Stub Fragment with no view
 * Used to decide if onboarding is to be shown or home screen
 */
@AndroidEntryPoint
class SplashFragment : Fragment() {

    private lateinit var navController: NavController

    @Inject
    lateinit var preferenceProvider: AudioPlayerPreferenceProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        navController = findNavController()
        lifecycleScope.launch {
            val complete = try {
                preferenceProvider.readOnBoardingComplete()
            } catch (_: Exception) {
                false
            }
            if (navController.currentDestination?.id != R.id.splashFragment) return@launch
            val action = if (complete) {
                R.id.action_splashFragment_to_homeFragment
            } else {
                R.id.action_splashFragment_to_onBoardingFragment
            }
            navController.navigate(action)
        }
        return null
    }
}