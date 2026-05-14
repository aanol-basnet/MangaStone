package com.personal.mangarock.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.ui.theme.ReadChapter
import com.personal.mangarock.ui.theme.TextMuted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ChapterListItem(
    chapter: Chapter,
    isRead: Boolean,
    isDownloaded: Boolean,
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
        IconButton(onClick = onDownloadClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                contentDescription = if (isDownloaded) "Downloaded" else "Download",
                tint = if (isDownloaded) MaterialTheme.colorScheme.primary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
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
