package com.personal.mangastone.data.local.dao

import androidx.room.*
import com.personal.mangastone.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY lastReadAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites ORDER BY lastReadAt DESC")
    suspend fun getAllFavoritesOnce(): List<FavoriteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<FavoriteEntity>)

    @Query("SELECT * FROM favorites WHERE mangaId = :mangaId")
    suspend fun getFavorite(mangaId: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mangaId = :mangaId)")
    fun isFavorite(mangaId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Delete
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE mangaId = :mangaId")
    suspend fun deleteFavoriteById(mangaId: String)

    @Query("UPDATE favorites SET lastReadAt = :readAt, lastReadChapterId = :chapterId, lastReadChapterNumber = :chapterNumber WHERE mangaId = :mangaId")
    suspend fun updateLastRead(mangaId: String, readAt: Long, chapterId: String, chapterNumber: String?)

    @Query("UPDATE favorites SET latestKnownChapterNumber = :chapterNumber WHERE mangaId = :mangaId")
    suspend fun updateLatestKnownChapter(mangaId: String, chapterNumber: String)
}
