package com.personal.mangastone.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.personal.mangastone.data.paging.GenrePagingSource
import com.personal.mangastone.data.paging.SourcePagingSource
import com.personal.mangastone.data.scraper.MangaHereScraper
import com.personal.mangastone.domain.models.Manga
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MangaRepositoryImpl @Inject constructor(
    private val scraper: MangaHereScraper
) : MangaRepository {

    override fun getBrowseManga(): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            SourcePagingSource(scraper, null)
        }.flow

    override fun searchManga(query: String): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            SourcePagingSource(scraper, query)
        }.flow

    override fun getMangaByGenre(genre: String): Flow<PagingData<Manga>> =
        Pager(PagingConfig(pageSize = 24, enablePlaceholders = false)) {
            GenrePagingSource(scraper, genre)
        }.flow

    override suspend fun getManga(id: String): Result<Manga> = runCatching {
        scraper.getMangaDetail(id)
    }

    override fun getPopularManga(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { scraper.getPopularManga(1).manga })
    }

    override fun getNewReleases(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { scraper.getNewestManga(1).manga })
    }

    override fun getCompletedManga(): Flow<Result<List<Manga>>> = flow {
        emit(runCatching { scraper.getCompletedManga(1).manga })
    }
}
