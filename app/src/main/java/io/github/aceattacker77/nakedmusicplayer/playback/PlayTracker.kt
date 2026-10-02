package io.github.aceattacker77.nakedmusicplayer.playback

import io.github.aceattacker77.nakedmusicplayer.data.db.PlayCountRule

/**
 * Accumulates listened time per item instance and reports a play once [PlayCountRule] holds.
 * Time is evaluated whenever playback pauses or the item changes.
 */
class PlayTracker(private val onCounted: (songId: Long) -> Unit) {
    private var mediaId: String? = null
    private var durationMs = 0L
    private var playingSinceMs: Long? = null
    private var listenedMs = 0L
    private var counted = false

    fun onPlaying(mediaId: String, durationMs: Long, nowMs: Long) {
        if (mediaId != this.mediaId) {
            reset()
            this.mediaId = mediaId
        }
        this.durationMs = durationMs
        if (playingSinceMs == null) playingSinceMs = nowMs
    }

    fun onPaused(nowMs: Long) {
        accumulate(nowMs)
    }

    fun onItemChanged(nowMs: Long) {
        accumulate(nowMs)
        reset()
    }

    private fun accumulate(nowMs: Long) {
        playingSinceMs?.let { listenedMs += nowMs - it }
        playingSinceMs = null
        val songId = mediaId?.let(LibraryTree::songIdOf)
        if (!counted && songId != null && durationMs > 0 && PlayCountRule.countsAsPlay(listenedMs, durationMs)) {
            counted = true
            onCounted(songId)
        }
    }

    private fun reset() {
        mediaId = null
        durationMs = 0
        playingSinceMs = null
        listenedMs = 0
        counted = false
    }
}
