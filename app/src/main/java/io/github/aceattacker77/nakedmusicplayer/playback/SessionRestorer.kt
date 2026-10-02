package io.github.aceattacker77.nakedmusicplayer.playback

import androidx.media3.common.MediaItem
import io.github.aceattacker77.nakedmusicplayer.data.session.SavedSession

object SessionRestorer {
    data class Restored(val items: List<MediaItem>, val index: Int, val positionMs: Long)

    /**
     * Rebuilds a saved queue, dropping songs that no longer exist. If the current song is gone the
     * next surviving one is selected at 0 ms (or the last one when nothing follows).
     */
    fun restore(saved: SavedSession, resolve: (String) -> MediaItem?): Restored? {
        if (saved.mediaIds.isEmpty()) return null
        val resolved = saved.mediaIds.map(resolve)
        val items = resolved.filterNotNull()
        if (items.isEmpty()) return null

        val current = saved.index.coerceIn(0, saved.mediaIds.lastIndex)
        val survivorsBefore = resolved.subList(0, current).count { it != null }
        return if (resolved[current] != null) {
            Restored(items, survivorsBefore, saved.positionMs)
        } else {
            Restored(items, survivorsBefore.coerceAtMost(items.lastIndex), 0L)
        }
    }
}
