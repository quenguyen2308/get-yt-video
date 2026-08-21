package dev.quenguyen.ytgrab.model

import java.util.UUID

enum class MediaFormat { MP4, MP3 }

enum class DownloadStatus { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }

/** null heightCap means "best available" for [MediaFormat.MP4]. */
sealed class OutputQuality {
    data class Video(val heightCap: Int?) : OutputQuality() {
        val label: String get() = heightCap?.let { "${it}p" } ?: "Best"
    }

    /** [audioQualityArg] is a yt-dlp --audio-quality value: "0" (best) or a bitrate like "192K". */
    data class Audio(val label: String, val audioQualityArg: String) : OutputQuality()
}

data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val sourceUrl: String,
    val title: String,
    val format: MediaFormat,
    val quality: OutputQuality,
    /** yt-dlp audio language code (e.g. "vi"), or null for the video's original/default track. */
    val audioLanguage: String? = null,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val etaSeconds: Long = 0,
    val outputUri: String? = null,
    val errorMessage: String? = null,
)
