package com.personal.mangastone.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.personal.mangastone.ui.components.ErrorState
import com.personal.mangastone.ui.components.LoadingIndicator
import com.personal.mangastone.ui.components.MangaCoverCard
import com.personal.mangastone.ui.utils.displayTitle
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.Surface
import com.personal.mangastone.ui.theme.TextMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMangaClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val genreResults = viewModel.genreResults.collectAsLazyPagingItems()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val showSearch = query.length >= 2
    val showGenre = selectedGenre != null && !showSearch

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search manga...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // Genre chips — always visible
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GENRES) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { viewModel.selectGenre(genre) },
                        label = { Text(genre.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = androidx.compose.ui.graphics.Color.White
                        )
                    )
                }
            }

            HorizontalDivider(color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.05f))

            when {
                // Search results
                showSearch -> {
                    when (searchResults.loadState.refresh) {
                        is LoadState.Loading -> LoadingIndicator(fullScreen = true)
                        is LoadState.Error -> ErrorState(
                            message = (searchResults.loadState.refresh as LoadState.Error)
                                .error.message ?: "Search failed",
                            onRetry = { searchResults.retry() }
                        )
                        else -> if (searchResults.itemCount == 0) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No results for \"$query\"",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            MangaGrid(
                                itemCount = searchResults.itemCount,
                                getItem = { searchResults[it] },
                                onMangaClick = {
                                    viewModel.submitSearch(query)
                                    onMangaClick(it)
                                },
                                isLoadingMore = searchResults.loadState.append is LoadState.Loading
                            )
                        }
                    }
                }

                // Genre results
                showGenre -> {
                    when (genreResults.loadState.refresh) {
                        is LoadState.Loading -> LoadingIndicator(fullScreen = true)
                        is LoadState.Error -> ErrorState(
                            message = (genreResults.loadState.refresh as LoadState.Error)
                                .error.message ?: "Failed to load genre",
                            onRetry = { genreResults.retry() }
                        )
                        else -> MangaGrid(
                            itemCount = genreResults.itemCount,
                            getItem = { genreResults[it] },
                            onMangaClick = onMangaClick,
                            isLoadingMore = genreResults.loadState.append is LoadState.Loading
                        )
                    }
                }

                // Recent searches
                else -> {
                    if (recentSearches.isNotEmpty()) {
                        Text(
                            "Recent",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyColumn {
                            items(recentSearches) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.onQueryChange(item.query)
                                            viewModel.submitSearch(item.query)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(item.query, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { viewModel.deleteHistory(item.query) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MangaGrid(
    itemCount: Int,
    getItem: (Int) -> com.personal.mangastone.domain.models.Manga?,
    onMangaClick: (String) -> Unit,
    isLoadingMore: Boolean
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(itemCount) { index ->
            getItem(index)?.let { manga ->
                MangaCoverCard(
                    title = manga.displayTitle(),
                    coverUrl = manga.coverUrl,
                    onClick = { onMangaClick(manga.id) }
                )
            }
        }
        if (isLoadingMore) {
            item { LoadingIndicator() }
            item { }
        }
    }
}
