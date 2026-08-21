package dev.quenguyen.ytgrab.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_history")
data class DownloadHistoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceUrl: String,
    val format: String,
    val qualityLabel: String,
    val contentUri: String,
    val completedAtMillis: Long,
)
