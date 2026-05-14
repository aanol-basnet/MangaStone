package com.personal.mangarock.ui.screens.mangainfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import com.personal.mangarock.data.repository.ChapterRepository
import com.personal.mangarock.data.repository.FavoritesRepository
import com.personal.mangarock.data.repository.MangaRepository
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.Manga
import dagger.hilt.android.lifecycle.HiltViewModel
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
        val chaptersNewestFirst: Boolean = true
    ) : MangaInfoUiState()
    data class Error(val message: String) : MangaInfoUiState()
}

@HiltViewModel
class MangaInfoViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val chapterRepository: ChapterRepository,
    private val favoritesRepository: FavoritesRepository
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

    fun retry(mangaId: String) {
        currentMangaId = ""
        load(mangaId)
    }
}
