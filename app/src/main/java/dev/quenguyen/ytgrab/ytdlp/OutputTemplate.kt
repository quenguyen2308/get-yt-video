package dev.quenguyen.ytgrab.ytdlp

import java.net.URI

/**
 * Picks the yt-dlp `-o` output template based on the source URL.
 * Kept free of Android/yt-dlp-android types so it can be unit tested on the JVM.
 */
object OutputTemplate {

    /**
     * yt-dlp's "title" field for Weibo is the post's caption text (often long, hashtag-heavy,
     * and unrelated to the clip itself) rather than a meaningful video name — using it as the
     * filename basically reproduces the post/link title. Fall back to the video's own id for
     * Weibo URLs so the filename matches the actual video instead of its caption.
     */
    fun forUrl(sourceUrl: String): String =
        if (isWeiboUrl(sourceUrl)) "%(id)s.%(ext)s" else "%(title).150s.%(ext)s"

    private fun isWeiboUrl(sourceUrl: String): Boolean {
        val host = runCatching { URI(sourceUrl).host }.getOrNull()?.lowercase().orEmpty()
        return host == "weibo.com" || host.endsWith(".weibo.com") ||
            host == "weibo.cn" || host.endsWith(".weibo.cn")
    }
}
