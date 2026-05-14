package com.personal.mangarock.data.source

import com.personal.mangarock.data.api.MangaHookApi
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.Manga
import com.personal.mangarock.domain.models.Tag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaHookSource @Inject constructor(
    private val api: MangaHookApi
) : MangaSource {

    override val id = "mangahook"
    override val name = "MangaHook"

    private fun String.toAbsoluteImageUrl(): String =
        if (startsWith("http")) this else "https:$this"

    override suspend fun getPopularManga(page: Int): SourcePage {
        val response = api.getMangaList(page = page, type = "topview")
        return SourcePage(response.mangaList.map { it.toManga() }, response.mangaList.size >= 24)
    }

    suspend fun getNewestManga(page: Int): SourcePage {
        val response = api.getMangaList(page = page, type = "newest")
        return SourcePage(response.mangaList.map { it.toManga() }, response.mangaList.size >= 24)
    }

    suspend fun getCompletedManga(page: Int): SourcePage {
        val response = api.getMangaList(page = page, type = "topview", state = "completed")
        return SourcePage(response.mangaList.map { it.toManga() }, response.mangaList.size >= 24)
    }

    override suspend fun searchManga(query: String, page: Int): SourcePage {
        val response = api.search(query = query, page = page)
        return SourcePage(response.mangaList.map { it.toManga() }, response.mangaList.size >= 24)
    }

    override suspend fun getMangaDetail(mangaId: String): Manga {
        val r = api.getManga(mangaId)
        val tags = r.genres.orEmpty().map { Tag(id = it.lowercase().replace(" ", "-"), name = it, group = "genre") }
        return Manga(
            id = mangaId,
            title = r.name,
            titleRomanji = null,
            coverUrl = r.imageUrl.toAbsoluteImageUrl(),
            description = "",
            status = r.status ?: "",
            tags = tags,
            author = r.author ?: "",
            year = null,
            lastChapter = null,
            latestChapterId = null,
            createdAt = r.updated ?: "",
            updatedAt = r.updated ?: ""
        )
    }

    override suspend fun getChapterList(mangaId: String): List<Chapter> {
        val r = api.getManga(mangaId)
        return r.chapterList.map { ch ->
            // Encode mangaId into chapterId so getPageList can split it back out
            val compositeId = "$mangaId::${ch.id}"
            Chapter(
                id = compositeId,
                mangaId = mangaId,
                title = ch.name.ifBlank { null },
                chapterNumber = ch.id.substringAfterLast("/c")
                    .takeIf { it.isNotBlank() && it.toFloatOrNull() != null }
                    ?: ch.name.substringAfter("Ch.").substringBefore(" ").trim().ifBlank { null },
                volume = null,
                pages = 0,
                publishAt = ch.createdAt ?: "",
                scanlationGroup = "MangaKakalot",
                externalUrl = null
            )
        }
    }

    override suspend fun getPageList(chapterId: String): List<String> {
        // chapterId is encoded as "mangaId::chapId"
        val (mangaId, chapId) = chapterId.split("::", limit = 2)
        val response = api.getChapterImages(mangaId, chapId)
        return response.images.map { it.image.toAbsoluteImageUrl() }
    }

    private fun com.personal.mangarock.data.api.models.MangaListItem.toManga() = Manga(
        id = id,
        title = title,
        titleRomanji = null,
        coverUrl = image.toAbsoluteImageUrl(),
        description = description ?: "",
        status = "",
        tags = emptyList(),
        author = "",
        year = null,
        lastChapter = chapter,
        latestChapterId = null,
        createdAt = "",
        updatedAt = ""
    )
}
