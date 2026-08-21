package dev.quenguyen.ytgrab.ytdlp

/**
 * Pure helpers for turning a quality choice into yt-dlp format-selector arguments.
 * Kept free of Android/yt-dlp-android types so it can be unit tested on the JVM.
 */
object FormatArgs {

    /**
     * [languageCode] is a yt-dlp audio-track language code (e.g. "vi"). When set, this prefers
     * that dub track but falls back to the video's original/default audio if the video doesn't
     * offer it — yt-dlp's `[language=xx]` filter fails the whole selector branch (not just that
     * clause) when no format matches, so the fallback must be a second full `+`-joined branch.
     */
    fun videoFormatSelector(heightCap: Int?, languageCode: String? = null): String {
        val heightFilter = heightCap?.let { "[height<=$it]" }.orEmpty()
        val defaultSelector = "bestvideo$heightFilter+bestaudio/best$heightFilter"
        return if (languageCode == null) {
            defaultSelector
        } else {
            "bestvideo$heightFilter+bestaudio[language=$languageCode]/$defaultSelector"
        }
    }

    /** Same language-with-fallback idea as [videoFormatSelector], for audio-only extraction. Null means "don't pass -f, use yt-dlp's default". */
    fun audioFormatSelector(languageCode: String?): String? =
        languageCode?.let { "bestaudio[language=$it]/bestaudio/best" }
}
