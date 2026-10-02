package io.github.aceattacker77.nakedmusicplayer.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.aceattacker77.nakedmusicplayer.AppContainer
import io.github.aceattacker77.nakedmusicplayer.LocalAppContainer
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongMenu
import io.github.aceattacker77.nakedmusicplayer.ui.library.AlbumDetailPane
import io.github.aceattacker77.nakedmusicplayer.ui.library.ArtistDetailPane
import io.github.aceattacker77.nakedmusicplayer.ui.search.SearchScreen
import io.github.aceattacker77.nakedmusicplayer.ui.search.SearchViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.library.AlbumsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.library.ArtistsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.library.LibraryViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.library.SongsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.PermissionDeniedScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.PermissionScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.needsNotificationPermission
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.requiredAudioPermission
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme

/** Themed root: applies the active skin, then gates the app on audio permission. */
@Composable
fun AppRoot() {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())
    val skin by container.skinManager.active.collectAsState()
    AppTheme(skin = skin, settings = settings) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            PermissionGate(container) { AppContent(container) }
        }
    }
}

@Composable
private fun PermissionGate(container: AppContainer, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val audioPermission = remember { requiredAudioPermission(Build.VERSION.SDK_INT) }
    var granted by remember { mutableStateOf(context.hasPermission(audioPermission)) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val libraryDenied by container.libraryRepository.permissionDenied.collectAsStateWithLifecycle()

    // The user may change the permission in system settings and come back.
    LifecycleResumeEffect(Unit) {
        val nowGranted = context.hasPermission(audioPermission)
        if (nowGranted != granted) {
            granted = nowGranted
            container.libraryRepository.refresh()
        }
        onPauseOrDispose {}
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        requested = true
        granted = context.hasPermission(audioPermission)
        if (granted) container.libraryRepository.refresh()
    }

    when {
        !granted && !requested -> PermissionScreen(onAllow = {
            val wanted = buildList {
                add(audioPermission)
                if (needsNotificationPermission(Build.VERSION.SDK_INT)) add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(wanted.toTypedArray())
        })
        !granted || libraryDenied -> PermissionDeniedScreen()
        else -> content()
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

@Composable
private fun AppContent(container: AppContainer) {
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    libraryRepository = container.libraryRepository,
                    settingsRepository = container.settingsRepository,
                    playerConnection = container.playerConnection,
                    computeDispatcher = container.computeDispatcher,
                )
            }
        },
    )
    val searchViewModel: SearchViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SearchViewModel(container.libraryRepository, container.computeDispatcher) }
        },
    )
    val nav = rememberNavController()
    var menuSong by remember { mutableStateOf<Song?>(null) }

    val openAlbum: (Album) -> Unit = { nav.navigate(AlbumDetail(it.id)) }
    val openArtist: (Artist) -> Unit = { nav.navigate(ArtistDetail(it.id)) }
    val showMenu: (Song) -> Unit = { menuSong = it }

    AppScaffold(
        nav = nav,
        miniPlayer = {},
        onSearch = { nav.navigate(Search) },
        onSettings = {},
    ) {
        NavHost(navController = nav, startDestination = Songs) {
            composable<Songs> { SongsScreen(libraryViewModel, onSongLongClick = showMenu) }
            composable<Albums> { AlbumsScreen(libraryViewModel, onSongLongClick = showMenu) }
            composable<Artists> { ArtistsScreen(libraryViewModel, onAlbumClick = openAlbum, onSongLongClick = showMenu) }
            composable<Playlists> {
                EmptyState(
                    title = stringResource(R.string.library_playlists),
                    message = stringResource(R.string.playlists_coming_soon),
                    action = null,
                )
            }
            composable<AlbumDetail> { entry ->
                AlbumDetailPane(
                    albumId = entry.toRoute<AlbumDetail>().id,
                    viewModel = libraryViewModel,
                    showBack = true,
                    onBack = { nav.popBackStack() },
                    onSongLongClick = showMenu,
                )
            }
            composable<ArtistDetail> { entry ->
                ArtistDetailPane(
                    artistId = entry.toRoute<ArtistDetail>().id,
                    viewModel = libraryViewModel,
                    showBack = true,
                    onBack = { nav.popBackStack() },
                    onAlbumClick = openAlbum,
                    onSongLongClick = showMenu,
                )
            }
            composable<Search> {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    libraryViewModel = libraryViewModel,
                    onBack = { nav.popBackStack() },
                    onAlbumClick = openAlbum,
                    onArtistClick = openArtist,
                    onSongLongClick = showMenu,
                )
            }
        }
    }

    menuSong?.let { song ->
        SongMenu(
            song = song,
            onPlayNext = { libraryViewModel.playNext(listOf(song)) },
            onAddToQueue = { libraryViewModel.addToQueue(listOf(song)) },
            onAddToPlaylist = {}, // wired to the playlist sheet in the playlists task
            onGoToAlbum = { nav.navigate(AlbumDetail(song.albumId)) },
            onGoToArtist = { nav.navigate(ArtistDetail(song.artistId)) },
            onDismiss = { menuSong = null },
        )
    }
}
