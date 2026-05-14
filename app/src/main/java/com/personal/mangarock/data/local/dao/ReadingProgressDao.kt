package com.personal.mangarock.data.local.dao

import androidx.room.*
import com.personal.mangarock.data.local.entities.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingProgressDao {

    @Query("SELECT * FROM reading_progress WHERE chapterId = :chapterId")
    suspend fun getProgress(chapterId: String): ReadingProgressEntity?

    @Query("SELECT * FROM reading_progress WHERE mangaId = :mangaId ORDER BY readAt DESC LIMIT 1")
    suspend fun getLastReadForManga(mangaId: String): ReadingProgressEntity?

    @Query("SELECT chapterId FROM reading_progress WHERE mangaId = :mangaId")
    fun getReadChapterIds(mangaId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE mangaId = :mangaId")
    suspend fun clearMangaProgress(mangaId: String)
}
