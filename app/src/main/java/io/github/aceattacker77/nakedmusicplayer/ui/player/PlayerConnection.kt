package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.PlaybackService
import io.github.aceattacker77.nakedmusicplayer.playback.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The single UI-side handle on playback: wraps one [Player] (a MediaController in the app). */
class PlayerConnection(
    private val player: Player,
    @Suppress("unused") private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<PlayerUiState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            _state.value = snapshot()
        }
    }

    init {
        player.addListener(listener)
    }

    /** Emits the playback position every 250 ms, but only while someone is collecting. */
    fun positionMs(): Flow<Long> = flow {
        while (true) {
            emit(player.currentPosition)
            delay(POSITION_POLL_MS)
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int) {
        if (songs.isEmpty()) return
        player.setMediaItems(songs.map { it.toMediaItem() }, startIndex.coerceIn(0, songs.lastIndex), 0L)
        player.prepare()
        player.play()
    }

    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        if (player.mediaItemCount == 0) {
            playSongs(songs, 0)
        } else {
            player.addMediaItems(player.currentMediaItemIndex + 1, songs.map { it.toMediaItem() })
        }
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        val wasEmpty = player.mediaItemCount == 0
        player.addMediaItems(songs.map { it.toMediaItem() })
        if (wasEmpty) player.prepare()
    }

    fun moveQueueItem(from: Int, to: Int) = player.moveMediaItem(from, to)

    fun removeQueueItem(index: Int) = player.removeMediaItem(index)

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) player.prepare()
            player.play()
        }
    }

    fun next() = player.seekToNextMediaItem()

    fun previous() = player.seekToPreviousMediaItem()

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeat() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        player.removeListener(listener)
        (player as? MediaController)?.release()
    }

    private fun snapshot(): PlayerUiState {
        val duration = player.duration.takeIf { it != C.TIME_UNSET }
            ?: player.currentMediaItem?.mediaMetadata?.durationMs
            ?: 0L
        return PlayerUiState(
            current = player.currentMediaItem,
            isPlaying = player.isPlaying,
            durationMs = duration,
            queue = List(player.mediaItemCount) { player.getMediaItemAt(it) },
            currentIndex = if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
    }

    companion object {
        private const val POSITION_POLL_MS = 250L
    }
}

/** Connects a [MediaController] to [PlaybackService] and wraps it in a [PlayerConnection]. */
suspend fun PlayerConnection.Companion.connect(context: Context, scope: CoroutineScope): PlayerConnection {
    val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
    val future = MediaController.Builder(context, token).buildAsync()
    val controller = suspendCancellableCoroutine { continuation ->
        future.addListener(
            {
                try {
                    continuation.resume(future.get())
                } catch (t: Throwable) {
                    continuation.resumeWithException(t)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        continuation.invokeOnCancellation { future.cancel(false) }
    }
    return PlayerConnection(controller, scope)
}
