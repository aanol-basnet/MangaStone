package com.personal.mangastone.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.mangastone.data.local.entities.DownloadStatus
import com.personal.mangastone.domain.models.Chapter
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.ReadChapter
import com.personal.mangastone.ui.theme.TextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChapterListItem(
    chapter: Chapter,
    isRead: Boolean,
    /** null = never queued; otherwise reflects current download state */
    downloadStatus: DownloadStatus?,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textColor = if (isRead) ReadChapter else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append("Chapter ")
                    append(chapter.chapterNumber ?: "?")
                    if (!chapter.title.isNullOrBlank()) append(" — ${chapter.title}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
            Text(
                text = "${chapter.scanlationGroup} · ${chapter.publishAt.toRelativeTime()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        // Download icon / progress indicator
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            when (downloadStatus) {
                DownloadStatus.COMPLETED -> IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = Primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
                DownloadStatus.QUEUED -> CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Primary.copy(alpha = 0.4f),
                    strokeWidth = 2.dp
                )
                DownloadStatus.FAILED -> IconButton(onClick = onDownloadClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Retry download",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
                null -> IconButton(onClick = onDownloadClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun String.toRelativeTime(): String {
    return try {
        val instant = Instant.parse(this)
        val now = Instant.now()
        val diffSeconds = now.epochSecond - instant.epochSecond
        when {
            diffSeconds < 60 -> "just now"
            diffSeconds < 3600 -> "${diffSeconds / 60}m ago"
            diffSeconds < 86400 -> "${diffSeconds / 3600}h ago"
            diffSeconds < 604800 -> "${diffSeconds / 86400}d ago"
            else -> DateTimeFormatter.ofPattern("MMM d, yyyy")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    } catch (e: Exception) {
        this
    }
}
