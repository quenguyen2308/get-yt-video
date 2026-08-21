package dev.quenguyen.ytgrab.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.quenguyen.ytgrab.ui.common.PendingSessionRepository
import dev.quenguyen.ytgrab.ytdlp.YtDlpRepository
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: YtDlpRepository) : ViewModel() {

    var urlField by mutableStateOf(TextFieldValue(""))
        private set

    val urlText: String
        get() = urlField.text

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onUrlChanged(value: TextFieldValue) {
        urlField = value
        errorMessage = null
    }

    /** Sets the URL from outside user typing (paste, shared link) — cursor lands at the end so the field scrolls there without a manual drag. */
    fun setUrlText(value: String) {
        urlField = TextFieldValue(value, selection = TextRange(value.length))
        errorMessage = null
    }

    fun submit(onReady: () -> Unit) {
        val url = urlText.trim()
        if (url.isBlank()) {
            errorMessage = "Paste a YouTube video or playlist link first"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val metadata = repository.fetchMetadata(url)
                PendingSessionRepository.set(metadata)
                onReady()
            } catch (e: Exception) {
                errorMessage = "Couldn't read that link: ${e.message ?: "unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }
}
