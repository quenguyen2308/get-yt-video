package dev.quenguyen.ytgrab.ui.common

import dev.quenguyen.ytgrab.model.YtMetadata
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the metadata for whatever link was just fetched on the Home screen, for the Selection screen to read. */
object PendingSessionRepository {
    private val _metadata = MutableStateFlow<YtMetadata?>(null)
    val metadata: StateFlow<YtMetadata?> = _metadata.asStateFlow()

    fun set(metadata: YtMetadata) {
        _metadata.value = metadata
    }

    fun clear() {
        _metadata.value = null
    }
}
