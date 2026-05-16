package com.personal.mangastone.ui.screens.reader

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangastone.data.repository.ChapterRepository
import com.personal.mangastone.data.repository.FavoritesRepository
import com.personal.mangastone.domain.models.Chapter
import com.personal.mangastone.domain.models.ReadingProgress
import com.personal.mangastone.ui.screens.settings.AppPreferences
import com.personal.mangastone.workers.DownloadWorker
import com.personal.mangastone.workers.toSafeDirName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import java.io.File
import javax.inject.Inject

enum class ReadingDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL }
enum class ReaderBackground { BLACK, WHITE, SEPIA }

data class ReaderSettings(
    val direction: ReadingDirection = ReadingDirection.VERTICAL,
    val background: ReaderBackground = ReaderBackground.BLACK,
    val dataSaver: Boolean = false,
    val zoomLock: Boolean = false
)

sealed class PageItem {
    data class MangaPage(val url: String) : PageItem()
    data class ChapterDivider(val endNumber: String?, val startNumber: String?) : PageItem()
}

// Tracks one chapter's slot range within the flat pageItems list
data class ChapterSegment(
    val chapterId: String,
    val chapterNumber: String?,
    val chapterTitle: String?,
    val startSlotIndex: Int,  // index of first MangaPage in pageItems
    val pageCount: Int
)

sealed class ReaderUiState {
    object Loading : ReaderUiState()
    data class Success(
        val pageItems: List<PageItem>,
        val currentSlot: Int = 0,
        val currentChapterNumber: String? = null,
        val currentChapterTitle: String? = null,
        val currentPageInChapter: Int = 0,
        val currentChapterTotalPages: Int = 0,
        val currentChapterStartSlot: Int = 0,
        val settings: ReaderSettings = ReaderSettings(),
        val showOverlay: Boolean = false
    ) : ReaderUiState()
    data class Error(val message: String) : ReaderUiState()
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val chapterRepository: ChapterRepository,
    private val favoritesRepository: FavoritesRepository,
    private val prefs: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var sortedChapters: List<Chapter> = emptyList()
    private var currentMangaId = ""
    private var firstChapterId = ""
    private var chapterSegments: List<ChapterSegment> = emptyList()
    private var isLoadingNextChapter = false

    fun load(chapterId: String, mangaId: String) {
        if (firstChapterId == chapterId && _uiState.value is ReaderUiState.Success) return
        firstChapterId = chapterId
        currentMangaId = mangaId
        viewModelScope.launch {
            if (sortedChapters.isEmpty() || currentMangaId != mangaId) {
                chapterRepository.getMangaChapters(mangaId).onSuccess { chapters ->
                    sortedChapters = chapters.sortedWith(compareByDescending {
                        it.chapterNumber?.toFloatOrNull() ?: Float.MIN_VALUE
                    })
                }
            }
            loadInitialChapter(chapterId)
        }
    }

    private suspend fun loadInitialChapter(chapterId: String) {
        val settings = (_uiState.value as? ReaderUiState.Success)?.settings ?: run {
            val dirStr = prefs.readingDirection.first()
            val direction = try { ReadingDirection.valueOf(dirStr) } catch (e: Exception) { ReadingDirection.VERTICAL }
            ReaderSettings(direction = direction, dataSaver = prefs.dataSaver.first())
        }
        _uiState.value = ReaderUiState.Loading

        val savedProgress = chapterRepository.getProgress(chapterId)
        val chapterIndex = sortedChapters.indexOfFirst { it.id == chapterId }
        val chapter = sortedChapters.getOrNull(chapterIndex)

        val pages: List<String> = loadLocalPages(chapterId)
            ?: chapterRepository.getChapterImageUrls(chapterId, settings.dataSaver).getOrElse {
                _uiState.value = ReaderUiState.Error(it.message ?: "Failed to load pages")
                return
            }

        val initialPage = savedProgress?.lastPage ?: 0
        chapterSegments = listOf(
            ChapterSegment(
                chapterId = chapterId,
                chapterNumber = chapter?.chapterNumber,
                chapterTitle = chapter?.title,
                startSlotIndex = 0,
                pageCount = pages.size
            )
        )

        _uiState.value = ReaderUiState.Success(
            pageItems = pages.map { PageItem.MangaPage(it) },
            currentSlot = initialPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            currentChapterNumber = chapter?.chapterNumber,
            currentChapterTitle = chapter?.title,
            currentPageInChapter = initialPage.coerceIn(0, (pages.size - 1).coerceAtLeast(0)),
            currentChapterTotalPages = pages.size,
            currentChapterStartSlot = 0,
            settings = settings
        )
    }

    // Called by the screen on every slot change (pager page or list scroll position)
    fun onSlotChanged(slotIndex: Int) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        if (slotIndex < 0 || slotIndex >= state.pageItems.size) return

