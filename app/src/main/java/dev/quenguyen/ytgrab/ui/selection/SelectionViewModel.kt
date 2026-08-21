package dev.quenguyen.ytgrab.ui.selection

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.quenguyen.ytgrab.download.DownloadQueueRepository
import dev.quenguyen.ytgrab.download.DownloadService
import dev.quenguyen.ytgrab.model.AudioLanguageOption
import dev.quenguyen.ytgrab.model.DownloadTask
import dev.quenguyen.ytgrab.model.MP3_QUALITY_PRESETS
import dev.quenguyen.ytgrab.model.MediaFormat
import dev.quenguyen.ytgrab.model.OutputQuality
import dev.quenguyen.ytgrab.model.YtMetadata
import dev.quenguyen.ytgrab.settings.SettingsRepository
import dev.quenguyen.ytgrab.ui.common.PendingSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelectionViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    val metadata: YtMetadata? = PendingSessionRepository.metadata.value

    var selectedEntryIds by mutableStateOf<Set<String>>(metadata?.entries?.map { it.id }?.toSet() ?: emptySet())
        private set

    var format by mutableStateOf(MediaFormat.MP4)
        private set

    /** null = "Best" */
    var videoHeightCap by mutableStateOf<Int?>(null)
        private set

    var audioPreset by mutableStateOf(MP3_QUALITY_PRESETS.first())
        private set

    /** Dubbed-language choices to offer, besides the always-available "original" option. */
    val audioLanguageOptions: List<AudioLanguageOption> = when {
        metadata == null -> emptyList()
        // A playlist comes from --flat-playlist, which never resolves per-entry formats, so we
        // can't detect which languages each video actually offers. Offer Vietnamese manually —
        // buildDownloadRequest's language-selector fallback quietly keeps the original track for
        // any entry that doesn't have it, so this is safe to offer even when we can't confirm it.
        metadata.isPlaylist -> listOf(AudioLanguageOption(code = "vi", label = "Tiếng Việt", isOriginal = false))
        else -> metadata.availableAudioLanguages.filterNot { it.isOriginal }
            .sortedBy { languageSortPriority(it.code) }
    }

    /** null = the video's original/default audio track. */
    var audioLanguage by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            format = settings.defaultFormat
            videoHeightCap = settings.defaultHeightCap
            audioPreset = MP3_QUALITY_PRESETS.firstOrNull { it.audioQualityArg == settings.defaultAudioQualityArg }
                ?: MP3_QUALITY_PRESETS.first()
        }
    }

    fun toggleEntry(id: String) {
        selectedEntryIds = if (id in selectedEntryIds) selectedEntryIds - id else selectedEntryIds + id
    }

    fun selectAll(select: Boolean) {
        selectedEntryIds = if (select) metadata?.entries.orEmpty().map { it.id }.toSet() else emptySet()
    }

    fun onFormatSelected(value: MediaFormat) {
        format = value
    }

    fun onVideoHeightCapSelected(value: Int?) {
        videoHeightCap = value
    }

    fun onAudioPresetSelected(value: OutputQuality.Audio) {
        audioPreset = value
    }

    fun onAudioLanguageSelected(code: String?) {
        audioLanguage = code
    }

    private fun buildTasks(): List<DownloadTask> {
        val meta = metadata ?: return emptyList()
        val quality: OutputQuality =
            if (format == MediaFormat.MP4) OutputQuality.Video(videoHeightCap) else audioPreset
        return if (meta.isPlaylist) {
            meta.entries.filter { it.id in selectedEntryIds }.map { entry ->
                DownloadTask(
                    sourceUrl = entry.url,
                    title = entry.title,
                    format = format,
                    quality = quality,
                    audioLanguage = audioLanguage,
                )
            }
        } else {
            listOf(
                DownloadTask(
                    sourceUrl = meta.sourceUrl,
                    title = meta.title,
                    format = format,
                    quality = quality,
                    audioLanguage = audioLanguage,
                ),
            )
        }
    }

    /** Returns false if there was nothing to enqueue (e.g. no playlist items selected). */
    fun confirm(context: Context): Boolean {
        val tasks = buildTasks()
        if (tasks.isEmpty()) return false
        DownloadQueueRepository.enqueue(tasks)
        DownloadService.start(context)
        PendingSessionRepository.clear()
        return true
    }

    private companion object {
        /** Vietnamese first, then English, then everything else in whatever order it arrived. */
        fun languageSortPriority(code: String): Int = when (code) {
            "vi" -> 0
            "en" -> 1
            else -> 2
        }
    }
}
