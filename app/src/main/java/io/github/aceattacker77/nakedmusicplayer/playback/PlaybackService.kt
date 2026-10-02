package io.github.aceattacker77.nakedmusicplayer.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlaybackService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private var session: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        val container = (application as MusicApp).container
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
        session = MediaLibrarySession.Builder(this, player, LibraryCallback(container)).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
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

    private inner class LibraryCallback(private val container: AppContainer) : MediaLibrarySession.Callback {
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
