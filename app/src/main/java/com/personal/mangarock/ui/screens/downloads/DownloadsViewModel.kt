package com.personal.mangarock.ui.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.data.local.dao.DownloadDao
import com.personal.mangarock.data.local.entities.DownloadEntity
import com.personal.mangarock.data.local.entities.DownloadStatus
import com.personal.mangarock.workers.DownloadWorker
import com.personal.mangarock.workers.toSafeDirName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadsUiState(
    val grouped: Map<String, List<DownloadEntity>> = emptyMap(),
    val activeCount: Int = 0   // QUEUED + DOWNLOADING
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = downloadDao.getAllDownloads()
        .map { list ->
            DownloadsUiState(
                grouped = list.groupBy { it.mangaTitle },
                activeCount = list.count {
                    it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsUiState())

    fun deleteDownload(chapterId: String) {
        viewModelScope.launch {
            DownloadWorker.chapterDir(context, chapterId).deleteRecursively()
            downloadDao.deleteDownload(chapterId)
        }
    }

    fun retryDownload(chapterId: String) {
        DownloadWorker.enqueue(context, chapterId)
    }

    fun getStorageSize(chapterId: String): Long =
        DownloadWorker.chapterDir(context, chapterId)
            .walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
