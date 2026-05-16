package com.personal.mangarock.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.personal.mangarock.ui.screens.browse.BrowseScreen
import com.personal.mangarock.ui.screens.downloads.DownloadsScreen
import com.personal.mangarock.ui.screens.downloads.DownloadsViewModel
import com.personal.mangarock.ui.screens.favorites.FavoritesScreen
import com.personal.mangarock.ui.screens.foryou.ForYouScreen
import com.personal.mangarock.ui.screens.latest.LatestScreen
import com.personal.mangarock.ui.screens.mangainfo.MangaInfoScreen
import com.personal.mangarock.ui.screens.notifications.NotificationsScreen
import com.personal.mangarock.ui.screens.reader.ReaderScreen
import com.personal.mangarock.ui.screens.search.SearchScreen
import com.personal.mangarock.ui.screens.settings.SettingsScreen
import com.personal.mangarock.ui.theme.Background
import com.personal.mangarock.ui.theme.Primary
import com.personal.mangarock.ui.theme.Surface
import com.personal.mangarock.ui.theme.TextMuted
import com.personal.mangarock.ui.utils.LocalTitlePreference

sealed class Screen(val route: String) {
    object ForYou : Screen("foryou")
    object Browse : Screen("browse")
    object Latest : Screen("latest")
    object Favorites : Screen("favorites")
    object Downloads : Screen("downloads")
    object Search : Screen("search")
    object Settings : Screen("settings")
    object Notifications : Screen("notifications")
    object MangaInfo : Screen("mangainfo/{mangaId}") {
        fun route(mangaId: String) = "mangainfo/$mangaId"
    }
    object Reader : Screen("reader/{chapterId}/{mangaId}") {
        fun route(chapterId: String, mangaId: String) = "reader/${Uri.encode(chapterId)}/$mangaId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.ForYou, "For You", Icons.Default.Home),
    BottomNavItem(Screen.Browse, "All", Icons.Default.ViewModule),
    BottomNavItem(Screen.Latest, "Latest", Icons.Default.NewReleases),
    BottomNavItem(Screen.Favorites, "Favorites", Icons.Default.Favorite),
    BottomNavItem(Screen.Downloads, "Downloads", Icons.Default.Download)
)

@Composable
fun AppNavigation(titleLanguage: String = "en") {
    CompositionLocalProvider(LocalTitlePreference provides titleLanguage) {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // Observe active download count for the badge — activity-scoped ViewModel
        val downloadsViewModel: DownloadsViewModel = hiltViewModel()
        val downloadsState by downloadsViewModel.uiState.collectAsStateWithLifecycle()
        val activeDownloadCount = downloadsState.activeCount

        val showBottomBar = bottomNavItems.any {
            currentDestination?.hierarchy?.any { dest -> dest.route == it.screen.route } == true
        }

        Scaffold(
            containerColor = Background,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(containerColor = Surface) {
                        bottomNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (item.screen == Screen.Downloads && activeDownloadCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(containerColor = Primary) {
                                                    Text(
                                                        text = if (activeDownloadCount > 99) "99+" else "$activeDownloadCount",
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(item.icon, contentDescription = item.label)
                                        }
                                    } else {
                                        Icon(item.icon, contentDescription = item.label)
                                    }
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Primary,
                                    selectedTextColor = Primary,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.ForYou.route,
                modifier = Modifier.padding(paddingValues),
                enterTransition = {
                    fadeIn(tween(200)) + slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start, tween(200)
                    )
                },
                exitTransition = { fadeOut(tween(200)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = {
                    fadeOut(tween(200)) + slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End, tween(200)
                    )
                }
            ) {
                composable(Screen.ForYou.route) {
                    ForYouScreen(
                        onMangaClick = { navController.navigate(Screen.MangaInfo.route(it)) },
                        onSearchClick = { navController.navigate(Screen.Search.route) },
                        onNotificationsClick = { navController.navigate(Screen.Notifications.route) },
                        onSettingsClick = { navController.navigate(Screen.Settings.route) }
                    )
                }
                composable(Screen.Notifications.route) {
                    NotificationsScreen(
                        onChapterClick = { chapterId, mangaId ->
                            navController.navigate(Screen.Reader.route(chapterId, mangaId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Browse.route) {
                    BrowseScreen(
                        onMangaClick = { navController.navigate(Screen.MangaInfo.route(it)) },
                        onSearchClick = { navController.navigate(Screen.Search.route) }
                    )
                }
                composable(Screen.Latest.route) {
                    LatestScreen(
                        onMangaClick = { navController.navigate(Screen.MangaInfo.route(it)) }
                    )
                }
                composable(Screen.Favorites.route) {
                    FavoritesScreen(
                        onMangaClick = { navController.navigate(Screen.MangaInfo.route(it)) }
                    )
                }
                composable(Screen.Downloads.route) {
                    DownloadsScreen(
                        onChapterClick = { chapterId, mangaId ->
                            navController.navigate(Screen.Reader.route(chapterId, mangaId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Search.route) {
                    SearchScreen(
                        onMangaClick = { navController.navigate(Screen.MangaInfo.route(it)) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.MangaInfo.route) { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getString("mangaId") ?: return@composable
                    MangaInfoScreen(
                        mangaId = mangaId,
                        onChapterClick = { chapterId ->
                            navController.navigate(Screen.Reader.route(chapterId, mangaId))
                        },
                        onTagClick = { navController.navigate(Screen.Browse.route) },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.Reader.route) { backStackEntry ->
                    val chapterId = backStackEntry.arguments?.getString("chapterId") ?: return@composable
                    val mangaId = backStackEntry.arguments?.getString("mangaId") ?: return@composable
                    ReaderScreen(
                        chapterId = chapterId,
                        mangaId = mangaId,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
