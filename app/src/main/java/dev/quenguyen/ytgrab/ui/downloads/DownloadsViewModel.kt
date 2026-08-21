package dev.quenguyen.ytgrab.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import dev.quenguyen.ytgrab.download.DownloadQueueRepository
import dev.quenguyen.ytgrab.history.DownloadHistoryDao
import dev.quenguyen.ytgrab.history.DownloadHistoryEntity
import dev.quenguyen.ytgrab.model.DownloadTask
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(private val historyDao: DownloadHistoryDao) : ViewModel() {

    val activeTasks: StateFlow<List<DownloadTask>> = DownloadQueueRepository.tasks

    val history: StateFlow<List<DownloadHistoryEntity>> = historyDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(taskId: String) {
        YoutubeDL.getInstance().destroyProcessById(taskId)
        DownloadQueueRepository.requestCancel(taskId)
    }

    fun deleteHistoryEntry(entity: DownloadHistoryEntity) {
        viewModelScope.launch { historyDao.delete(entity) }
    }
}
