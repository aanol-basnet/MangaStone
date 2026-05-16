package com.personal.mangastone.ui.screens.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.personal.mangastone.ui.components.ErrorState
import com.personal.mangastone.ui.components.LoadingIndicator
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.Sepia
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chapterId: String,
    mangaId: String,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    LaunchedEffect(chapterId, mangaId) { viewModel.load(chapterId, mangaId) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettingsSheet by remember { mutableStateOf(false) }
    // Slot index to scroll to when slider moves; cleared after consumption
    var scrollToSlot by remember { mutableStateOf<Int?>(null) }

    when (val state = uiState) {
        is ReaderUiState.Loading -> LoadingIndicator(fullScreen = true)
        is ReaderUiState.Error -> ErrorState(
            message = state.message,
            onRetry = { viewModel.retry() }
        )
        is ReaderUiState.Success -> {
            val bgColor = when (state.settings.background) {
                ReaderBackground.BLACK -> Color.Black
                ReaderBackground.WHITE -> Color.White
                ReaderBackground.SEPIA -> Sepia
            }

            Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                when (state.settings.direction) {
                    ReadingDirection.VERTICAL -> VerticalReader(
                        pageItems = state.pageItems,
                        initialSlot = state.currentSlot,
                        scrollToSlot = scrollToSlot,
                        onScrollConsumed = { scrollToSlot = null },
                        onSlotChanged = viewModel::onSlotChanged,
                        onTap = viewModel::toggleOverlay
                    )
                    else -> HorizontalReader(
                        pageItems = state.pageItems,
                        initialSlot = state.currentSlot,
                        reverseLayout = state.settings.direction == ReadingDirection.RIGHT_TO_LEFT,
                        scrollToSlot = scrollToSlot,
                        onScrollConsumed = { scrollToSlot = null },
                        onSlotChanged = viewModel::onSlotChanged,
                        onTap = viewModel::toggleOverlay
                    )
                }

                AnimatedVisibility(
                    visible = state.showOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    ReaderOverlay(
                        currentPageInChapter = state.currentPageInChapter,
                        totalPagesInChapter = state.currentChapterTotalPages,
                        chapterNumber = state.currentChapterNumber,
                        chapterTitle = state.currentChapterTitle,
                        onBack = onBack,
                        onSettingsClick = { showSettingsSheet = true },
                        onPageSlide = { pageInChapter ->
                            val slot = state.currentChapterStartSlot + pageInChapter
                            viewModel.onSlotChanged(slot)
                            scrollToSlot = slot
                        }
                    )
                }
            }

            if (showSettingsSheet) {
                ReaderSettingsSheet(
                    settings = state.settings,
                    onSettingsChange = viewModel::updateSettings,
                    onDismiss = { showSettingsSheet = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun HorizontalReader(
    pageItems: List<PageItem>,
    initialSlot: Int,
    reverseLayout: Boolean,
    scrollToSlot: Int?,
    onScrollConsumed: () -> Unit,
    onSlotChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    if (pageItems.isEmpty()) return

    val pagerState = rememberPagerState(initialPage = initialSlot.coerceIn(0, pageItems.lastIndex))

    LaunchedEffect(pagerState.currentPage) {
        onSlotChanged(pagerState.currentPage)
    }

    LaunchedEffect(scrollToSlot) {
        scrollToSlot?.let { slot ->
            pagerState.animateScrollToPage(slot.coerceIn(0, pageItems.lastIndex))
            onScrollConsumed()
        }
    }

    HorizontalPager(
        count = pageItems.size,
        state = pagerState,
        reverseLayout = reverseLayout,
        modifier = Modifier.fillMaxSize()
    ) { slot ->
        when (val item = pageItems[slot]) {
            is PageItem.MangaPage -> ReaderPage(
                url = item.url,
                contentDescription = "Page",
                onTap = onTap
            )
            is PageItem.ChapterDivider -> HorizontalChapterDivider(
                endNumber = item.endNumber,
                startNumber = item.startNumber
            )
        }
    }
}

@Composable
private fun VerticalReader(
    pageItems: List<PageItem>,
    initialSlot: Int,
    scrollToSlot: Int?,
    onScrollConsumed: () -> Unit,
    onSlotChanged: (Int) -> Unit,
    onTap: () -> Unit
) {
    val safeInitial = initialSlot.coerceIn(0, maxOf(pageItems.lastIndex, 0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)
    val scope = rememberCoroutineScope()

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onSlotChanged(listState.firstVisibleItemIndex)
    }

    LaunchedEffect(scrollToSlot) {
        scrollToSlot?.let { slot ->
            scope.launch {
                listState.animateScrollToItem(slot.coerceIn(0, maxOf(pageItems.lastIndex, 0)))
            }
            onScrollConsumed()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
    ) {
        items(pageItems.size) { slot ->
            when (val item = pageItems[slot]) {
                is PageItem.MangaPage -> {
                    var scale by remember { mutableStateOf(1f) }
                    SubcomposeAsyncImage(
                        model = item.url,
                        contentDescription = "Page",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectPinchGesture { zoom ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                }
                            }
                            .scale(scale),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(300.dp),
                                contentAlignment = Alignment.Center
                            ) { CircularProgressIndicator(color = Primary) }
                        },
                        error = { PageErrorPlaceholder() }
                    )
                }
                is PageItem.ChapterDivider -> VerticalChapterDivider(
                    endNumber = item.endNumber,
                    startNumber = item.startNumber
                )
            }
        }
    }
}

@Composable
private fun ReaderPage(
    url: String,
    contentDescription: String,
    onTap: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectPinchGesture { zoom ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { scale = 1f }
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale),
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            },
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) },
                    contentAlignment = Alignment.Center
                ) { PageErrorPlaceholder() }
            }
        )
    }
}

