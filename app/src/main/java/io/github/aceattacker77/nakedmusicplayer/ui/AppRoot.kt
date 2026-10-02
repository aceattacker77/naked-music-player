package io.github.aceattacker77.nakedmusicplayer.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.PredictiveBackHandler
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
import io.github.aceattacker77.nakedmusicplayer.data.playlists.SmartPlaylist
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongMenu
import io.github.aceattacker77.nakedmusicplayer.ui.library.AlbumDetailPane
import io.github.aceattacker77.nakedmusicplayer.ui.library.ArtistDetailPane
import io.github.aceattacker77.nakedmusicplayer.ui.equalizer.EqualizerScreen
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.AddToPlaylistSheet
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.PlaylistDetailScreen
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.PlaylistsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.PlaylistsViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.SmartPlaylistDetailScreen
import io.github.aceattacker77.nakedmusicplayer.ui.search.SearchScreen
import io.github.aceattacker77.nakedmusicplayer.ui.settings.AboutScreen
import io.github.aceattacker77.nakedmusicplayer.ui.settings.SettingsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.settings.SettingsViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.settings.SkinPickerScreen
import io.github.aceattacker77.nakedmusicplayer.ui.settings.rememberAddFolderAction
import io.github.aceattacker77.nakedmusicplayer.ui.search.SearchViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.library.AlbumsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.library.ArtistsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.library.LibraryViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.library.SongsScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.PermissionDeniedScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.PermissionScreen
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.needsNotificationPermission
import io.github.aceattacker77.nakedmusicplayer.ui.onboarding.requiredAudioPermission
import io.github.aceattacker77.nakedmusicplayer.ui.player.MiniPlayer
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingHost
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerConnection
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerUiState
import io.github.aceattacker77.nakedmusicplayer.ui.player.rememberPlayerSheetState
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow

