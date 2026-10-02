package io.github.aceattacker77.nakedmusicplayer.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerConnection
import io.github.aceattacker77.nakedmusicplayer.ui.player.connect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Commands travel to the service asynchronously; give them a moment to leave before disconnecting.
private const val COMMAND_FLUSH_MS = 400L

/**
 * Connects a MediaController, runs [command], disconnects. Connecting wakes the service, which
 * restores the last queue paused, so Play on an idle widget resumes where the user left off.
 */
private suspend fun withPlayer(context: Context, command: (PlayerConnection) -> Unit) {
    val connection = withContext(Dispatchers.Main) {
        PlayerConnection.connect(context.applicationContext, CoroutineScope(Dispatchers.Main))
    }
    withContext(Dispatchers.Main) { command(connection) }
    delay(COMMAND_FLUSH_MS)
    withContext(Dispatchers.Main) { connection.release() }
}

class PrevAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        withPlayer(context) { it.previous() }
}

class PlayPauseAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        withPlayer(context) { it.togglePlayPause() }
}

class NextAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        withPlayer(context) { it.next() }
}

class ShuffleAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        withPlayer(context) { it.toggleShuffle() }
}

class RepeatAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) =
        withPlayer(context) { it.cycleRepeat() }
}
