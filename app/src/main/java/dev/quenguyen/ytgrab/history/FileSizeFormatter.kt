package dev.quenguyen.ytgrab.history

import java.util.Locale

/**
 * Formats a byte count as a human-readable MB string for display.
 * Kept free of Android types so it can be unit tested on the JVM.
 */
object FileSizeFormatter {

    fun formatMb(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
