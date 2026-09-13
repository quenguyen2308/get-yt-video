package dev.quenguyen.ytgrab.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadHistoryEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun downloadHistoryDao(): DownloadHistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ytgrab.db",
                )
                    // Personal sideloaded app with no user-facing export of this data — destructive
                    // migration (drop + recreate) is simpler than a real Migration for the
                    // fileSizeBytes column added in version 2.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
