package com.personal.mangastone.data.source

import com.personal.mangastone.domain.models.Chapter
import com.personal.mangastone.domain.models.Manga

data class SourcePage(val manga: List<Manga>, val hasNextPage: Boolean)

interface MangaSource {
    val id: String
    val name: String
    suspend fun getPopularManga(page: Int): SourcePage
    suspend fun searchManga(query: String, page: Int): SourcePage
    suspend fun getMangaDetail(mangaId: String): Manga
    suspend fun getChapterList(mangaId: String): List<Chapter>
    suspend fun getPageList(chapterId: String): List<String>
}
