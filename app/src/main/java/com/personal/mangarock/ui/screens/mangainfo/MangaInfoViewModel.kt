package com.personal.mangarock.ui.screens.mangainfo

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import com.personal.mangarock.data.local.dao.DownloadDao
import com.personal.mangarock.data.local.entities.DownloadEntity
import com.personal.mangarock.data.local.entities.DownloadStatus
import com.personal.mangarock.data.repository.ChapterRepository
import com.personal.mangarock.data.repository.FavoritesRepository
import com.personal.mangarock.data.repository.MangaRepository
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.Manga
import com.personal.mangarock.workers.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MangaInfoUiState {
    object Loading : MangaInfoUiState()
    data class Success(
        val manga: Manga,
        val chapters: List<Chapter>,
        val isFavorite: Boolean,
        val readChapterIds: Set<String>,
        val resumeChapterId: String?,
        val chaptersNewestFirst: Boolean = true,
        /** chapterId → current download status (only present if ever queued) */
        val downloadStatuses: Map<String, DownloadStatus> = emptyMap()
    ) : MangaInfoUiState()
    data class Error(val message: String) : MangaInfoUiState()
}

@HiltViewModel
class MangaInfoViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val favoritesRepository: FavoritesRepository,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<MangaInfoUiState>(MangaInfoUiState.Loading)
    val uiState: StateFlow<MangaInfoUiState> = _uiState.asStateFlow()

    private var currentMangaId: String = ""

    fun load(mangaId: String) {
        if (currentMangaId == mangaId && _uiState.value is MangaInfoUiState.Success) return
        currentMangaId = mangaId
        viewModelScope.launch {
            _uiState.value = MangaInfoUiState.Loading
            val mangaResult = mangaRepository.getManga(mangaId)
            val chaptersResult = chapterRepository.getMangaChapters(mangaId)
            if (mangaResult.isFailure) {
                _uiState.value = MangaInfoUiState.Error(mangaResult.exceptionOrNull()?.message ?: "Failed to load")
                return@launch
            }
            val manga = mangaResult.getOrThrow()
            val chapters = chaptersResult.getOrDefault(emptyList())
            val isFavorite = favoritesRepository.isFavorite(mangaId).first()
            val readProgress = chapterRepository.getLastReadForManga(mangaId)
            val readChapterIds = chapterRepository.getReadChapterIds(mangaId).first()
            _uiState.value = MangaInfoUiState.Success(
                manga = manga,
                chapters = chapters,
                isFavorite = isFavorite,
                readChapterIds = readChapterIds,
                resumeChapterId = readProgress?.chapterId
            )

            // Keep read state live as user reads chapters
            launch {
                chapterRepository.getReadChapterIds(mangaId).collectLatest { ids ->
                    (_uiState.value as? MangaInfoUiState.Success)?.let { s ->
                        _uiState.value = s.copy(readChapterIds = ids)
                    }
                }
            }

            // Keep download statuses live
            launch {
                downloadDao.getDownloadsForManga(mangaId).collectLatest { downloads ->
                    val statuses = downloads.associate { it.chapterId to it.status }
                    (_uiState.value as? MangaInfoUiState.Success)?.let { s ->
                        _uiState.value = s.copy(downloadStatuses = statuses)
                    }
                }
            }
        }
    }

    fun toggleFavorite() {
        val state = _uiState.value as? MangaInfoUiState.Success ?: return
        viewModelScope.launch {
            if (state.isFavorite) {
                favoritesRepository.removeFavorite(state.manga.id)
            } else {
                favoritesRepository.addFavorite(state.manga)
            }
            _uiState.value = state.copy(isFavorite = !state.isFavorite)
        }
    }

    fun toggleSortOrder() {
        val state = _uiState.value as? MangaInfoUiState.Success ?: return
        _uiState.value = state.copy(
            chapters = state.chapters.reversed(),
            chaptersNewestFirst = !state.chaptersNewestFirst
        )
    }

    /** Queue a single chapter for download. No-op if already completed or in progress. */
    fun downloadChapter(chapter: Chapter) {
        val state = _uiState.value as? MangaInfoUiState.Success ?: return
        val existingStatus = state.downloadStatuses[chapter.id]
        if (existingStatus == DownloadStatus.COMPLETED || existingStatus == DownloadStatus.DOWNLOADING) return
        viewModelScope.launch {
            downloadDao.insertDownload(
                DownloadEntity(
                    chapterId = chapter.id,
                    mangaId = chapter.mangaId,
                    mangaTitle = state.manga.title,
                    chapterNumber = chapter.chapterNumber,
                    chapterTitle = chapter.title,
                    totalPages = 0,
                    status = DownloadStatus.QUEUED
                )
            )
            DownloadWorker.enqueue(context, chapter.id)
        }
    }

    /** Queue every chapter that hasn't been downloaded yet. */
    fun downloadAllChapters() {
        val state = _uiState.value as? MangaInfoUiState.Success ?: return
        val toDownload = state.chapters.filter { chapter ->
            when (state.downloadStatuses[chapter.id]) {
                DownloadStatus.COMPLETED, DownloadStatus.DOWNLOADING, DownloadStatus.QUEUED -> false
                else -> true
            }
        }
        if (toDownload.isEmpty()) return
        viewModelScope.launch {
            toDownload.forEach { chapter ->
                downloadDao.insertDownload(
                    DownloadEntity(
                        chapterId = chapter.id,
                        mangaId = chapter.mangaId,
                        mangaTitle = state.manga.title,
                        chapterNumber = chapter.chapterNumber,
                        chapterTitle = chapter.title,
                        totalPages = 0,
                        status = DownloadStatus.QUEUED
                    )
                )
                DownloadWorker.enqueue(context, chapter.id)
            }
        }
    }

    fun retry(mangaId: String) {
        currentMangaId = ""
        load(mangaId)
    }
}
