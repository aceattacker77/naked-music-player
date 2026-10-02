package io.github.aceattacker77.nakedmusicplayer.ui.player.layouts

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingSlots
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition

/** Large artwork with everything else underneath (TOP/CENTER) or beside it (LEFT). */
@Composable
fun ClassicLayout(slots: NowPlayingSlots, artPosition: ArtPosition, modifier: Modifier = Modifier) {
    val art: @Composable () -> Unit = { SquareArea(Modifier.fillMaxSize()) { slots.artwork() } }
    if (artPosition == ArtPosition.LEFT) {
        SideBySide(slots, modifier, art)
    } else {
        Stacked(slots, modifier, art)
    }
}
