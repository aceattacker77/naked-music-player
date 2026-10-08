package io.github.aceattacker77.nakedmusicplayer.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** How long to wait between widget progress pushes: about one step of a 40-step bar, kept between 2 and 10 seconds. */
internal fun liveProgressDelayMs(durationMs: Long): Long =
    if (durationMs <= 0) 5_000L else (durationMs / 40).coerceIn(2_000L, 10_000L)

/**
 * Calls [tick] repeatedly while live progress is switched on and a song is playing, and not at all otherwise. [update] is
 * safe to call on every player event: nothing restarts unless one of the three inputs changed.
 */
internal class WidgetProgressTicker(private val scope: CoroutineScope, private val tick: suspend () -> Unit) {
    private var job: Job? = null
    private var running: Triple<Boolean, Boolean, Long>? = null

    fun update(enabled: Boolean, playing: Boolean, durationMs: Long) {
        val active = enabled && playing
        val inputs = Triple(enabled, playing, durationMs)
        if (inputs == running) return
        running = inputs
        job?.cancel()
        job = null
        if (!active) return
        job = scope.launch {
            while (isActive) {
                delay(liveProgressDelayMs(durationMs))
                tick()
            }
        }
    }

    fun stop() = update(enabled = false, playing = false, durationMs = 0)
}
