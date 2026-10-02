package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import io.github.aceattacker77.nakedmusicplayer.ui.player.layouts.CassetteLayout
import io.github.aceattacker77.nakedmusicplayer.ui.player.layouts.ClassicLayout
import io.github.aceattacker77.nakedmusicplayer.ui.player.layouts.CompactLayout
import io.github.aceattacker77.nakedmusicplayer.ui.player.layouts.MinimalLayout
import io.github.aceattacker77.nakedmusicplayer.ui.player.layouts.VinylLayout
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition
import io.github.aceattacker77.nakedmusicplayer.ui.skins.LayoutSpec
import io.github.aceattacker77.nakedmusicplayer.ui.skins.LayoutType

/**
 * The pieces of Now Playing, built once by [NowPlayingScreen]; a skin's layout only decides where
 * each goes. The artwork slot fills whatever space the layout gives it.
 */
@Immutable
class NowPlayingSlots(
    val artwork: @Composable () -> Unit,
    val trackInfo: @Composable () -> Unit,
    val seekBar: @Composable () -> Unit,
    val controls: @Composable () -> Unit,
    val secondaryControls: @Composable () -> Unit,
    val queueHandle: @Composable () -> Unit,
)

private const val WIDE_WIDTH_DP = 600

/** Arranges [slots] per [spec]. Landscape and wide windows always put the artwork on the left. */
@Composable
fun NowPlayingLayout(spec: LayoutSpec, slots: NowPlayingSlots, modifier: Modifier = Modifier) {
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= WIDE_WIDTH_DP || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val position = if (wide) ArtPosition.LEFT else spec.artPosition
    when (spec.type) {
        LayoutType.CLASSIC -> ClassicLayout(slots, position, modifier)
        LayoutType.VINYL -> VinylLayout(slots, position, modifier)
        LayoutType.MINIMAL -> MinimalLayout(slots, position, modifier)
        LayoutType.CASSETTE -> CassetteLayout(slots, position, modifier)
        LayoutType.COMPACT -> CompactLayout(slots, modifier)
    }
}
