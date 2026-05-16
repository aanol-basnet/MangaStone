package com.personal.mangarock.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.personal.mangarock.data.local.dao.SearchHistoryDao
import com.personal.mangarock.data.local.entities.SearchHistoryEntity
import com.personal.mangarock.data.repository.MangaRepository
import com.personal.mangarock.domain.models.Manga
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Genre(val name: String, val slug: String)

val GENRES = listOf(
    Genre("Action", "action"),
    Genre("Adventure", "adventure"),
    Genre("Comedy", "comedy"),
    Genre("Drama", "drama"),
    Genre("Fantasy", "fantasy"),
    Genre("Romance", "romance"),
    Genre("Supernatural", "supernatural"),
    Genre("Mystery", "mystery"),
    Genre("Horror", "horror"),
    Genre("Sci-fi", "sci-fi"),
    Genre("Slice of Life", "slice-of-life"),
    Genre("Sports", "sports"),
    Genre("Psychological", "psychological"),
    Genre("Historical", "historical"),
    Genre("Martial Arts", "martial-arts"),
    Genre("Shounen", "shounen"),
    Genre("Seinen", "seinen"),
    Genre("Shoujo", "shoujo"),
    Genre("Josei", "josei"),
    Genre("Mecha", "mecha"),
    Genre("Harem", "harem"),
    Genre("Ecchi", "ecchi"),
    Genre("Webtoon", "webtoons"),
    Genre("Tragedy", "tragedy"),
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedGenre = MutableStateFlow<Genre?>(null)
    val selectedGenre: StateFlow<Genre?> = _selectedGenre.asStateFlow()

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = searchHistoryDao
        .getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: Flow<PagingData<Manga>> = _query
        .debounce(400)
        .distinctUntilChanged()
        .filter { it.length >= 2 }
        .flatMapLatest { query -> mangaRepository.searchManga(query) }
        .cachedIn(viewModelScope)

    val genreResults: Flow<PagingData<Manga>> = _selectedGenre
        .filterNotNull()
        .distinctUntilChanged()
        .flatMapLatest { genre -> mangaRepository.getMangaByGenre(genre.slug) }
        .cachedIn(viewModelScope)

    fun onQueryChange(q: String) {
        _query.value = q
        if (q.length >= 2) _selectedGenre.value = null  // query takes over
    }

    fun selectGenre(genre: Genre) {
        if (_selectedGenre.value == genre) {
            _selectedGenre.value = null  // tap again to deselect
        } else {
            _selectedGenre.value = genre
            _query.value = ""  // clear search when browsing by genre
        }
    }

    fun submitSearch(q: String) {
        if (q.isBlank()) return
        viewModelScope.launch {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query = q))
        }
    }

    fun deleteHistory(query: String) {
        viewModelScope.launch { searchHistoryDao.deleteSearch(query) }
    }
}
