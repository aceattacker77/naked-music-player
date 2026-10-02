package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

enum class PlayerSheetValue { Collapsed, Expanded }

/** Whether Now Playing is open, plus how far a predictive-back gesture has progressed (0..1). */
@Stable
class PlayerSheetState(initial: PlayerSheetValue = PlayerSheetValue.Collapsed) {
    var value by mutableStateOf(initial)
        private set

    /** Progress of an in-flight predictive back gesture; the player shrinks as it grows. */
    var backProgress by mutableFloatStateOf(0f)

    val isExpanded: Boolean get() = value == PlayerSheetValue.Expanded

    fun expand() {
        value = PlayerSheetValue.Expanded
    }

    fun collapse() {
        value = PlayerSheetValue.Collapsed
        backProgress = 0f
    }
}

@Composable
fun rememberPlayerSheetState(): PlayerSheetState = rememberSaveable(
    saver = Saver(save = { it.value.name }, restore = { PlayerSheetState(PlayerSheetValue.valueOf(it)) }),
) { PlayerSheetState() }
