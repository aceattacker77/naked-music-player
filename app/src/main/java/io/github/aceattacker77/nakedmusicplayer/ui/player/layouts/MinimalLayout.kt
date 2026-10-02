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
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition

/**
 * Typography first. With the art on the LEFT it shrinks to a thumbnail beside the title; otherwise
 * a modest square sits above it.
 */
@Composable
fun MinimalLayout(slots: NowPlayingSlots, artPosition: ArtPosition, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(LayoutPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        if (artPosition == ArtPosition.LEFT) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp)) { slots.artwork() }
                Box(Modifier.weight(1f)) { slots.trackInfo() }
            }
        } else {
            Box(Modifier.fillMaxWidth().weight(1f, fill = false), contentAlignment = Alignment.Center) {
                SquareArea(Modifier.fillMaxWidth().size(220.dp), maxSide = 220.dp) { slots.artwork() }
            }
            slots.trackInfo()
        }
        slots.seekBar()
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.controls() }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.secondaryControls() }
        slots.queueHandle()
    }
}
