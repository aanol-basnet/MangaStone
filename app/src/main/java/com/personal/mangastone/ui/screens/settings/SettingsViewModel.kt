package com.personal.mangastone.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangastone.data.backup.ImportResult
import com.personal.mangastone.data.backup.LibraryBackupManager
import com.personal.mangastone.update.UpdateChecker
import com.personal.mangastone.update.UpdateResult
import com.personal.mangastone.update.VersionInfo
import com.personal.mangastone.workers.ChapterUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val updateIntervalHours: Long = 6L,
    val dataSaver: Boolean = false,
    val cacheSize: Long = 0L,
    val titleLanguage: String = "en",
    val readingDirection: String = "VERTICAL"
)

sealed class BackupState {
    object Idle : BackupState()
    object Exporting : BackupState()
    data class ExportReady(val intent: Intent) : BackupState()
    object Importing : BackupState()
    data class ImportDone(val message: String) : BackupState()
    data class Error(val message: String) : BackupState()
}

sealed class AppUpdateState {
    object Idle : AppUpdateState()
    object Checking : AppUpdateState()
    data class Available(val info: VersionInfo) : AppUpdateState()
    object UpToDate : AppUpdateState()
    data class Downloading(val progress: Int) : AppUpdateState()
    data class ReadyToInstall(val file: File) : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val updateChecker: UpdateChecker,
    private val backupManager: LibraryBackupManager,
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

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    // Must match versionName in build.gradle.kts
    private val currentVersion = "1.1.1"

    fun checkForUpdate() {
        if (_updateState.value is AppUpdateState.Checking) return
        _updateState.value = AppUpdateState.Checking
        viewModelScope.launch {
            _updateState.value = when (val result = updateChecker.checkForUpdate(currentVersion)) {
                is UpdateResult.Available -> AppUpdateState.Available(result.info)
                is UpdateResult.UpToDate  -> AppUpdateState.UpToDate
                is UpdateResult.Error     -> AppUpdateState.Error(result.message)
            }
        }
    }

    fun downloadUpdate(info: VersionInfo) {
        _updateState.value = AppUpdateState.Downloading(0)
        viewModelScope.launch {
            val file = updateChecker.downloadApk(info.apkUrl) { progress ->
                _updateState.value = AppUpdateState.Downloading(progress)
            }
            _updateState.value = if (file != null) {
                AppUpdateState.ReadyToInstall(file)
            } else {
                AppUpdateState.Error("Download failed")
            }
        }
    }

    fun installUpdate(file: File) {
        updateChecker.triggerInstall(file)
    }

    fun dismissUpdate() {
        _updateState.value = AppUpdateState.Idle
    }

    // ── Library backup ────────────────────────────────────────────────────────

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState.asStateFlow()

    fun exportLibrary() {
        if (_backupState.value is BackupState.Exporting) return
        _backupState.value = BackupState.Exporting
        viewModelScope.launch {
            try {
                val uri    = backupManager.export()
                val intent = backupManager.shareIntent(uri)
                _backupState.value = BackupState.ExportReady(intent)
            } catch (e: Exception) {
                _backupState.value = BackupState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun importLibrary(stream: InputStream) {
        _backupState.value = BackupState.Importing
        viewModelScope.launch {
            _backupState.value = when (val result = backupManager.import(stream)) {
                is ImportResult.Success -> BackupState.ImportDone(
                    "Restored ${result.favoritesRestored} favorites and ${result.chaptersRestored} chapters"
                )
                is ImportResult.Error   -> BackupState.Error(result.message)
            }
        }
    }

    fun dismissBackupState() { _backupState.value = BackupState.Idle }

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
