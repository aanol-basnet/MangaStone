package com.personal.mangarock.data.repository

import com.personal.mangarock.data.local.dao.ReadingProgressDao
import com.personal.mangarock.data.local.entities.ReadingProgressEntity
import com.personal.mangarock.data.source.MangaSource
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.ReadingProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChapterRepositoryImpl @Inject constructor(
    private val mangaHookSource: MangaSource,
    private val progressDao: ReadingProgressDao
) : ChapterRepository {

    override suspend fun getMangaChapters(mangaId: String, offset: Int): Result<List<Chapter>> =
        runCatching { mangaHookSource.getChapterList(mangaId) }

    override suspend fun getChapterImageUrls(
        chapterId: String,
        dataSaver: Boolean
    ): Result<List<String>> = runCatching { mangaHookSource.getPageList(chapterId) }

    override suspend fun saveProgress(progress: ReadingProgress) {
        progressDao.saveProgress(
            ReadingProgressEntity(
                chapterId = progress.chapterId,
                mangaId = progress.mangaId,
                lastPage = progress.lastPage,
                totalPages = progress.totalPages,
                chapterNumber = progress.chapterNumber,
                readAt = progress.readAt
            )
        )
    }

    override suspend fun getProgress(chapterId: String): ReadingProgress? =
        progressDao.getProgress(chapterId)?.let {
            ReadingProgress(it.chapterId, it.mangaId, it.lastPage, it.totalPages, it.chapterNumber, it.readAt)
        }

    override suspend fun getLastReadForManga(mangaId: String): ReadingProgress? =
        progressDao.getLastReadForManga(mangaId)?.let {
            ReadingProgress(it.chapterId, it.mangaId, it.lastPage, it.totalPages, it.chapterNumber, it.readAt)
        }

    override fun getReadChapterIds(mangaId: String): Flow<Set<String>> =
        progressDao.getReadChapterIds(mangaId).map { it.toSet() }
}
