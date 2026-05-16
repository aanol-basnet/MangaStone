package com.personal.mangastone.ui.screens.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.mangastone.data.local.entities.DownloadEntity
import com.personal.mangastone.data.local.entities.DownloadStatus
import com.personal.mangastone.ui.components.EmptyState
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.Surface
import com.personal.mangastone.ui.theme.TextMuted

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.grouped.isEmpty()) {
            EmptyState(
                message = "No downloaded chapters.\nTap ↓ on any chapter to download it.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(contentPadding = padding) {

                // Active queue summary banner
                if (uiState.activeCount > 0) {
                    item {
                        ActiveDownloadsBanner(count = uiState.activeCount)
                    }
                }

                // Per-manga groups
                uiState.grouped.forEach { (mangaTitle, chapters) ->
                    item(key = "header_$mangaTitle") {
                        Text(
                            mangaTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 16.dp, bottom = 4.dp
                            )
                        )
                    }
                    items(chapters.size, key = { chapters[it].chapterId }) { index ->
                        val chapter = chapters[index]
                        DownloadChapterItem(
                            download = chapter,
                            onClick = {
                                if (chapter.status == DownloadStatus.COMPLETED) {
                                    onChapterClick(chapter.chapterId, chapter.mangaId)
                                }
                            },
                            onDelete = { viewModel.deleteDownload(chapter.chapterId) },
                            onRetry = { viewModel.retryDownload(chapter.chapterId) }
                        )
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ActiveDownloadsBanner(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        color = Primary.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = Primary,
                strokeWidth = 2.dp
            )
            Text(
                text = "$count chapter${if (count > 1) "s" else ""} downloading…",
                style = MaterialTheme.typography.bodyMedium,
                color = Primary
            )
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
            .clickable(
                enabled = download.status == DownloadStatus.COMPLETED,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Chapter ${download.chapterNumber ?: "?"}${
                    if (!download.chapterTitle.isNullOrBlank()) " — ${download.chapterTitle}" else ""
                }",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            when (download.status) {
                DownloadStatus.DOWNLOADING -> {
                    val progress = if (download.totalPages > 0)
                        download.downloadedPages.toFloat() / download.totalPages
                    else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary,
                        trackColor = Primary.copy(alpha = 0.2f)
                    )
                    Text(
                        "${download.downloadedPages} / ${download.totalPages} pages",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                DownloadStatus.QUEUED -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary.copy(alpha = 0.4f),
                        trackColor = Primary.copy(alpha = 0.1f)
                    )
                    Text(
                        "Waiting in queue…",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                DownloadStatus.COMPLETED -> Text(
                    "${download.totalPages} pages  •  tap to read",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                DownloadStatus.FAILED -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Download failed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(
                        onClick = onRetry,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Retry", color = Primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}
