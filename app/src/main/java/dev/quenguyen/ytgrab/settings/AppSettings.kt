package dev.quenguyen.ytgrab.settings

import dev.quenguyen.ytgrab.model.MediaFormat

data class AppSettings(
    val defaultFormat: MediaFormat = MediaFormat.MP4,
    /** null = "Best" */
    val defaultHeightCap: Int? = null,
    /** A yt-dlp --audio-quality value: "0" (best), "192K", or "128K". */
    val defaultAudioQualityArg: String = "0",
    val concurrency: Int = 1,
    val wifiOnly: Boolean = true,
)
