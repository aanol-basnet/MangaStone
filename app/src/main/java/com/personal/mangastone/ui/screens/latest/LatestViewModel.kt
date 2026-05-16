package com.personal.mangastone.ui.screens.latest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangastone.data.repository.MangaRepository
import com.personal.mangastone.domain.models.Manga
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LatestUiState {
    object Loading : LatestUiState()
    data class Success(val manga: List<Manga>) : LatestUiState()
    data class Error(val message: String) : LatestUiState()
}

@HiltViewModel
class LatestViewModel @Inject constructor(
    private val mangaRepository: MangaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LatestUiState>(LatestUiState.Loading)
    val uiState: StateFlow<LatestUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = LatestUiState.Loading
            mangaRepository.getNewReleases().collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = LatestUiState.Success(it) },
                    onFailure = { _uiState.value = LatestUiState.Error(it.message ?: "Error") }
                )
            }
        }
    }
}
