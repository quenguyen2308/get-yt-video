package dev.quenguyen.ytgrab.ytdlp

import dev.quenguyen.ytgrab.model.AudioLanguageOption
import dev.quenguyen.ytgrab.model.PlaylistEntryInfo
import dev.quenguyen.ytgrab.model.YtMetadata
import org.json.JSONObject

/**
 * Parses the raw stdout of `yt-dlp --flat-playlist --dump-single-json` ourselves instead of
 * relying on youtubedl-android's built-in VideoInfo model, which has no `entries` field and
 * therefore can't represent playlists.
 */
object MetadataParser {

    // yt-dlp's format_note for a language track isn't consistent across formats (e.g. "polski -
    // dubbed" for one itag, "Polish, low" for another for the very same track), and raw JSON
    // array order isn't guaranteed, so which note we'd see first is a coin flip. Prefer a native
    // display name for the languages YouTube's dubbing feature actually produces; format_note is
    // only a fallback for codes not covered here.
    private val NATIVE_LANGUAGE_NAMES = mapOf(
        "ar" to "العربية",
        "bn" to "বাংলা",
        "de" to "Deutsch",
        "en" to "English",
        "es" to "Español",
        "fil" to "Filipino",
        "fr" to "Français",
        "hi" to "हिन्दी",
        "id" to "Indonesia",
        "it" to "Italiano",
        "ja" to "日本語",
        "ko" to "한국어",
        "ms" to "Bahasa Melayu",
        "nl" to "Nederlands",
        "pl" to "polski",
        "pt" to "Português",
        "ru" to "Русский",
        "th" to "ไทย",
        "tr" to "Türkçe",
        "uk" to "Українська",
        "vi" to "Tiếng Việt",
        "zh" to "中文",
    )

    fun parse(sourceUrl: String, rawJson: String): YtMetadata {
        val json = JSONObject(rawJson)
        val entriesArray = json.optJSONArray("entries")
        val entries = mutableListOf<PlaylistEntryInfo>()
        if (entriesArray != null) {
            for (i in 0 until entriesArray.length()) {
                val entry = entriesArray.optJSONObject(i) ?: continue
                val id = entry.optString("id")
                if (id.isNullOrBlank()) continue
                entries += PlaylistEntryInfo(
                    id = id,
                    title = entry.optString("title", id),
                    url = "https://www.youtube.com/watch?v=$id",
                    durationSeconds = entry.optInt("duration", -1).takeIf { it >= 0 },
                    thumbnail = entry.optString("thumbnail").takeIf { it.isNotBlank() },
                )
            }
        }

        val heights = mutableSetOf<Int>()
        val audioLanguages = linkedMapOf<String, AudioLanguageOption>()
        json.optJSONArray("formats")?.let { formats ->
            for (i in 0 until formats.length()) {
                val format = formats.optJSONObject(i) ?: continue
                val vcodec = format.optString("vcodec", "none")
                val height = format.optInt("height", -1)
                if (vcodec != "none" && vcodec.isNotBlank() && height > 0) heights += height

                val acodec = format.optString("acodec", "none")
                val languageCode = format.optString("language").takeIf { it.isNotBlank() }
                // Audio-only formats carrying a language tag are YouTube's multi-language dub
                // tracks; video+audio combined formats never carry one, so this only fires for
                // videos that actually offer dubs.
                if (vcodec == "none" && acodec != "none" && acodec.isNotBlank() && languageCode != null) {
                    audioLanguages.getOrPut(languageCode) {
                        val note = format.optString("format_note").takeIf { it.isNotBlank() }
                        val isOriginal = format.optInt("language_preference", -1) >= 10
                        AudioLanguageOption(
                            code = languageCode,
                            label = NATIVE_LANGUAGE_NAMES[languageCode.lowercase()]
                                ?: note?.substringBefore(" - ")?.trim()?.takeIf { it.isNotBlank() && !it.contains(",") }
                                ?: languageCode.uppercase(),
                            isOriginal = isOriginal,
                        )
                    }
                }
            }
        }

        return YtMetadata(
            sourceUrl = sourceUrl,
            id = json.optString("id", sourceUrl),
            title = json.optString("title", sourceUrl),
            thumbnail = json.optString("thumbnail").takeIf { it.isNotBlank() },
            // A non-null but empty entries array (e.g. an unresolved mix/radio queue) isn't a
            // usable playlist, so fall back to treating this as a single video.
            isPlaylist = entriesArray != null && entries.isNotEmpty(),
            availableHeights = heights.sortedDescending(),
            availableAudioLanguages = audioLanguages.values.sortedByDescending { it.isOriginal },
            entries = entries,
        )
    }
}