        // On a divider — just track position, don't update chapter progress
        if (state.pageItems[slotIndex] is PageItem.ChapterDivider) {
            _uiState.value = state.copy(currentSlot = slotIndex)
            return
        }

        val segment = chapterSegments.lastOrNull { it.startSlotIndex <= slotIndex } ?: return
        val pageInChapter = slotIndex - segment.startSlotIndex

        _uiState.value = state.copy(
            currentSlot = slotIndex,
            currentChapterNumber = segment.chapterNumber,
            currentChapterTitle = segment.chapterTitle,
            currentPageInChapter = pageInChapter,
            currentChapterTotalPages = segment.pageCount,
            currentChapterStartSlot = segment.startSlotIndex
        )

        viewModelScope.launch {
            chapterRepository.saveProgress(
                ReadingProgress(
                    chapterId = segment.chapterId,
                    mangaId = currentMangaId,
                    lastPage = pageInChapter,
                    totalPages = segment.pageCount,
                    chapterNumber = segment.chapterNumber,
                    readAt = System.currentTimeMillis()
                )
            )
            favoritesRepository.updateLastRead(currentMangaId, segment.chapterId, segment.chapterNumber)
        }

        // Pre-load next chapter when within 3 pages of this segment's end
        val pagesLeft = segment.pageCount - 1 - pageInChapter
        if (pagesLeft <= 3) {
            triggerNextChapterLoad(state.settings)
        }
    }

    private fun triggerNextChapterLoad(settings: ReaderSettings) {
        if (isLoadingNextChapter) return
        val lastSegment = chapterSegments.lastOrNull() ?: return
        val lastChapterIndex = sortedChapters.indexOfFirst { it.id == lastSegment.chapterId }
        val nextChapter = sortedChapters.getOrNull(lastChapterIndex - 1) ?: return
        // Already loaded?
        if (chapterSegments.any { it.chapterId == nextChapter.id }) return

        isLoadingNextChapter = true
        viewModelScope.launch {
            val nextPages: List<String>? = loadLocalPages(nextChapter.id)
                ?: chapterRepository.getChapterImageUrls(nextChapter.id, settings.dataSaver).getOrNull()

            isLoadingNextChapter = false

            if (nextPages.isNullOrEmpty()) return@launch
            val currentState = _uiState.value as? ReaderUiState.Success ?: return@launch

            val dividerSlot = currentState.pageItems.size
            val nextStartSlot = dividerSlot + 1
            val divider = PageItem.ChapterDivider(lastSegment.chapterNumber, nextChapter.chapterNumber)
            val nextItems = nextPages.map { PageItem.MangaPage(it) }

            chapterSegments = chapterSegments + ChapterSegment(
                chapterId = nextChapter.id,
                chapterNumber = nextChapter.chapterNumber,
                chapterTitle = nextChapter.title,
                startSlotIndex = nextStartSlot,
                pageCount = nextPages.size
            )
            _uiState.value = currentState.copy(
                pageItems = currentState.pageItems + divider + nextItems
            )
        }
    }

    fun toggleOverlay() {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(showOverlay = !state.showOverlay)
    }

    fun updateSettings(settings: ReaderSettings) {
        val state = _uiState.value as? ReaderUiState.Success ?: return
        _uiState.value = state.copy(settings = settings)
        viewModelScope.launch {
            prefs.setReadingDirection(settings.direction.name)
            // Reload current chapter with new data-saver setting
            val segment = chapterSegments.firstOrNull() ?: return@launch
            chapterRepository.getChapterImageUrls(segment.chapterId, settings.dataSaver).onSuccess { urls ->
                (_uiState.value as? ReaderUiState.Success)?.let { s ->
                    // Reset to first chapter only; chapters will re-append as user reads
                    val pages = urls.map { PageItem.MangaPage(it) as PageItem }
                    chapterSegments = listOf(chapterSegments.first())
                    isLoadingNextChapter = false
                    _uiState.value = s.copy(pageItems = pages)
                }
            }
        }
    }

    fun retry() {
        firstChapterId = ""
        viewModelScope.launch { loadInitialChapter(chapterSegments.firstOrNull()?.chapterId ?: return@launch) }
    }

    private fun loadLocalPages(chapterId: String): List<String>? {
        val dir = DownloadWorker.chapterDir(context, chapterId)
        return if (dir.exists()) {
            dir.listFiles()
                ?.filter { it.isFile && it.extension == "jpg" }
                ?.sortedBy { it.nameWithoutExtension.toIntOrNull() ?: 0 }
                ?.map { it.toUri().toString() }
                ?.takeIf { it.isNotEmpty() }
        } else null
    }
}
