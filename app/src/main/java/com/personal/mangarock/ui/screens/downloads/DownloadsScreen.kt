package com.personal.mangarock.ui.screens.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.mangarock.data.local.entities.DownloadEntity
import com.personal.mangarock.data.local.entities.DownloadStatus
import com.personal.mangarock.ui.components.EmptyState
import com.personal.mangarock.ui.theme.Primary
import com.personal.mangarock.ui.theme.Surface
import com.personal.mangarock.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onChapterClick: (chapterId: String, mangaId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.grouped.isEmpty()) {
            EmptyState(
                message = "No downloaded chapters.\nTap the download icon on any chapter.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(contentPadding = padding) {
                uiState.grouped.forEach { (mangaTitle, chapters) ->
                    item(key = "header_$mangaTitle") {
                        Text(
                            mangaTitle,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                    items(chapters.size) { index ->
                        DownloadChapterItem(
                            download = chapters[index],
                            onClick = {
                                if (chapters[index].status == DownloadStatus.COMPLETED) {
                                    onChapterClick(chapters[index].chapterId, chapters[index].mangaId)
                                }
                            },
                            onDelete = { viewModel.deleteDownload(chapters[index].chapterId) },
                            onRetry = { viewModel.retryDownload(chapters[index].chapterId, chapters[index].mangaId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadChapterItem(
    download: DownloadEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Chapter ${download.chapterNumber ?: "?"}${
                    if (!download.chapterTitle.isNullOrBlank()) " — ${download.chapterTitle}" else ""
                }",
                style = MaterialTheme.typography.bodyMedium
            )
            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    LinearProgressIndicator(
                        progress = { download.downloadedPages.toFloat() / download.totalPages.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        color = Primary
                    )
                    Text(
                        "${download.downloadedPages}/${download.totalPages} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                DownloadStatus.COMPLETED -> Text(
                    "${download.totalPages} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                DownloadStatus.FAILED -> TextButton(onClick = onRetry) {
                    Text("Retry", color = Primary)
                }
                DownloadStatus.QUEUED -> Text(
                    "Queued",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
        }
    }
}
