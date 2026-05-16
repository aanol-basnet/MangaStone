package com.personal.mangastone.ui.screens.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.personal.mangastone.data.repository.MangaRepository
import com.personal.mangastone.domain.models.Manga
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val mangaRepository: MangaRepository
) : ViewModel() {

    val mangaPagingFlow: Flow<PagingData<Manga>> =
        mangaRepository.getBrowseManga().cachedIn(viewModelScope)
}
