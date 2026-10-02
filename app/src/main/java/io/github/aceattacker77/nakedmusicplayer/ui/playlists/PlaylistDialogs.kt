package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import io.github.aceattacker77.nakedmusicplayer.R

/** Asks for a playlist name; the confirm button stays disabled while the name is blank. */
@Composable
fun PlaylistNameDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    label: String = stringResource(R.string.playlist_name),
) {
    var name by remember { mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length))) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // AlertDialog already caps its own width; the platform default width makes Robolectric's
        // Compose test host spin forever when a dialog contains a text field.
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(label) },
                modifier = Modifier.testTag("playlist-name-field"),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.text.trim()) }, enabled = name.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
fun DeletePlaylistDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = { Text(stringResource(R.string.delete_playlist_title)) },
        text = { Text(stringResource(R.string.delete_playlist_message, name)) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm-delete")) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
