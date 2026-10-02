package io.github.aceattacker77.nakedmusicplayer.playback

/** Decides what to do when an item fails to play: skip it, or give up after repeated failures. */
class SkipPolicy(private val maxConsecutiveFailures: Int = 3) {
    enum class Action { SKIP, STOP }

    private var failures = 0

    fun onError(): Action {
        failures++
        if (failures < maxConsecutiveFailures) return Action.SKIP
        failures = 0 // having given up, the next attempt starts fresh
        return Action.STOP
    }

    fun onItemStartedSuccessfully() {
        failures = 0
    }
}
