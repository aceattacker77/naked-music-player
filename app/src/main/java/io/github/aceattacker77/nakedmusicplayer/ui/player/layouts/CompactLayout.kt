package io.github.aceattacker77.nakedmusicplayer.ui.player.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingSlots

/** A single dense column: small art beside the title, then seek bar, controls, extras and queue. */
@Composable
fun CompactLayout(slots: NowPlayingSlots, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(LayoutPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(112.dp)) { slots.artwork() }
            Box(Modifier.weight(1f)) { slots.trackInfo() }
        }
        slots.seekBar()
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.controls() }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.secondaryControls() }
        slots.queueHandle()
    }
}
