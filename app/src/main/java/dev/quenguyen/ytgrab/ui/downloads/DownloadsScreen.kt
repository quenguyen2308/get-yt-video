package dev.quenguyen.ytgrab.ui.downloads

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.quenguyen.ytgrab.history.DownloadHistoryEntity
import dev.quenguyen.ytgrab.model.DownloadStatus
import dev.quenguyen.ytgrab.model.DownloadTask
import dev.quenguyen.ytgrab.model.MediaFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(viewModel: DownloadsViewModel, onBack: () -> Unit) {
    val active by viewModel.activeTasks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Completed tasks reuse their id as the history row id (see DownloadService.saveHistory),
            // so they must drop out of this list once done to avoid a duplicate LazyColumn key.
            val activeVisible = active.filter {
                it.status != DownloadStatus.COMPLETED && it.status != DownloadStatus.CANCELLED
            }
            if (activeVisible.isNotEmpty()) {
                item { SectionHeader("Active") }
                items(activeVisible, key = { it.id }) { task ->
                    ActiveTaskRow(task, onCancel = { viewModel.cancel(task.id) })
                    HorizontalDivider()
                }
            }

            item { SectionHeader("History") }
            if (history.isEmpty()) {
                item { Text("No downloads yet", modifier = Modifier.padding(16.dp)) }
            } else {
                items(history, key = { it.id }) { entity ->
                    HistoryRow(
                        entity,
                        onOpen = { openFile(context, entity) },
                        onDelete = { viewModel.deleteHistoryEntry(entity) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ActiveTaskRow(task: DownloadTask, onCancel: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            val statusLabel = when (task.status) {
                DownloadStatus.QUEUED -> "Queued"
                DownloadStatus.RUNNING -> "${task.progress.toInt()}%"
                DownloadStatus.COMPLETED -> "Done"
                DownloadStatus.FAILED -> "Failed: ${task.errorMessage ?: ""}"
                DownloadStatus.CANCELLED -> "Cancelled"
            }
            Text(statusLabel, style = MaterialTheme.typography.bodySmall)
            if (task.status == DownloadStatus.RUNNING) {
                LinearProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (task.status == DownloadStatus.QUEUED || task.status == DownloadStatus.RUNNING) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Cancel, contentDescription = "Cancel")
            }
        }
    }
}

@Composable
private fun HistoryRow(entity: DownloadHistoryEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen),
        ) {
            Text(entity.title, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
            Text("${entity.format} · ${entity.qualityLabel}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete")
        }
    }
}

private fun openFile(context: android.content.Context, entity: DownloadHistoryEntity) {
    val mimeType = if (entity.format == MediaFormat.MP4.name) "video/mp4" else "audio/mpeg"
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(entity.contentUri), mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}
