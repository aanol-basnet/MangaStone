package com.personal.mangastone.data.repository

import com.personal.mangastone.data.local.dao.FavoriteDao
import com.personal.mangastone.data.local.entities.FavoriteEntity
import com.personal.mangastone.domain.models.Manga
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoritesRepository {

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> =
        favoriteDao.getAllFavorites()

    override fun isFavorite(mangaId: String): Flow<Boolean> =
        favoriteDao.isFavorite(mangaId)

    override suspend fun addFavorite(manga: Manga) {
        favoriteDao.insertFavorite(
            FavoriteEntity(
                mangaId = manga.id,
                title = manga.title,
                coverUrl = manga.coverUrl,
                status = manga.status
            )
        )
    }

    override suspend fun removeFavorite(mangaId: String) {
        favoriteDao.deleteFavoriteById(mangaId)
    }

    override suspend fun updateLastRead(
        mangaId: String,
        chapterId: String,
        chapterNumber: String?
    ) {
        favoriteDao.updateLastRead(mangaId, System.currentTimeMillis(), chapterId, chapterNumber)
    }
}
