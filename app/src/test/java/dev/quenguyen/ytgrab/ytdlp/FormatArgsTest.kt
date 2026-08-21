package dev.quenguyen.ytgrab.ytdlp

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatArgsTest {

    @Test
    fun `best quality has no height cap`() {
        assertEquals("bestvideo+bestaudio/best", FormatArgs.videoFormatSelector(null))
    }

    @Test
    fun `720p caps both video and fallback streams`() {
        assertEquals(
            "bestvideo[height<=720]+bestaudio/best[height<=720]",
            FormatArgs.videoFormatSelector(720),
        )
    }

    @Test
    fun `1080p caps both video and fallback streams`() {
        assertEquals(
            "bestvideo[height<=1080]+bestaudio/best[height<=1080]",
            FormatArgs.videoFormatSelector(1080),
        )
    }

    @Test
    fun `language filter falls back to the default selector when unavailable`() {
        assertEquals(
            "bestvideo[height<=720]+bestaudio[language=vi]/bestvideo[height<=720]+bestaudio/best[height<=720]",
            FormatArgs.videoFormatSelector(720, "vi"),
        )
    }

    @Test
    fun `language filter with best quality still falls back`() {
        assertEquals(
            "bestvideo+bestaudio[language=vi]/bestvideo+bestaudio/best",
            FormatArgs.videoFormatSelector(null, "vi"),
        )
    }

    @Test
    fun `audio selector is null when no language requested`() {
        assertEquals(null, FormatArgs.audioFormatSelector(null))
    }

    @Test
    fun `audio selector prefers the requested language then falls back`() {
        assertEquals("bestaudio[language=vi]/bestaudio/best", FormatArgs.audioFormatSelector("vi"))
    }
}
