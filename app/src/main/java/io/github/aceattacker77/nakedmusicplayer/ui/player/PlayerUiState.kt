package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

@Immutable
data class PlayerUiState(
    val current: MediaItem?,
    val isPlaying: Boolean,
    val durationMs: Long,
    val queue: List<MediaItem>,
    val currentIndex: Int,
    val shuffle: Boolean,
    val repeatMode: Int,
) {
    companion object {
        val EMPTY = PlayerUiState(
            current = null,
            isPlaying = false,
            durationMs = 0L,
            queue = emptyList(),
            currentIndex = -1,
            shuffle = false,
            repeatMode = Player.REPEAT_MODE_OFF,
        )
    }
}