@Composable
private fun HorizontalChapterDivider(endNumber: String?, startNumber: String?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (endNumber != null) {
                Text(
                    text = "End of Chapter $endNumber",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(
                modifier = Modifier.width(120.dp),
                color = Color.White.copy(alpha = 0.2f)
            )
            if (startNumber != null) {
                Text(
                    text = "Chapter $startNumber",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun VerticalChapterDivider(endNumber: String?, startNumber: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (endNumber != null) {
            Text(
                text = "End of Chapter $endNumber",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
        if (startNumber != null) {
            Text(
                text = "Chapter $startNumber",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PageErrorPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Icon(
            Icons.Default.BrokenImage,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text("Failed to load page", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReaderOverlay(
    currentPageInChapter: Int,
    totalPagesInChapter: Int,
    chapterNumber: String?,
    chapterTitle: String?,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    onPageSlide: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    if (chapterNumber != null) {
                        Text(
                            "Chapter $chapterNumber",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    if (chapterTitle != null) {
                        Text(
                            chapterTitle,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (totalPagesInChapter > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Slider(
                        value = currentPageInChapter.toFloat(),
                        onValueChange = { onPageSlide(it.toInt()) },
                        valueRange = 0f..(totalPagesInChapter - 1).toFloat().coerceAtLeast(0f),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                    )
                    Text(
                        "${currentPageInChapter + 1} / $totalPagesInChapter",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    settings: ReaderSettings,
    onSettingsChange: (ReaderSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16213E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reader Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)

            Text("Direction", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingDirection.values().forEach { dir ->
                    FilterChip(
                        selected = settings.direction == dir,
                        onClick = { onSettingsChange(settings.copy(direction = dir)) },
                        label = { Text(dir.label()) }
                    )
                }
            }

            Text("Background", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReaderBackground.values().forEach { bg ->
                    FilterChip(
                        selected = settings.background == bg,
                        onClick = { onSettingsChange(settings.copy(background = bg)) },
                        label = { Text(bg.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Data Saver", color = Color.White)
                Switch(
                    checked = settings.dataSaver,
                    onCheckedChange = { onSettingsChange(settings.copy(dataSaver = it)) }
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private suspend fun PointerInputScope.detectPinchGesture(onZoom: (Float) -> Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                val zoom = event.calculateZoom()
                if (zoom != 1f) {
                    onZoom(zoom)
                    event.changes.forEach { it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun ReadingDirection.label() = when (this) {
    ReadingDirection.LEFT_TO_RIGHT -> "L→R"
    ReadingDirection.RIGHT_TO_LEFT -> "R←L"
    ReadingDirection.VERTICAL -> "Webtoon"
}
