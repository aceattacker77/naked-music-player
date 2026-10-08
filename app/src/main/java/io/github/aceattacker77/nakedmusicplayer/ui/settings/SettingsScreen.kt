package io.github.aceattacker77.nakedmusicplayer.ui.settings

import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.DocumentPaths

private val MIN_LENGTH_OPTIONS_MS = listOf(0L, 15_000L, 30_000L, 60_000L)

/** Returns an action that opens the folder picker and hands the choice to [viewModel]. */
@Composable
fun rememberAddFolderAction(viewModel: SettingsViewModel): () -> Unit {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) viewModel.addScanFolder(uri)
    }
    return remember(launcher) { { launcher.launch(null) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    equalizerAvailable: Boolean,
    onBack: () -> Unit,
    onOpenSkins: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenAbout: () -> Unit,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val folderOptions by viewModel.folderOptions.collectAsStateWithLifecycle()
    var choosingExclusions by remember { mutableStateOf(false) }

    Column(modifier.fillMaxSize().testTag("settings-screen")) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_theme))
            ThemeMode.entries.forEach { mode ->
                ListItem(
                    headlineContent = { Text(stringResource(mode.labelRes())) },
                    leadingContent = { RadioButton(selected = settings.themeMode == mode, onClick = null) },
                    modifier = Modifier.clickable { viewModel.setThemeMode(mode) },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_dynamic_colour)) },
                    supportingContent = { Text(stringResource(R.string.settings_dynamic_colour_hint)) },
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = viewModel::setDynamicColor,
                            modifier = Modifier.testTag("dynamic-switch"),
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setDynamicColor(!settings.dynamicColor) },
                )
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_skins)) },
                supportingContent = { Text(stringResource(R.string.settings_skins_hint)) },
                modifier = Modifier.clickable(onClick = onOpenSkins).padding(top = 8.dp),
            )

            SectionHeader(stringResource(R.string.settings_widget))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_widget_live)) },
                supportingContent = { Text(stringResource(R.string.settings_widget_live_hint)) },
                trailingContent = {
                    Switch(
                        checked = settings.widgetLiveProgress,
                        onCheckedChange = viewModel::setWidgetLiveProgress,
                        modifier = Modifier.testTag("widget-live-switch"),
                    )
                },
                modifier = Modifier.clickable { viewModel.setWidgetLiveProgress(!settings.widgetLiveProgress) },
            )

            SectionHeader(stringResource(R.string.settings_library))
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.settings_min_length), style = MaterialTheme.typography.bodyLarge)
                Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MIN_LENGTH_OPTIONS_MS.forEach { ms ->
                        FilterChip(
                            selected = settings.minDurationMs == ms,
                            onClick = { viewModel.setMinDuration(ms) },
                            label = { Text(minLengthLabel(ms)) },
                        )
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_excluded_folders)) },
                supportingContent = {
                    Text(pluralStringResource(R.plurals.folders_excluded, settings.excludedFolders.size, settings.excludedFolders.size))
                },
                modifier = Modifier.clickable { choosingExclusions = true },
            )

            Text(
                text = stringResource(R.string.settings_scan_folders),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 16.dp, top = 12.dp),
            )
            settings.scanFolderUris.sorted().forEach { uri ->
                ListItem(
                    headlineContent = { Text(folderLabel(uri)) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeScanFolder(uri) }) {
                            Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.settings_remove_folder))
                        }
                    },
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_add_folder)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onAddFolder),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_rescan)) },
                modifier = Modifier.clickable(onClick = viewModel::rescan),
            )

            if (equalizerAvailable) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.equalizer)) },
                    modifier = Modifier.clickable(onClick = onOpenEqualizer).testTag("settings-equalizer").padding(top = 8.dp),
                )
            }
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                modifier = Modifier.clickable(onClick = onOpenAbout),
            )
        }
    }

    if (choosingExclusions) {
        ExcludedFoldersDialog(
            options = folderOptions,
            initiallyExcluded = settings.excludedFolders,
            onConfirm = {
                viewModel.setExcludedFolders(it)
                choosingExclusions = false
            },
            onDismiss = { choosingExclusions = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ExcludedFoldersDialog(
    options: List<String>,
    initiallyExcluded: Set<String>,
    onConfirm: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember { mutableStateOf(initiallyExcluded) }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.settings_excluded_folders)) },
        text = {
            if (options.isEmpty()) {
                Text(stringResource(R.string.settings_no_folders))
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(options, key = { it }) { folder ->
                        val checked = folder in selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("folder-option-$folder")
                                .clickable { selected = if (checked) selected - folder else selected + folder },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Text(folder, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.done)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}

@Composable
private fun minLengthLabel(ms: Long): String =
    if (ms == 0L) stringResource(R.string.min_length_off) else stringResource(R.string.min_length_seconds, (ms / 1000).toInt())

/** A readable name for a picked folder: its path when it has one, otherwise the raw URI. */
private fun folderLabel(uriString: String): String {
    val uri = Uri.parse(uriString)
    return runCatching { DocumentPaths.toFilePath(DocumentsContract.getTreeDocumentId(uri)) }.getOrNull() ?: uriString
}
