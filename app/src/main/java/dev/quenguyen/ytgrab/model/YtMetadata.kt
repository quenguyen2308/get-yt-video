package dev.quenguyen.ytgrab.model

data class PlaylistEntryInfo(
    val id: String,
    val title: String,
    val url: String,
    val durationSeconds: Int?,
    val thumbnail: String?,
)

/** One dubbed/original audio track a video offers, e.g. YouTube's auto-dub languages. */
data class AudioLanguageOption(
    val code: String,
    val label: String,
    val isOriginal: Boolean,
)

data class YtMetadata(
    val sourceUrl: String,
    val id: String,
    val title: String,
    val thumbnail: String?,
    val isPlaylist: Boolean,
    /** Distinct video heights available for this video, sorted descending. Empty for playlists. */
    val availableHeights: List<Int>,
    /** Every audio-language track this video reports, original track first. Empty if the video only has one. */
    val availableAudioLanguages: List<AudioLanguageOption>,
    /** Populated only when [isPlaylist] is true. */
    val entries: List<PlaylistEntryInfo>,
)

val FIXED_MP4_PRESET_HEIGHTS = listOf(1080, 720, 480, 360)

val MP3_QUALITY_PRESETS = listOf(
    OutputQuality.Audio("Best (~320kbps)", "0"),
    OutputQuality.Audio("Medium (192kbps)", "192K"),
    OutputQuality.Audio("Low (128kbps)", "128K"),
)
