package com.personal.mangarock.data.repository

import androidx.paging.PagingData
import com.personal.mangarock.domain.models.Manga
import kotlinx.coroutines.flow.Flow

interface MangaRepository {
    fun getBrowseManga(): Flow<PagingData<Manga>>
    fun searchManga(query: String): Flow<PagingData<Manga>>
    suspend fun getManga(id: String): Result<Manga>
    fun getPopularManga(): Flow<Result<List<Manga>>>
    fun getNewReleases(): Flow<Result<List<Manga>>>
    fun getCompletedManga(): Flow<Result<List<Manga>>>
}
