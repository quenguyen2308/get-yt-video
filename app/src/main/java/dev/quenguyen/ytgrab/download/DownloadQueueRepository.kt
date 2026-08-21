package dev.quenguyen.ytgrab.download

import dev.quenguyen.ytgrab.model.DownloadStatus
import dev.quenguyen.ytgrab.model.DownloadTask
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory, process-wide source of truth for the download queue. Both the UI (via [tasks]) and
 * [DownloadService] (via [pendingTaskIds]) observe/drive this same singleton, which avoids having
 * to pass complex task objects through Intent extras.
 */
object DownloadQueueRepository {

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val pendingTaskIds = Channel<String>(Channel.UNLIMITED)

    fun enqueue(newTasks: List<DownloadTask>) {
        _tasks.update { it + newTasks }
        newTasks.forEach { pendingTaskIds.trySend(it.id) }
    }

    suspend fun awaitNextPendingId(): String = pendingTaskIds.receive()

    fun taskById(id: String): DownloadTask? = _tasks.value.firstOrNull { it.id == id }

    fun update(id: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun requestCancel(id: String) {
        update(id) { it.copy(status = DownloadStatus.CANCELLED) }
    }

    fun hasActiveOrQueuedTasks(): Boolean =
        _tasks.value.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }

    fun clearFinished() {
        _tasks.update { list -> list.filter { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING } }
    }
}
