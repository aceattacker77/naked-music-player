package io.github.aceattacker77.nakedmusicplayer.ui.settings

import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinAccent
import androidx.compose.foundation.layout.padding
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.themedButtonShape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinText
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.ScreenTitle
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ImportOutcome
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the user picks, imports, exports and deletes skins. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinPickerScreen(
    manager: SkinManager,
    settings: AppSettings,
    ioDispatcher: CoroutineDispatcher,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val skins by manager.available.collectAsStateWithLifecycle()
    val active by manager.active.collectAsStateWithLifecycle()

    var importError by remember { mutableStateOf<String?>(null) }
    var replaceCandidate by remember { mutableStateOf<Uri?>(null) }
    var menuSkin by remember { mutableStateOf<Skin?>(null) }
    var exportSkin by remember { mutableStateOf<Skin?>(null) }
    val cannotRead = stringResource(R.string.skin_import_unreadable)
    val exportFailed = stringResource(R.string.skin_export_failed)

    fun import(uri: Uri, replace: Boolean) {
        scope.launch {
            val outcome = readThenImport(context, manager, uri, replace, cannotRead, ioDispatcher)
            when (outcome) {
                is ImportOutcome.Installed -> Unit
                is ImportOutcome.AlreadyExists -> replaceCandidate = uri
                is ImportOutcome.Failed -> importError = outcome.message
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) import(uri, replace = false)
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        val skin = exportSkin
        exportSkin = null
        if (uri != null && skin != null) {
            scope.launch {
                val ok = writeSkin(context, manager, skin, uri, ioDispatcher)
                if (!ok) Toast.makeText(context, exportFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier.fillMaxSize().testTag("skin-picker")) {
        val titleCards = LocalOrnament.current.titleCards
        TopAppBar(
            title = { if (!titleCards) Text(stringResource(R.string.settings_skins)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
            },
            actions = {
                ImportSkinButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, accent = titleCards)
            },
            windowInsets = WindowInsets(0),
        )
        if (titleCards) {
            ScreenTitle(skinText("skins_kicker", stringResource(R.string.kicker_skins)), stringResource(R.string.settings_skins))
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(skins, key = { it.id }) { skin ->
                Box {
                    SkinPreviewCard(
                        skin = skin,
                        settings = settings,
                        selected = skin.id == active.id,
                        onClick = { scope.launch { manager.apply(skin.id) } },
                        onLongClick = { menuSkin = skin },
                    )
                    DropdownMenu(expanded = menuSkin?.id == skin.id, onDismissRequest = { menuSkin = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.skin_export)) },
                            onClick = {
                                menuSkin = null
                                exportSkin = skin
                                exportLauncher.launch("${skin.id}.mskin")
                            },
                        )
                        if (skin.baseDir != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete)) },
                                onClick = {
                                    menuSkin = null
                                    scope.launch { manager.delete(skin.id) }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = { importError = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.skin_import_failed)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text(stringResource(R.string.ok)) } },
        )
    }
    replaceCandidate?.let { uri ->
        AlertDialog(
            onDismissRequest = { replaceCandidate = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            title = { Text(stringResource(R.string.skin_replace_title)) },
            text = { Text(stringResource(R.string.skin_replace_message)) },
            confirmButton = {
                TextButton(onClick = {
                    replaceCandidate = null
                    import(uri, replace = true)
                }) { Text(stringResource(R.string.skin_replace)) }
            },
            dismissButton = { TextButton(onClick = { replaceCandidate = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private suspend fun readThenImport(
    context: Context,
    manager: SkinManager,
    uri: Uri,
    replace: Boolean,
    unreadableMessage: String,
    ioDispatcher: CoroutineDispatcher,
): ImportOutcome {
    val stream = withContext(ioDispatcher) { runCatching { context.contentResolver.openInputStream(uri) }.getOrNull() }
        ?: return ImportOutcome.Failed(unreadableMessage)
    return stream.use { manager.import(it, replace) }
}

private suspend fun writeSkin(
    context: Context,
    manager: SkinManager,
    skin: Skin,
    uri: Uri,
    ioDispatcher: CoroutineDispatcher,
): Boolean =
    withContext(ioDispatcher) {
        runCatching {
            val out = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            out.use { manager.export(skin, it) }
            true
        }.getOrDefault(false)
    }

/** The import action: a text button, or with [accent] a themed outlined button with a plus icon and the skin's kana. */
@Composable
internal fun ImportSkinButton(onClick: () -> Unit, accent: Boolean) {
    val label = stringResource(R.string.skin_import)
    if (accent) {
        val kana = skinAccent("import_kana")
        OutlinedButton(onClick = onClick, shape = themedButtonShape(), modifier = Modifier.padding(end = 8.dp)) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(skinLabel(label), modifier = Modifier.originalLabel(label))
            if (kana != null) Text(" $kana", modifier = Modifier.clearAndSetSemantics {})
        }
    } else {
        TextButton(onClick = onClick) { Text(label) }
    }
}
