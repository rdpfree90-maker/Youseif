package com.youseif.playerpro.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.youseif.playerpro.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val language: String = "en",
    val customUserAgent: String = SettingsRepository.DEFAULT_UA,
    val defaultReferer: String = "",
    val enableCookies: Boolean = true,
    val enableCache: Boolean = true,
    val enableJavascript: Boolean = true,
    val autoplay: Boolean = false,
    val hardwareAcceleration: Boolean = true,
    val dataSaver: Boolean = false,
    val theme: String = "dark"
)

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.language,
        repository.customUserAgent,
        repository.defaultReferer,
        repository.enableCookies,
        repository.enableCache,
        repository.enableJavascript,
        repository.autoplay,
        repository.hardwareAcceleration,
        repository.dataSaver,
        repository.theme
    ) { values ->
        SettingsUiState(
            language = values[0] as String,
            customUserAgent = values[1] as String,
            defaultReferer = values[2] as String,
            enableCookies = values[3] as Boolean,
            enableCache = values[4] as Boolean,
            enableJavascript = values[5] as Boolean,
            autoplay = values[6] as Boolean,
            hardwareAcceleration = values[7] as Boolean,
            dataSaver = values[8] as Boolean,
            theme = values[9] as String
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setLanguage(v: String) = viewModelScope.launch { repository.setLanguage(v) }
    fun setCustomUserAgent(v: String) = viewModelScope.launch { repository.setCustomUserAgent(v) }
    fun setDefaultReferer(v: String) = viewModelScope.launch { repository.setDefaultReferer(v) }
    fun setEnableCookies(v: Boolean) = viewModelScope.launch { repository.setEnableCookies(v) }
    fun setEnableCache(v: Boolean) = viewModelScope.launch { repository.setEnableCache(v) }
    fun setEnableJavascript(v: Boolean) = viewModelScope.launch { repository.setEnableJavascript(v) }
    fun setAutoplay(v: Boolean) = viewModelScope.launch { repository.setAutoplay(v) }
    fun setHardwareAcceleration(v: Boolean) = viewModelScope.launch { repository.setHardwareAcceleration(v) }
    fun setDataSaver(v: Boolean) = viewModelScope.launch { repository.setDataSaver(v) }
    fun setTheme(v: String) = viewModelScope.launch { repository.setTheme(v) }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
