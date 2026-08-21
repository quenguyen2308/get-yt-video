package dev.quenguyen.ytgrab.ytdlp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataParserTest {

    @Test
    fun `single video is not a playlist and exposes distinct sorted heights`() {
        val json = """
            {
              "id": "abc123",
              "title": "Some Video",
              "thumbnail": "https://example.com/thumb.jpg",
              "formats": [
                {"vcodec": "avc1", "height": 360},
                {"vcodec": "avc1", "height": 1080},
                {"vcodec": "avc1", "height": 720},
                {"vcodec": "avc1", "height": 720},
                {"vcodec": "none", "acodec": "opus", "height": 0}
              ]
            }
        """.trimIndent()

        val metadata = MetadataParser.parse("https://youtu.be/abc123", json)

        assertFalse(metadata.isPlaylist)
        assertTrue(metadata.entries.isEmpty())
        assertEquals(listOf(1080, 720, 360), metadata.availableHeights)
        assertEquals("Some Video", metadata.title)
        assertTrue("single-language video shouldn't report a language picker", metadata.availableAudioLanguages.isEmpty())
    }

    @Test
    fun `multi-language dubs are exposed with the original track first`() {
        val json = """
            {
              "id": "abc123",
              "title": "Dubbed Video",
              "formats": [
                {"vcodec": "avc1", "height": 360},
                {"vcodec": "none", "acodec": "mp4a.40.5", "language": "vi", "language_preference": -1, "format_note": "Tiếng Việt - dubbed"},
                {"vcodec": "none", "acodec": "mp4a.40.5", "language": "vi", "language_preference": -1, "format_note": "Vietnamese, low"},
                {"vcodec": "none", "acodec": "mp4a.40.5", "language": "en", "language_preference": 10, "format_note": "English - original (original)"}
              ]
            }
        """.trimIndent()

        val metadata = MetadataParser.parse("https://youtu.be/abc123", json)

        assertEquals(2, metadata.availableAudioLanguages.size)
        assertEquals("en", metadata.availableAudioLanguages[0].code)
        assertTrue(metadata.availableAudioLanguages[0].isOriginal)
        assertEquals("English", metadata.availableAudioLanguages[0].label)
        assertEquals("vi", metadata.availableAudioLanguages[1].code)
        assertFalse(metadata.availableAudioLanguages[1].isOriginal)
        assertEquals("Tiếng Việt", metadata.availableAudioLanguages[1].label)
    }

    @Test
    fun `playlist url exposes entries with reconstructed watch urls`() {
        val json = """
            {
              "id": "PL123",
              "title": "My Playlist",
              "entries": [
                {"id": "vid1", "title": "First", "duration": 120},
                {"id": "vid2", "title": "Second", "duration": 90}
              ]
            }
        """.trimIndent()

        val metadata = MetadataParser.parse("https://youtube.com/playlist?list=PL123", json)

        assertTrue(metadata.isPlaylist)
        assertEquals(2, metadata.entries.size)
        assertEquals("https://www.youtube.com/watch?v=vid1", metadata.entries[0].url)
        assertEquals(120, metadata.entries[0].durationSeconds)
    }
}
