package com.personal.mangarock.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.workers.ChapterUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val updateIntervalHours: Long = 6L,
    val dataSaver: Boolean = false,
    val cacheSize: Long = 0L,
    val titleLanguage: String = "en",
    val readingDirection: String = "VERTICAL"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.theme,
        prefs.updateIntervalHours,
        prefs.dataSaver,
        prefs.titleLanguage,
        prefs.readingDirection
    ) { theme, interval, dataSaver, titleLanguage, readingDirection ->
        SettingsUiState(
            theme = theme,
            updateIntervalHours = interval,
            dataSaver = dataSaver,
            cacheSize = getCacheSize(),
            titleLanguage = titleLanguage,
            readingDirection = readingDirection
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setTheme(theme: String) = viewModelScope.launch { prefs.setTheme(theme) }
    fun setUpdateInterval(hours: Long) = viewModelScope.launch {
        prefs.setUpdateInterval(hours)
        ChapterUpdateWorker.schedule(context, hours)
    }
    fun setDataSaver(enabled: Boolean) = viewModelScope.launch { prefs.setDataSaver(enabled) }
    fun setTitleLanguage(language: String) = viewModelScope.launch { prefs.setTitleLanguage(language) }
    fun setReadingDirection(direction: String) = viewModelScope.launch { prefs.setReadingDirection(direction) }

    fun clearCache() = viewModelScope.launch {
        context.cacheDir.deleteRecursively()
        context.externalCacheDir?.deleteRecursively()
    }

    private fun getCacheSize(): Long {
        val cacheSize = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val extCacheSize = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        return cacheSize + extCacheSize
    }
}
