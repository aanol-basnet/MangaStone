package com.personal.mangarock.data.repository

import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.ReadingProgress
import kotlinx.coroutines.flow.Flow

interface ChapterRepository {
    suspend fun getMangaChapters(mangaId: String, offset: Int = 0): Result<List<Chapter>>
    suspend fun getChapterImageUrls(chapterId: String, dataSaver: Boolean): Result<List<String>>
    suspend fun saveProgress(progress: ReadingProgress)
    suspend fun getProgress(chapterId: String): ReadingProgress?
    suspend fun getLastReadForManga(mangaId: String): ReadingProgress?
    fun getReadChapterIds(mangaId: String): Flow<Set<String>>
}
