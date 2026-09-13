package dev.quenguyen.ytgrab.ytdlp

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateStatus
import com.yausername.youtubedl_android.YoutubeDLRequest
import dev.quenguyen.ytgrab.model.DownloadTask
import dev.quenguyen.ytgrab.model.MediaFormat
import dev.quenguyen.ytgrab.model.OutputQuality
import dev.quenguyen.ytgrab.model.YtMetadata
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YtDlpRepository(context: Context) {

    private val appContext = context.applicationContext

    val downloadsDir: File by lazy {
        File(appContext.getExternalFilesDir(null), "downloads").apply { mkdirs() }
    }

    suspend fun fetchMetadata(url: String): YtMetadata = withContext(Dispatchers.IO) {
        val request = YoutubeDLRequest(url).apply {
            addOption("--flat-playlist")
            addOption("--dump-single-json")
            addOption("--no-warnings")
            addOption("--skip-download")
            // Without this, YouTube sometimes attaches an autoplay "Mix"/recommended queue to a
            // plain watch URL, which yt-dlp then reports as an (empty) playlist. This keeps a
            // bare video link a single video; a URL that is *only* a playlist is unaffected.
            addOption("--no-playlist")
        }
        val response = YoutubeDL.getInstance().execute(request)
        MetadataParser.parse(url, response.out)
    }

    suspend fun updateYoutubeDl(): UpdateStatus? = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().updateYoutubeDL(appContext)
    }

    fun buildDownloadRequest(task: DownloadTask): YoutubeDLRequest {
        val request = YoutubeDLRequest(task.sourceUrl)
        request.addOption("-o", File(downloadsDir, OutputTemplate.forUrl(task.sourceUrl)).absolutePath)
        request.addOption("--no-playlist")
        when (task.format) {
            MediaFormat.MP4 -> {
                val heightCap = (task.quality as OutputQuality.Video).heightCap
                request.addOption("-f", FormatArgs.videoFormatSelector(heightCap, task.audioLanguage))
                request.addOption("--merge-output-format", "mp4")
            }
            MediaFormat.MP3 -> {
                val audio = task.quality as OutputQuality.Audio
                FormatArgs.audioFormatSelector(task.audioLanguage)?.let { request.addOption("-f", it) }
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", audio.audioQualityArg)
            }
        }
        return request
    }
}
