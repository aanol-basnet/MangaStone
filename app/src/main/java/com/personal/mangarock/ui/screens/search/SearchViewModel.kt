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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val recentSearches: StateFlow<List<SearchHistoryEntity>> = searchHistoryDao
        .getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: Flow<PagingData<Manga>> = _query
        .debounce(400)
        .distinctUntilChanged()
        .filter { it.length >= 2 }
        .flatMapLatest { query -> mangaRepository.searchManga(query) }
        .cachedIn(viewModelScope)

    fun onQueryChange(q: String) { _query.value = q }

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
