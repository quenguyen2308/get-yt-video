package dev.quenguyen.ytgrab.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.yausername.youtubedl_android.YoutubeDL
import dev.quenguyen.ytgrab.MainActivity
import dev.quenguyen.ytgrab.R
import dev.quenguyen.ytgrab.YtGrabApp
import dev.quenguyen.ytgrab.history.AppDatabase
import dev.quenguyen.ytgrab.history.DownloadHistoryEntity
import dev.quenguyen.ytgrab.model.DownloadStatus
import dev.quenguyen.ytgrab.model.DownloadTask
import dev.quenguyen.ytgrab.model.OutputQuality
import dev.quenguyen.ytgrab.ytdlp.YtDlpRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class DownloadService : LifecycleService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: YtDlpRepository
    private var workerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        repository = YtDlpRepository(applicationContext)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(title = getString(R.string.downloading_placeholder), progress = 0))
        if (workerJob == null) {
            workerJob = serviceScope.launch {
                val concurrency = (application as YtGrabApp).settingsRepository.settings.first().concurrency
                List(concurrency) { launch { processQueue() } }.joinAll()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun processQueue() {
        while (true) {
            val id = DownloadQueueRepository.awaitNextPendingId()
            val task = DownloadQueueRepository.taskById(id) ?: continue
            if (task.status == DownloadStatus.CANCELLED) continue
            runDownload(task)
            if (!DownloadQueueRepository.hasActiveOrQueuedTasks()) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
        }
    }

    private suspend fun runDownload(task: DownloadTask) {
        val wifiOnly = (application as YtGrabApp).settingsRepository.settings.first().wifiOnly
        if (wifiOnly && !NetworkUtils.isUnmeteredConnection(applicationContext)) {
            DownloadQueueRepository.update(task.id) {
                it.copy(status = DownloadStatus.FAILED, errorMessage = "Wi-Fi only is enabled — connect to Wi-Fi and retry")
            }
            return
        }
        DownloadQueueRepository.update(task.id) { it.copy(status = DownloadStatus.RUNNING) }
        startForeground(NOTIFICATION_ID, buildNotification(task.title, 0))
        val outputDir = repository.downloadsDir
        val before = outputDir.listFiles()?.toSet() ?: emptySet()
        try {
            val request = repository.buildDownloadRequest(task)
            YoutubeDL.getInstance().execute(request, task.id) { progress, etaSeconds, _ ->
                // The library reports -1 as a "no progress yet" sentinel while it's still
                // resolving the URL/formats, before the first real "[download] x%" line — don't
                // let that leak into the UI as a literal "-1%".
                if (progress >= 0f) {
                    DownloadQueueRepository.update(task.id) { it.copy(progress = progress, etaSeconds = etaSeconds) }
                    startForeground(NOTIFICATION_ID, buildNotification(task.title, progress.toInt()))
                }
            }
            val newFile = ((outputDir.listFiles()?.toSet() ?: emptySet()) - before).firstOrNull()
                ?: outputDir.listFiles()?.maxByOrNull { it.lastModified() }

            val savedUri = newFile?.let { MediaStoreSaver.save(applicationContext, it, task.format) }
            if (savedUri == null) {
                DownloadQueueRepository.update(task.id) {
                    it.copy(status = DownloadStatus.FAILED, errorMessage = "Could not save output file")
                }
            } else {
                newFile.delete()
                DownloadQueueRepository.update(task.id) {
                    it.copy(status = DownloadStatus.COMPLETED, progress = 100f, outputUri = savedUri.toString())
                }
                saveHistory(task, savedUri.toString())
            }
        } catch (e: Exception) {
            // A user-initiated cancel kills the process, which makes execute() throw here too —
            // don't let that clobber the CANCELLED status the cancel action already set.
            if (DownloadQueueRepository.taskById(task.id)?.status == DownloadStatus.CANCELLED) {
                deletePartialFiles(outputDir)
            } else {
                DownloadQueueRepository.update(task.id) {
                    it.copy(status = DownloadStatus.FAILED, errorMessage = e.message ?: "Download failed")
                }
            }
        }
    }

    /**
     * yt-dlp names its in-progress temp files deterministically from the video title+id, so a
     * cancelled attempt's .part file is already present in [outputDir] on the next try of the
     * same video — a before/after directory diff can't detect it as "new". Delete by suffix
     * instead: .part/.ytdl are always yt-dlp's own incomplete/resume-metadata markers and never
     * remain after a successful download.
     */
    private fun deletePartialFiles(outputDir: File) {
        outputDir.listFiles { file -> file.name.endsWith(".part") || file.name.endsWith(".ytdl") }
            ?.forEach { it.delete() }
    }

    private suspend fun saveHistory(task: DownloadTask, contentUri: String) {
        val qualityLabel = when (val q = task.quality) {
            is OutputQuality.Video -> q.label
            is OutputQuality.Audio -> q.label
        }
        AppDatabase.get(applicationContext).downloadHistoryDao().insert(
            DownloadHistoryEntity(
                id = task.id,
                title = task.title,
                sourceUrl = task.sourceUrl,
                format = task.format.name,
                qualityLabel = qualityLabel,
                contentUri = contentUri,
                completedAtMillis = System.currentTimeMillis(),
            )
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.download_channel_name), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText("$progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            context.startForegroundService(Intent(context, DownloadService::class.java))
        }
    }
}
