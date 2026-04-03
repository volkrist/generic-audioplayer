package com.generic.audioplayes.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.generic.audioplayes.Constants
import com.generic.audioplayes.R
import com.generic.audioplayes.Screens
import com.generic.audioplayes.data.AudioPlayerPreferenceProvider
import com.generic.audioplayes.data.music.ScanStatus
import com.generic.audioplayes.data.music.SongExtractor
import com.generic.audioplayes.util.MessageStore
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val songExtractor: SongExtractor,
    private val prefs: AudioPlayerPreferenceProvider,
    private val messageStore: MessageStore,
    private val appUpdateManager: AppUpdateManager,
) : ViewModel() {

    val scanStatus = songExtractor.scanStatus
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 300,
                replayExpirationMillis = 0
            ),
            initialValue = ScanStatus.ScanNotRunning
        )

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private fun showMessage(message: String){
        viewModelScope.launch {
            _message.update { message }
            delay(Constants.MESSAGE_DURATION)
            _message.update { "" }
        }
    }

    fun scanForMusic() {
        songExtractor.scanForMusic()
    }

    private val _tabsSelection = MutableStateFlow<List<Pair<Screens,Boolean>>>(listOf())
    val tabsSelection = _tabsSelection.asStateFlow()

    init {
        val selectedScreens = prefs.selectedTabs.value ?: listOf()
        val allScreens = Screens.values()
        val currentSelection = arrayListOf<Pair<Screens,Boolean>>()
        selectedScreens.forEach {
            try {
                currentSelection += Pair(allScreens[it],true)
            } catch (_: Exception){

            }
        }
        allScreens.forEach {
            if (!selectedScreens.contains(it.ordinal)){
                currentSelection += Pair(it,false)
            }
        }
        _tabsSelection.update { currentSelection.toList() }
    }

    fun onTabsSelectChanged(screen: Screens, isSelected: Boolean){
        viewModelScope.launch {
            val newSelection = _tabsSelection.value.map {
                if (it.first.ordinal == screen.ordinal){
                    Pair(it.first, isSelected)
                } else {
                    it
                }
            }
            _tabsSelection.update { newSelection }
        }
    }

    fun onTabsOrderChanged(fromIndex: Int, toIndex: Int){
        viewModelScope.launch {
            val newOrder = _tabsSelection.value.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }.toList()
            _tabsSelection.update { newOrder }
        }
    }

    fun saveTabsOrder() {
        viewModelScope.launch {
            val order = _tabsSelection.value.filter { it.second }.map { it.first.ordinal }
            if (order.isEmpty()){
                showMessage(messageStore.getString(R.string.minimum_one_tab_selection_is_required))
            } else if (order.size > Screens.values().size) {
                showMessage(
                    messageStore.getString(
                        R.string.maximum_tab_selections_are_allowed,
                        Screens.values().size,
                    ),
                )
            } else if(!order.contains(Screens.Songs.ordinal)) {
                showMessage(messageStore.getString(R.string.songs_tab_cannot_be_removed))
            } else {
                prefs.updateSelectedTabs(order)
                showMessage(messageStore.getString(R.string.done))
            }
        }
    }

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo = _appUpdateInfo.asStateFlow()

    init {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                _appUpdateInfo.update { appUpdateInfo }
            }
        }
    }

    fun consumeAppUpdateInfo() {
        _appUpdateInfo.update { null }
    }

    fun showPremiumPlaceholder() {
        showMessage(messageStore.getString(R.string.settings_premium_desc))
    }
}