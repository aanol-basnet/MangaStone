package com.personal.mangastone.ui.screens.favorites

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import coil.compose.AsyncImage
import com.personal.mangastone.data.local.entities.FavoriteEntity
import com.personal.mangastone.ui.components.EmptyState
import com.personal.mangastone.ui.components.MangaCoverCard
import com.personal.mangastone.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FavoritesScreen(
    onMangaClick: (String) -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedForRemoval by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.allFavorites.isEmpty()) {
            EmptyState(
                message = "No favorites yet.\nTap the heart on any manga to add it here.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.allFavorites, key = { it.mangaId }) { favorite ->
                    Box {
                        MangaCoverCard(
                            title = favorite.title,
                            coverUrl = favorite.coverUrl,
                            hasNewChapter = uiState.hasUpdates.any { it.mangaId == favorite.mangaId },
                            onClick = { onMangaClick(favorite.mangaId) },
                            modifier = Modifier.combinedClickable(
                                onClick = { onMangaClick(favorite.mangaId) },
                                onLongClick = { selectedForRemoval = favorite.mangaId }
                            )
                        )
                    }
                }
            }
        }
    }

    if (selectedForRemoval != null) {
        AlertDialog(
            onDismissRequest = { selectedForRemoval = null },
            title = { Text("Remove Favorite") },
            text = { Text("Remove this manga from your favorites?") },
            confirmButton = {
                TextButton(onClick = {
                    selectedForRemoval?.let { viewModel.removeFavorite(it) }
                    selectedForRemoval = null
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { selectedForRemoval = null }) { Text("Cancel") }
            }
        )
    }
}
