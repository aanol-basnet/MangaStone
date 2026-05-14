package com.personal.mangarock.ui.screens.browse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.personal.mangarock.ui.components.ErrorState
import com.personal.mangarock.ui.components.LoadingIndicator
import com.personal.mangarock.ui.components.MangaCoverCard
import com.personal.mangarock.ui.utils.displayTitle
import com.personal.mangarock.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onMangaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel()
) {
    val pagingItems = viewModel.mangaPagingFlow.collectAsLazyPagingItems()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Manga") },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = pagingItems.loadState.refresh) {
            is LoadState.Loading -> LoadingIndicator(fullScreen = true)
            is LoadState.Error -> ErrorState(
                message = state.error.message ?: "Unknown error",
                onRetry = { pagingItems.retry() }
            )
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 8.dp, end = 8.dp,
                        top = paddingValues.calculateTopPadding() + 8.dp,
                        bottom = paddingValues.calculateBottomPadding() + 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(pagingItems.itemCount) { index ->
                        pagingItems[index]?.let { manga ->
                            MangaCoverCard(
                                title = manga.displayTitle(),
                                coverUrl = manga.coverUrl,
                                onClick = { onMangaClick(manga.id) }
                            )
                        }
                    }
                    if (pagingItems.loadState.append is LoadState.Loading) {
                        item { LoadingIndicator() }
                        item { }
                    }
                }
            }
        }
    }
}
