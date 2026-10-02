package io.github.aceattacker77.nakedmusicplayer.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState

/** One-time rationale shown before the system permission dialog. */
@Composable
fun PermissionScreen(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.permission_title),
        message = stringResource(R.string.permission_rationale),
        action = stringResource(R.string.permission_allow) to onAllow,
        modifier = modifier,
    )
}

/** Shown when access was refused (or revoked): the only way forward is the app's system settings. */
@Composable
fun PermissionDeniedScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    EmptyState(
        title = stringResource(R.string.permission_denied_title),
        message = stringResource(R.string.permission_denied_message),
        action = stringResource(R.string.open_settings) to { openAppSettings(context) },
        modifier = modifier,
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
