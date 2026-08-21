package dev.quenguyen.ytgrab.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import dev.quenguyen.ytgrab.model.MediaFormat
import java.io.File

/** Copies a finished download from the app-private folder into the public Movies/YtGrab or Music/YtGrab collection. */
object MediaStoreSaver {

    fun save(context: Context, file: File, format: MediaFormat): Uri? {
        val resolver = context.contentResolver
        val collection: Uri
        val values = ContentValues()
        when (format) {
            MediaFormat.MP4 -> {
                collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                values.put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                values.put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/YtGrab")
                values.put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            MediaFormat.MP3 -> {
                collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                values.put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/YtGrab")
                values.put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collection, values) ?: return null
        resolver.openOutputStream(itemUri)?.use { out ->
            file.inputStream().use { it.copyTo(out) }
        } ?: return null

        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(itemUri, values, null, null)
        return itemUri
    }
}
