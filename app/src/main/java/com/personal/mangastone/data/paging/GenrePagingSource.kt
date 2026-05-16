package com.personal.mangastone.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.personal.mangastone.data.scraper.MangaHereScraper
import com.personal.mangastone.domain.models.Manga

class GenrePagingSource(
    private val source: MangaHereScraper,
    private val genre: String
) : PagingSource<Int, Manga>() {

    override fun getRefreshKey(state: PagingState<Int, Manga>): Int? =
        state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Manga> {
        val page = params.key ?: 1
        return try {
            val result = source.getMangaByGenre(genre, page)
            LoadResult.Page(
                data = result.manga,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (result.hasNextPage) page + 1 else null
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
