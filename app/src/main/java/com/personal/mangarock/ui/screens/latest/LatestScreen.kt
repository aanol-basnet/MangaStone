package com.personal.mangarock.ui.screens.latest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.personal.mangarock.ui.components.ErrorState
import com.personal.mangarock.ui.components.LoadingIndicator
import com.personal.mangarock.ui.components.MangaCoverCard
import com.personal.mangarock.ui.theme.Surface
import com.personal.mangarock.ui.utils.displayTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatestScreen(
    onMangaClick: (String) -> Unit,
    viewModel: LatestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Releases") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (val state = uiState) {
            is LatestUiState.Loading -> LoadingIndicator(fullScreen = true)
            is LatestUiState.Error -> ErrorState(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier.padding(padding)
            )
            is LatestUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 8.dp, end = 8.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.manga) { manga ->
                        MangaCoverCard(
                            title = manga.displayTitle(),
                            coverUrl = manga.coverUrl,
                            onClick = { onMangaClick(manga.id) }
                        )
                    }
                }
            }
        }
    }
}
