package com.personal.mangarock.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.data.local.entities.FavoriteEntity
import com.personal.mangarock.data.repository.FavoritesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesUiState(
    val allFavorites: List<FavoriteEntity> = emptyList(),
    val hasUpdates: List<FavoriteEntity> = emptyList()
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepository: FavoritesRepository
) : ViewModel() {

    val uiState: StateFlow<FavoritesUiState> = favoritesRepository.getAllFavorites()
        .map { list ->
            FavoritesUiState(
                allFavorites = list,
                hasUpdates = emptyList() // populated by WorkManager check
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FavoritesUiState())

    fun removeFavorite(mangaId: String) {
        viewModelScope.launch {
            favoritesRepository.removeFavorite(mangaId)
        }
    }
}
