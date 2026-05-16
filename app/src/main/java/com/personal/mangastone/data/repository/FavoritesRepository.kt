package com.personal.mangastone.data.repository

import com.personal.mangastone.data.local.entities.FavoriteEntity
import com.personal.mangastone.domain.models.Manga
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    fun isFavorite(mangaId: String): Flow<Boolean>
    suspend fun addFavorite(manga: Manga)
    suspend fun removeFavorite(mangaId: String)
    suspend fun updateLastRead(mangaId: String, chapterId: String, chapterNumber: String?)
}
