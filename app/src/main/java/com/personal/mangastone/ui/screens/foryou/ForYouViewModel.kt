package com.personal.mangastone.ui.screens.foryou

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangastone.data.local.dao.NotificationDao
import com.personal.mangastone.data.repository.MangaRepository
import com.personal.mangastone.domain.models.Manga
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SectionState {
    object Loading : SectionState()
    data class Success(val items: List<Manga>) : SectionState()
    data class Error(val message: String) : SectionState()
}

data class ForYouUiState(
    val popular: SectionState = SectionState.Loading,
    val newReleases: SectionState = SectionState.Loading,
    val completed: SectionState = SectionState.Loading,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class ForYouViewModel @Inject constructor(
    private val mangaRepository: MangaRepository,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForYouUiState())
    val uiState: StateFlow<ForYouUiState> = _uiState.asStateFlow()

    val unreadNotificationCount: StateFlow<Int> = notificationDao.getUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadAll()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        loadAll(isRefresh = true)
    }

    private fun loadAll(isRefresh: Boolean = false) {
        viewModelScope.launch {
            launch {
                mangaRepository.getPopularManga().collect { result ->
                    _uiState.value = _uiState.value.copy(
                        popular = result.fold(
                            onSuccess = { SectionState.Success(it) },
                            onFailure = { SectionState.Error(it.message ?: "Error") }
                        )
                    )
                }
            }
            launch {
                mangaRepository.getNewReleases().collect { result ->
                    _uiState.value = _uiState.value.copy(
                        newReleases = result.fold(
                            onSuccess = { SectionState.Success(it) },
                            onFailure = { SectionState.Error(it.message ?: "Error") }
                        )
                    )
                }
            }
            launch {
                mangaRepository.getCompletedManga().collect { result ->
                    _uiState.value = _uiState.value.copy(
                        completed = result.fold(
                            onSuccess = { SectionState.Success(it) },
                            onFailure = { SectionState.Error(it.message ?: "Error") }
                        ),
                        isRefreshing = false
                    )
                }
            }
        }
    }
}