/** Themed root: applies the active skin, then gates the app on audio permission. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppRoot() {
    val container = LocalAppContainer.current
    val settings by container.settingsRepository.settings.collectAsState(initial = AppSettings())
    val skin by container.skinManager.active.collectAsState()
    AppTheme(skin = skin, settings = settings) {
        // Lets UI Automator (the baseline-profile generator and benchmarks) find composables by test tag.
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
            color = MaterialTheme.colorScheme.background,
        ) {
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

@OptIn(ExperimentalSharedTransitionApi::class)
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
    val playlistsViewModel: PlaylistsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { PlaylistsViewModel(container.playlistRepository) }
        },
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { SettingsViewModel(container.settingsRepository, container.libraryRepository, container.folderScanner) }
        },
    )
    val addFolder = rememberAddFolderAction(settingsViewModel)
    val appSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val nav = rememberNavController()
    var menuSong by remember { mutableStateOf<Song?>(null) }
    var addTarget by remember { mutableStateOf<List<Song>?>(null) }

    // The player connects in the background; until it does the mini player simply stays hidden.
    val connection by produceState<PlayerConnection?>(initialValue = null, container) {
        value = runCatching { container.playerConnection.await() }.getOrNull()
    }
    val idle = remember { MutableStateFlow(PlayerUiState.EMPTY) }
    val playerState by (connection?.state ?: idle).collectAsStateWithLifecycle()
    val sheet = rememberPlayerSheetState()
    val equalizerCapabilities by container.equalizerController.capabilities.collectAsStateWithLifecycle()
    val hasTrack = playerState.current != null
    val expanded = sheet.isExpanded && hasTrack

    // Nothing left to show: fall back to the library.
    LaunchedEffect(hasTrack) { if (!hasTrack) sheet.collapse() }

    PredictiveBackHandler(enabled = expanded) { progress ->
        try {
            progress.collect { sheet.backProgress = it.progress }
            sheet.collapse()
        } catch (e: CancellationException) {
            sheet.backProgress = 0f
            throw e
        }
    }

    val openAlbum: (Album) -> Unit = { nav.navigate(AlbumDetail(it.id)) }
    val openArtist: (Artist) -> Unit = { nav.navigate(ArtistDetail(it.id)) }
    val showMenu: (Song) -> Unit = { menuSong = it }

    SharedTransitionLayout {
        Box(Modifier.fillMaxSize()) {
            AppScaffold(
                nav = nav,
                miniPlayer = {
                    AnimatedVisibility(
                        visible = hasTrack && !expanded,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                    ) {
                        connection?.let { player ->
                            MiniPlayer(
                                state = playerState,
                                positionMs = remember(player) { player.positionMs() },
                                onExpand = sheet::expand,
                                onPlayPause = player::togglePlayPause,
                                onNext = player::next,
                                artworkModifier = Modifier.sharedElement(
                                    rememberSharedContentState(ARTWORK_KEY),
                                    animatedVisibilityScope = this@AnimatedVisibility,
                                ),
                            )
                        }
                    }
                },
                onSearch = { nav.navigate(Search) },
                onSettings = { nav.navigate(Settings) },
            ) {
                NavHost(navController = nav, startDestination = Songs) {
                    composable<Songs> { SongsScreen(libraryViewModel, onSongLongClick = showMenu, onAddFolder = addFolder) }
                    composable<Albums> { AlbumsScreen(libraryViewModel, onSongLongClick = showMenu, onAddToPlaylist = { addTarget = it }) }
                    composable<Artists> {
                        ArtistsScreen(
                            libraryViewModel,
                            onAlbumClick = openAlbum,
                            onSongLongClick = showMenu,
                            onAddToPlaylist = { addTarget = it },
                        )
                    }
                    composable<Playlists> {
                        PlaylistsScreen(
                            viewModel = playlistsViewModel,
                            onOpenPlaylist = { nav.navigate(PlaylistDetail(it)) },
                            onOpenSmart = { nav.navigate(SmartPlaylistDetail(it.name)) },
                        )
                    }
                    composable<PlaylistDetail> { entry ->
                        PlaylistDetailScreen(
                            playlistId = entry.toRoute<PlaylistDetail>().id,
                            playlistsViewModel = playlistsViewModel,
                            libraryViewModel = libraryViewModel,
                            onBack = { nav.popBackStack() },
                            onSongLongClick = showMenu,
                        )
                    }
                    composable<SmartPlaylistDetail> { entry ->
                        SmartPlaylistDetailScreen(
                            kind = SmartPlaylist.valueOf(entry.toRoute<SmartPlaylistDetail>().kind),
                            playlistsViewModel = playlistsViewModel,
                            libraryViewModel = libraryViewModel,
                            onBack = { nav.popBackStack() },
                            onSongLongClick = showMenu,
                        )
                    }
                    composable<AlbumDetail> { entry ->
                        AlbumDetailPane(
                            albumId = entry.toRoute<AlbumDetail>().id,
                            viewModel = libraryViewModel,
                            showBack = true,
                            onBack = { nav.popBackStack() },
                            onSongLongClick = showMenu,
                            onAddToPlaylist = { addTarget = it },
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
                            onAddToPlaylist = { addTarget = it },
                        )
                    }
                    composable<Settings> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            equalizerAvailable = equalizerCapabilities != null,
                            onBack = { nav.popBackStack() },
                            onOpenSkins = { nav.navigate(Skins) },
                            onOpenEqualizer = { nav.navigate(Equalizer) },
                            onOpenAbout = { nav.navigate(About) },
                            onAddFolder = addFolder,
                        )
                    }
                    composable<Skins> {
                        SkinPickerScreen(container.skinManager, appSettings, container.ioDispatcher, onBack = { nav.popBackStack() })
                    }
                    composable<About> { AboutScreen(onBack = { nav.popBackStack() }) }
                    composable<Equalizer> {
                        EqualizerScreen(container.equalizerController, onBack = { nav.popBackStack() })
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

            AnimatedVisibility(
                visible = expanded,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 4 } + fadeOut(),
            ) {
                connection?.let { player ->
                    NowPlayingHost(
                        connection = player,
                        sheet = sheet,
                        equalizerAvailable = equalizerCapabilities != null,
                        onOpenEqualizer = {
                            sheet.collapse()
                            nav.navigate(Equalizer)
                        },
                        onAddToPlaylist = {
                            val current = playerState.current?.mediaId
                            container.libraryRepository.library.value.songs
                                .firstOrNull { "song:${it.id}" == current }
                                ?.let { addTarget = listOf(it) }
                        },
                        artworkModifier = Modifier.sharedElement(
                            rememberSharedContentState(ARTWORK_KEY),
                            animatedVisibilityScope = this@AnimatedVisibility,
                        ),
                    )
                }
            }
        }
    }

    menuSong?.let { song ->
        SongMenu(
            song = song,
            onPlayNext = { libraryViewModel.playNext(listOf(song)) },
            onAddToQueue = { libraryViewModel.addToQueue(listOf(song)) },
            onAddToPlaylist = { addTarget = listOf(song) },
            onGoToAlbum = { nav.navigate(AlbumDetail(song.albumId)) },
            onGoToArtist = { nav.navigate(ArtistDetail(song.artistId)) },
            onDismiss = { menuSong = null },
        )
    }

    addTarget?.let { songs ->
        val playlists = playlistsViewModel.playlists.collectAsStateWithLifecycle().value
        AddToPlaylistSheet(
            playlists = playlists,
            onPick = { playlistsViewModel.add(it.id, songs) },
            onCreate = { playlistsViewModel.createAndAdd(it, songs) },
            onDismiss = { addTarget = null },
        )
    }
}

private const val ARTWORK_KEY = "now-playing-artwork"
