package dev.quenguyen.ytgrab.history

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadHistoryDao {

    @Insert
    suspend fun insert(entity: DownloadHistoryEntity)

    @Query("SELECT * FROM download_history ORDER BY completedAtMillis DESC")
    fun observeAll(): Flow<List<DownloadHistoryEntity>>

    @Delete
    suspend fun delete(entity: DownloadHistoryEntity)

    @Query("DELETE FROM download_history")
    suspend fun clearAll()
}
