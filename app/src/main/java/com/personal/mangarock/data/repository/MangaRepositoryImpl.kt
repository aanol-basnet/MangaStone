package com.personal.mangarock.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.personal.mangarock.data.paging.GenrePagingSource
import com.personal.mangarock.data.paging.SourcePagingSource
import com.personal.mangarock.data.source.MangaHookSource
import com.personal.mangarock.domain.models.Manga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
    private val mangaHookSource: MangaHookSource
) : MangaRepository {

    override fun getBrowseManga(): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            SourcePagingSource(mangaHookSource, null)
        }.flow

    override fun searchManga(query: String): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            SourcePagingSource(mangaHookSource, query)
        }.flow

    override fun getMangaByGenre(genre: String): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            GenrePagingSource(mangaHookSource, genre)
        }.flow

    override suspend fun getManga(id: String): Result<Manga> = runCatching {
        mangaHookSource.getMangaDetail(id)
    }

    override fun getPopularManga(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { mangaHookSource.getPopularManga(1).manga })
    }

    override fun getNewReleases(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { mangaHookSource.getNewestManga(1).manga })
    }

    override fun getCompletedManga(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { mangaHookSource.getCompletedManga(1).manga })
    }
}
