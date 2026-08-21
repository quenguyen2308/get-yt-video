package dev.quenguyen.ytgrab

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dev.quenguyen.ytgrab.history.AppDatabase
import dev.quenguyen.ytgrab.settings.SettingsRepository
import dev.quenguyen.ytgrab.ytdlp.YtDlpRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YtGrabApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val ytDlpRepository by lazy { YtDlpRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val database by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            try {
                YoutubeDL.getInstance().init(this@YtGrabApp)
                FFmpeg.init(this@YtGrabApp)
                YoutubeDL.getInstance().updateYoutubeDL(this@YtGrabApp)
            } catch (e: Exception) {
                Log.e(TAG, "yt-dlp/ffmpeg initialization failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "YtGrabApp"
    }
}
