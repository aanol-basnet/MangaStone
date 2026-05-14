package com.personal.mangarock.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.di.ServerUrlHolder
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
    val readingDirection: String = "VERTICAL",
    val serverUrl: String = "http://192.168.1.65:3000/"
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val serverUrlHolder: ServerUrlHolder,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(prefs.theme, prefs.updateIntervalHours, prefs.dataSaver, prefs.titleLanguage, prefs.readingDirection) {
            theme, interval, dataSaver, titleLanguage, readingDirection ->
            listOf(theme, interval.toString(), dataSaver.toString(), titleLanguage, readingDirection)
        },
        prefs.serverUrl
    ) { first, serverUrl ->
        SettingsUiState(
            theme = first[0],
            updateIntervalHours = first[1].toLong(),
            dataSaver = first[2].toBoolean(),
            cacheSize = getCacheSize(),
            titleLanguage = first[3],
            readingDirection = first[4],
            serverUrl = serverUrl
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
    fun setServerUrl(url: String) = viewModelScope.launch {
        // Normalize: ensure http:// prefix and trailing slash
        val normalized = url.trim()
            .let { if (!it.startsWith("http://") && !it.startsWith("https://")) "http://$it" else it }
            .trimEnd('/') + "/"
        prefs.setServerUrl(normalized)
        serverUrlHolder.url = normalized  // takes effect immediately for all new requests
    }

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
