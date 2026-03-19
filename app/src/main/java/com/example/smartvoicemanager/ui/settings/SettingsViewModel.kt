package com.example.smartvoicemanager.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartvoicemanager.data.prefs.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = settingsManager.isDarkTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val language: StateFlow<String> = settingsManager.language
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val defaultMusicUri: StateFlow<String?> = settingsManager.defaultMusicUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setTheme(isDark: Boolean) {
        viewModelScope.launch {
            settingsManager.setTheme(isDark)
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsManager.setLanguage(lang)
        }
    }

    fun setDefaultMusicUri(uri: String?) {
        viewModelScope.launch {
            settingsManager.setDefaultMusicUri(uri)
        }
    }
}
