package io.github.aceattacker77.nakedmusicplayer.data.db

object PlayCountRule {
    private const val MAX_REQUIRED_MS = 240_000L

    fun countsAsPlay(listenedMs: Long, durationMs: Long): Boolean =
        listenedMs >= durationMs / 2 || listenedMs >= MAX_REQUIRED_MS
}
