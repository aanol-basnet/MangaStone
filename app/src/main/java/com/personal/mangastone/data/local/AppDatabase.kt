package com.personal.mangastone.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.personal.mangastone.data.local.dao.DownloadDao
import com.personal.mangastone.data.local.dao.FavoriteDao
import com.personal.mangastone.data.local.dao.NotificationDao
import com.personal.mangastone.data.local.dao.ReadingProgressDao
import com.personal.mangastone.data.local.dao.SearchHistoryDao
import com.personal.mangastone.data.local.entities.DownloadEntity
import com.personal.mangastone.data.local.entities.FavoriteEntity
import com.personal.mangastone.data.local.entities.NotificationEntity
import com.personal.mangastone.data.local.entities.ReadingProgressEntity
import com.personal.mangastone.data.local.entities.SearchHistoryEntity

@Database(
    entities = [
        FavoriteEntity::class,
        ReadingProgressEntity::class,
        DownloadEntity::class,
        SearchHistoryEntity::class,
        NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoriteDao(): FavoriteDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun downloadDao(): DownloadDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE favorites ADD COLUMN latestKnownChapterNumber TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mangaId TEXT NOT NULL,
                        mangaTitle TEXT NOT NULL,
                        mangaCoverUrl TEXT NOT NULL,
                        chapterId TEXT NOT NULL,
                        chapterNumber TEXT NOT NULL,
                        isRead INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "MangaStone.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { INSTANCE = it }
            }
    }
}
