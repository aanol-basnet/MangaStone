package com.personal.mangastone.ui.screens.foryou

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.personal.mangastone.ui.components.LoadingIndicator
import com.personal.mangastone.ui.components.MangaCoverCard
import com.personal.mangastone.ui.theme.Surface
import com.personal.mangastone.ui.utils.displayTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForYouScreen(
    onMangaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    viewModel: ForYouViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationCount.collectAsStateWithLifecycle()
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing = uiState.isRefreshing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("For You") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onNotificationsClick) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        SwipeRefresh(
            state = swipeRefreshState,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    MangaSection(
                        title = "Popular Right Now",
                        state = uiState.popular,
                        onMangaClick = onMangaClick
                    )
                }
                item {
                    MangaSection(
                        title = "New Releases",
                        state = uiState.newReleases,
                        onMangaClick = onMangaClick
                    )
                }
                item {
                    MangaSection(
                        title = "Completed Series",
                        state = uiState.completed,
                        onMangaClick = onMangaClick
                    )
                }
            }
        }
    }
}

@Composable
private fun MangaSection(
    title: String,
    state: SectionState,
    onMangaClick: (String) -> Unit,
    cardWidth: androidx.compose.ui.unit.Dp = 130.dp
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when (state) {
            is SectionState.Loading -> LoadingIndicator()
            is SectionState.Error -> Text(
                text = "Error: ${state.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            is SectionState.Success -> {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.items) { manga ->
                        MangaCoverCard(
                            title = manga.displayTitle(),
                            coverUrl = manga.coverUrl,
                            onClick = { onMangaClick(manga.id) },
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }
        }
    }
}
