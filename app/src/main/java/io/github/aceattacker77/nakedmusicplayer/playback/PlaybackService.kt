package io.github.aceattacker77.nakedmusicplayer.playback

import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import io.github.aceattacker77.nakedmusicplayer.AppContainer
import io.github.aceattacker77.nakedmusicplayer.MusicApp
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.session.SavedSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PlaybackService : MediaLibraryService() {
    private lateinit var container: AppContainer
    private lateinit var player: ExoPlayer
    private lateinit var playTracker: PlayTracker
    private val skipPolicy = SkipPolicy()
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        container = (application as MusicApp).container
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        playTracker = PlayTracker { songId ->
            container.applicationScope.launch {
                container.database.playStatDao().recordPlay(songId, System.currentTimeMillis())
            }
        }
        player.addListener(PlayerListener())
        session = MediaLibrarySession.Builder(this, player, LibraryCallback()).build()
        restoreSession()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveSession()
        if (!player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }

    /** Restores the last queue paused: it is never started automatically. */
    private fun restoreSession() {
        container.applicationScope.launch {
            val (saved, restored) = loadRestored() ?: return@launch
            ContextCompat.getMainExecutor(this@PlaybackService).execute {
                if (player.mediaItemCount == 0) {
                    player.shuffleModeEnabled = saved.shuffle
                    player.repeatMode = saved.repeatMode
                    player.setMediaItems(restored.items, restored.index, restored.positionMs)
                    player.prepare()
                }
            }
        }
    }

    private suspend fun loadRestored(): Pair<SavedSession, SessionRestorer.Restored>? {
        val saved = container.sessionStore.load() ?: return null
        // The library loads asynchronously; give it a moment so the saved ids can be resolved.
        withTimeoutOrNull(LIBRARY_WAIT_MS) { container.libraryRepository.library.first { it.songs.isNotEmpty() } }
        val library = container.libraryRepository.library.value
        val restored = SessionRestorer.restore(saved) { LibraryTree.item(it, library) }
        if (restored == null) {
            container.sessionStore.clear()
            return null
        }
        return saved to restored
    }

    private fun saveSession() {
        if (player.mediaItemCount == 0) return
        val snapshot = SavedSession(
            mediaIds = (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId },
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition,
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
        )
        container.applicationScope.launch { container.sessionStore.save(snapshot) }
    }

    private fun trackCurrentItem(nowMs: Long) {
        val item = player.currentMediaItem ?: return
        val duration = item.mediaMetadata.durationMs
            ?: player.duration.takeIf { it != C.TIME_UNSET }
            ?: 0L
        playTracker.onPlaying(item.mediaId, duration, nowMs)
    }

    private inner class PlayerListener : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            player.currentMediaItem?.let { item ->
                UnplayableRegistry.mark(item.mediaId)
                val title = item.mediaMetadata.title?.toString().orEmpty()
                Toast.makeText(this@PlaybackService, getString(R.string.skipped_cant_play, title), Toast.LENGTH_SHORT).show()
            }
            when (skipPolicy.onError()) {
                SkipPolicy.Action.SKIP ->
                    if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                        player.prepare()
                    } else {
                        player.stop()
                    }
                SkipPolicy.Action.STOP -> player.stop()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            val now = SystemClock.elapsedRealtime()
            if (isPlaying) {
                skipPolicy.onItemStartedSuccessfully()
                trackCurrentItem(now)
            } else {
                playTracker.onPaused(now)
                saveSession()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val now = SystemClock.elapsedRealtime()
            playTracker.onItemChanged(now)
            if (player.isPlaying) trackCurrentItem(now)
            saveSession()
        }
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        private val labels
            get() = LibraryTree.Labels(
                albums = getString(R.string.library_albums),
                artists = getString(R.string.library_artists),
                playlists = getString(R.string.library_playlists),
                songs = getString(R.string.library_songs),
            )
        private val library get() = container.libraryRepository.library.value

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(LibraryTree.item(LibraryTree.ROOT, library, labels)!!, params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = container.applicationScope.futureOf {
            val dao = container.database.playlistDao()
            val playlists = dao.observePlaylists().first()
            val playlistId = if (parentId.startsWith(LibraryTree.PLAYLIST_PREFIX)) {
                parentId.substringAfter(':').toLongOrNull()
            } else {
                null
            }
            val songIds = playlistId?.let { dao.observeSongIds(it).first() }.orEmpty()
            val children = LibraryTree.children(parentId, library, playlists, labels) { songIds }
            if (children == null) {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            } else {
                val from = (page * pageSize).coerceAtMost(children.size)
                val to = (from + pageSize).coerceAtMost(children.size)
                LibraryResult.ofItemList(ImmutableList.copyOf(children.subList(from, to)), params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = LibraryTree.item(mediaId, library, labels)
            return Futures.immediateFuture(
                if (item != null) LibraryResult.ofItem(item, null) else LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE),
            )
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            val library = library
            val resolved = mediaItems.mapNotNull { item ->
                LibraryTree.item(item.mediaId, library, labels)?.takeIf { it.localConfiguration != null }
                    ?: item.takeIf { it.localConfiguration != null }
            }
            return Futures.immediateFuture(resolved)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = container.applicationScope.futureOf {
            val (_, restored) = loadRestored() ?: throw UnsupportedOperationException("no session to resume")
            MediaSession.MediaItemsWithStartPosition(restored.items, restored.index, restored.positionMs)
        }
    }

    private companion object {
        const val LIBRARY_WAIT_MS = 10_000L
    }
}

/** Runs [block] on this scope and exposes its result as a Guava future, as Media3 callbacks require. */
internal fun <T> CoroutineScope.futureOf(block: suspend () -> T): ListenableFuture<T> {
    val future = SettableFuture.create<T>()
    launch {
        try {
            future.set(block())
        } catch (t: Throwable) {
            future.setException(t)
        }
    }
    return future
}
