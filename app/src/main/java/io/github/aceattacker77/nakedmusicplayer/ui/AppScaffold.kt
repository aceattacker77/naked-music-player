package io.github.aceattacker77.nakedmusicplayer.ui

import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoTab
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoNavigationBar
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.aceattacker77.nakedmusicplayer.R
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: Int, val icon: Int)

private val tabs = listOf(
    Tab(Songs, Songs::class, R.string.library_songs, R.drawable.ic_music_note),
    Tab(Albums, Albums::class, R.string.library_albums, R.drawable.ic_album),
    Tab(Artists, Artists::class, R.string.library_artists, R.drawable.ic_person),
    Tab(Playlists, Playlists::class, R.string.library_playlists, R.drawable.ic_playlist_play),
)

/**
 * Which window insets the scaffold body pads. Null keeps the Material default; with the custom bottom bar the bar
 * pads the navigation-bar inset itself (so its background reaches under the system bar), so the body leaves it out.
 */
internal fun scaffoldContentInsetSides(useGeoBar: Boolean): WindowInsetsSides? =
    if (useGeoBar) WindowInsetsSides.Top + WindowInsetsSides.Horizontal else null

private fun navigateToTab(nav: NavHostController, tab: Tab) {
    nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Bottom bar on phones, rail on tablets/foldables. Tabs and the app bar only appear on the four
 * top-level destinations; [miniPlayer] sits directly above the bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    nav: NavHostController,
    miniPlayer: @Composable () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val backStackEntry by nav.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val topLevel = tabs.any { destination?.hasRoute(it.routeClass) == true }
    val layoutType = if (topLevel) {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo())
    } else {
        NavigationSuiteType.None
    }
    // The chamfered-block bar replaces Material's bottom bar on compact widths only; rails stay Material.
    val useGeoBar = layoutType == NavigationSuiteType.NavigationBar && LocalOrnament.current.navBlock
    val geoTabs = tabs.map { GeoTab(it.icon, stringResource(it.label), "geo-tab-${it.routeClass.simpleName}") }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            tabs.forEach { tab ->
                item(
                    selected = destination?.hasRoute(tab.routeClass) == true,
                    onClick = { navigateToTab(nav, tab) },
                    icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                    label = { Text(skinLabel(stringResource(tab.label))) },
                )
            }
        },
        layoutType = if (useGeoBar) NavigationSuiteType.None else layoutType,
    ) {
        Scaffold(
            topBar = { if (topLevel) LibraryTopBar(onSearch = onSearch, onSettings = onSettings) },
            contentWindowInsets = scaffoldContentInsetSides(useGeoBar)
                ?.let { ScaffoldDefaults.contentWindowInsets.only(it) } ?: ScaffoldDefaults.contentWindowInsets,
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Box(Modifier.weight(1f)) { content() }
                miniPlayer()
                if (useGeoBar) {
                    GeoNavigationBar(
                        tabs = geoTabs,
                        selectedIndex = tabs.indexOfFirst { destination?.hasRoute(it.routeClass) == true },
                        onSelect = { navigateToTab(nav, tabs[it]) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryTopBar(onSearch: () -> Unit, onSettings: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(painterResource(R.drawable.ic_search), contentDescription = stringResource(R.string.search))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings)) },
                        onClick = {
                            menuOpen = false
                            onSettings()
                        },
                    )
                }
            }
        },
    )
}
