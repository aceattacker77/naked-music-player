package io.github.aceattacker77.nakedmusicplayer.ui.player.layouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingSlots

internal val LayoutPadding = 24.dp

/** Track info, seek bar, controls, secondary controls and queue handle, stacked. */
@Composable
internal fun InfoColumn(
    slots: NowPlayingSlots,
    modifier: Modifier = Modifier,
    centeredInfo: Boolean = false,
    scrollable: Boolean = false,
) {
    Column(
        modifier = modifier.then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = if (centeredInfo) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        slots.trackInfo()
        slots.seekBar()
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.controls() }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { slots.secondaryControls() }
        slots.queueHandle()
    }
}

/** A square of the largest size that fits the space it is given, centred. */
@Composable
internal fun SquareArea(modifier: Modifier = Modifier, maxSide: Dp = Dp.Infinity, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight, maxSide)
        Box(Modifier.size(side)) { content() }
    }
}

/** Art area on the left, the info column on the right; used for landscape and wide windows. */
@Composable
internal fun SideBySide(
    slots: NowPlayingSlots,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxSize().padding(LayoutPadding),
        horizontalArrangement = Arrangement.spacedBy(LayoutPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(0.45f).fillMaxSize(), contentAlignment = Alignment.Center) { art() }
        InfoColumn(slots, Modifier.weight(0.55f).fillMaxSize(), scrollable = true)
    }
}

/** Art area above the info column, for portrait. */
@Composable
internal fun Stacked(
    slots: NowPlayingSlots,
    modifier: Modifier = Modifier,
    art: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = LayoutPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { art() }
        InfoColumn(slots, centeredInfo = false)
    }
}
