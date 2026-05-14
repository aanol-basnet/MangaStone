package com.personal.mangarock.data.repository

import com.personal.mangarock.data.local.entities.FavoriteEntity
import com.personal.mangarock.domain.models.Manga
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getAllFavorites(): Flow<List<FavoriteEntity>>
    fun isFavorite(mangaId: String): Flow<Boolean>
    suspend fun addFavorite(manga: Manga)
    suspend fun removeFavorite(mangaId: String)
    suspend fun updateLastRead(mangaId: String, chapterId: String, chapterNumber: String?)
}
