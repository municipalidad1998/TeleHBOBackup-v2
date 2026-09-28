package com.denilson.music.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.denilson.music.ui.components.MiniPlayer
import com.denilson.music.ui.screens.EqualizerScreen
import com.denilson.music.ui.screens.FavoritesScreen
import com.denilson.music.ui.screens.HomeScreen
import com.denilson.music.ui.screens.PlayerScreen
import com.denilson.music.ui.screens.PlaylistsScreen
import com.denilson.music.ui.screens.SearchScreen
import com.denilson.music.ui.screens.SettingsScreen
import com.denilson.music.ui.screens.SongsScreen
import com.denilson.music.ui.screens.YoutubeMusicScreen
import com.denilson.music.ui.screens.YoutubeScreen
import com.denilson.music.ui.theme.maxGradient
import com.denilson.music.ui.viewmodels.LibraryViewModel
import com.denilson.music.ui.viewmodels.MainViewModel

object Routes {
    const val HOME = "home"
    const val SONGS = "songs"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val PLAYLISTS = "playlists"
    const val SETTINGS = "settings"
    const val EQUALIZER = "equalizer"
    const val PLAYER = "player"
    const val YOUTUBE = "youtube"
    const val YOUTUBE_MUSIC = "youtube_music"
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val navItems = listOf(
    NavItem(Routes.HOME, "Inicio", Icons.Rounded.Home),
    NavItem(Routes.SEARCH, "Buscar", Icons.Rounded.Search),
    NavItem(Routes.SONGS, "Canciones", Icons.Rounded.LibraryMusic),
    NavItem(Routes.YOUTUBE, "YouTube", Icons.Rounded.SmartDisplay),
    NavItem(Routes.YOUTUBE_MUSIC, "YT Music", Icons.AutoMirrored.Rounded.QueueMusic),
    NavItem(Routes.SETTINGS, "Ajustes", Icons.Rounded.Settings)
)

@Composable
fun AppRoot(onPermissionResult: () -> Unit = {}) {
    val navController = rememberNavController()
    val mainVm: MainViewModel = hiltViewModel()
    val libVm: LibraryViewModel = hiltViewModel()

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val nowPlaying by mainVm.player.nowPlaying.collectAsStateWithLifecycle()
    val isPlaying by mainVm.player.isPlaying.collectAsStateWithLifecycle()

    // Escanea al entrar si la biblioteca esta vacia
    LaunchedEffect(Unit) {
        delay(400L)
        if (libVm.songs.value.isEmpty()) libVm.scan()
    }

    // Reproductor y pantallas web ocupan toda la pantalla
    val fullScreen = currentRoute == Routes.PLAYER ||
        currentRoute == Routes.YOUTUBE ||
        currentRoute == Routes.YOUTUBE_MUSIC

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!fullScreen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding()
                ) {
                    nowPlaying?.let { np ->
                        MiniPlayer(
                            player = np,
                            isPlaying = isPlaying,
                            accent = maxGradient(),
                            onPlayPause = { mainVm.player.playPause() },
                            onNext = { mainVm.player.next() },
                            onExpand = { navController.navigate(Routes.PLAYER) }
                        )
                    }
                    MaxBottomBar(backStack?.destination) { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(libVm = libVm, onNavigate = { navController.navigate(it) })
            }
            composable(Routes.SONGS) { SongsScreen(libVm) }
            composable(Routes.SEARCH) { SearchScreen(libVm) }
            composable(Routes.FAVORITES) { FavoritesScreen(libVm) }
            composable(Routes.PLAYLISTS) { PlaylistsScreen(libVm) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    mainVm, libVm,
                    onOpenEqualizer = { navController.navigate(Routes.EQUALIZER) }
                )
            }
            composable(Routes.EQUALIZER) {
                EqualizerScreen(mainVm, onBack = { navController.popBackStack() })
            }
            composable(Routes.PLAYER) {
                PlayerScreen(mainVm, onBack = { navController.popBackStack() })
            }
            composable(Routes.YOUTUBE) {
                YoutubeScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.YOUTUBE_MUSIC) {
                YoutubeMusicScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun MaxBottomBar(destination: NavDestination?, onSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.height(66.dp)
    ) {
        navItems.forEach { item ->
            val selected = destination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
