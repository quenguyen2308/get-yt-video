package dev.quenguyen.ytgrab.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.quenguyen.ytgrab.model.MediaFormat
import dev.quenguyen.ytgrab.settings.AppSettings
import dev.quenguyen.ytgrab.settings.SettingsRepository
import dev.quenguyen.ytgrab.ytdlp.YtDlpRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val ytDlpRepository: YtDlpRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    var updateStatusText by mutableStateOf<String?>(null)
        private set

    var isUpdating by mutableStateOf(false)
        private set

    fun setDefaultFormat(format: MediaFormat) {
        viewModelScope.launch { settingsRepository.setDefaultFormat(format) }
    }

    fun setDefaultHeightCap(heightCap: Int?) {
        viewModelScope.launch { settingsRepository.setDefaultHeightCap(heightCap) }
    }

    fun setDefaultAudioQualityArg(value: String) {
        viewModelScope.launch { settingsRepository.setDefaultAudioQualityArg(value) }
    }

    fun setConcurrency(value: Int) {
        viewModelScope.launch { settingsRepository.setConcurrency(value) }
    }

    fun setWifiOnly(value: Boolean) {
        viewModelScope.launch { settingsRepository.setWifiOnly(value) }
    }

    fun checkForYtDlpUpdate() {
        viewModelScope.launch {
            isUpdating = true
            updateStatusText = null
            try {
                val status = ytDlpRepository.updateYoutubeDl()
                updateStatusText = status?.name ?: "Up to date"
            } catch (e: Exception) {
                updateStatusText = "Update failed: ${e.message}"
            } finally {
                isUpdating = false
            }
        }
    }
}
