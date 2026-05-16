package com.personal.mangarock.ui.screens.mangainfo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.personal.mangarock.data.local.entities.DownloadStatus
import com.personal.mangarock.domain.models.Chapter
import com.personal.mangarock.domain.models.Manga
import com.personal.mangarock.ui.components.ChapterListItem
import com.personal.mangarock.ui.components.ErrorState
import com.personal.mangarock.ui.components.GenreFilterChip
import com.personal.mangarock.ui.components.LoadingIndicator
import com.personal.mangarock.ui.components.MangaCoverCard
import com.personal.mangarock.ui.utils.displayTitle
import com.personal.mangarock.ui.theme.Background
import com.personal.mangarock.ui.theme.Primary
import com.personal.mangarock.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaInfoScreen(
    mangaId: String,
    onChapterClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: MangaInfoViewModel = hiltViewModel()
) {
    LaunchedEffect(mangaId) { viewModel.load(mangaId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Color.Transparent)
            )
        },
        containerColor = Background
    ) { padding ->
        when (val state = uiState) {
            is MangaInfoUiState.Loading -> LoadingIndicator(fullScreen = true)
            is MangaInfoUiState.Error -> ErrorState(
                message = state.message,
                onRetry = { viewModel.retry(mangaId) }
            )
            is MangaInfoUiState.Success -> {
                MangaInfoContent(
                    state = state,
                    onChapterClick = onChapterClick,
                    onTagClick = onTagClick,
                    onFavoriteToggle = viewModel::toggleFavorite,
                    onSortToggle = viewModel::toggleSortOrder,
                    onDownloadChapter = { chapter -> viewModel.downloadChapter(chapter) },
                    onDownloadAll = viewModel::downloadAllChapters
                )
            }
        }
    }
}

@Composable
private fun MangaInfoContent(
    state: MangaInfoUiState.Success,
    onChapterClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onFavoriteToggle: () -> Unit,
    onSortToggle: () -> Unit,
    onDownloadChapter: (Chapter) -> Unit,
    onDownloadAll: () -> Unit
) {
    val manga = state.manga
    var descriptionExpanded by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Hero header
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                // Blurred background
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(20.dp)
                )
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Background)))
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AsyncImage(
                        model = manga.coverUrl,
                        contentDescription = manga.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = manga.displayTitle(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = manga.author,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = manga.status.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary
                        )
                    }
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val chapterId = state.resumeChapterId ?: state.chapters.lastOrNull()?.id
                        chapterId?.let { onChapterClick(it) }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = state.chapters.isNotEmpty()
                ) {
                    Icon(
                        if (state.resumeChapterId != null) Icons.Default.PlayArrow else Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.resumeChapterId != null) "Resume" else "Start Reading")
                }
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .background(Surface, RoundedCornerShape(8.dp))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (state.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (state.isFavorite) Primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Description
        if (manga.description.isNotBlank()) {
            item {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { descriptionExpanded = !descriptionExpanded }
                        .animateContentSize()
                ) {
                    Text(
                        text = manga.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (descriptionExpanded) "Show less" else "Show more",
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary
                    )
                }
            }
        }

        // Tags / Genre chips
        if (manga.tags.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(manga.tags) { tag ->
                        GenreFilterChip(
                            label = tag.name,
                            selected = false,
                            onClick = { onTagClick(tag.id) }
                        )
                    }
                }
            }
        }

        // Chapter list header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${state.chapters.size} Chapters",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    // Download all undownloaded chapters
                    val allDownloaded = state.chapters.isNotEmpty() &&
                        state.chapters.all { state.downloadStatuses[it.id] == DownloadStatus.COMPLETED }
                    IconButton(onClick = onDownloadAll, enabled = !allDownloaded) {
                        Icon(
                            if (allDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            contentDescription = "Download all",
                            tint = if (allDownloaded) Primary else Primary.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = onSortToggle) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Toggle sort",
                            tint = Primary
                        )
                    }
                }
            }
        }

        // Chapters
        items(state.chapters) { chapter ->
            ChapterListItem(
                chapter = chapter,
                isRead = state.readChapterIds.contains(chapter.id),
                downloadStatus = state.downloadStatuses[chapter.id],
                onClick = { onChapterClick(chapter.id) },
                onDownloadClick = { onDownloadChapter(chapter) }
            )
            HorizontalDivider(color = Surface.copy(alpha = 0.5f), thickness = 0.5.dp)
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}
