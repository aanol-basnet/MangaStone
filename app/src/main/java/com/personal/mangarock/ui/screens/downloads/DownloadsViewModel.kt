package com.personal.mangarock.ui.screens.downloads

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.data.local.dao.DownloadDao
import com.personal.mangarock.data.local.entities.DownloadEntity
import com.personal.mangarock.workers.DownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class DownloadsUiState(
    val grouped: Map<String, List<DownloadEntity>> = emptyMap()
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<DownloadsUiState> = downloadDao.getAllDownloads()
        .map { list ->
            DownloadsUiState(grouped = list.groupBy { it.mangaTitle })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DownloadsUiState())

    fun deleteDownload(chapterId: String) {
        viewModelScope.launch {
            val chapterDir = File(context.getExternalFilesDir(null), "chapters/$chapterId")
            chapterDir.deleteRecursively()
            downloadDao.deleteDownload(chapterId)
        }
    }

    fun retryDownload(chapterId: String, mangaId: String) {
        DownloadWorker.enqueue(context, chapterId, mangaId)
    }

    fun getStorageSize(chapterId: String): Long {
        val dir = File(context.getExternalFilesDir(null), "chapters/$chapterId")
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
