package io.github.aceattacker77.nakedmusicplayer.widget

import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin

/**
 * The parts of a skin's ornament the home-screen widget can draw. The widget is a RemoteViews surface, so it never
 * chamfers or uses skin fonts; it takes the frame, brackets, hexagon tile, tile border and segmented progress.
 */
data class WidgetStyle(
    val brackets: Boolean,
    val hexagonTile: Boolean,
    val tileBorder: Boolean,
    val segmentedProgress: Boolean,
    val segments: Int,
    val seekColor: SeekColor,
) {
    companion object {
        /** No ornament: the widget exactly as it was before skins could ask for any. */
        val PLAIN = WidgetStyle(
            brackets = false, hexagonTile = false, tileBorder = false, segmentedProgress = false, segments = 40, seekColor = SeekColor.PRIMARY,
        )
    }
}

/** Reuses the Phase 2 fields: `components.brackets`, `player.artPlaceholder`, `player.artBorder` and `player.seekBar`. */
fun widgetStyleOf(skin: Skin) = WidgetStyle(
    brackets = skin.brackets,
    hexagonTile = skin.player.artPlaceholder == ArtPlaceholder.HEXAGON,
    tileBorder = skin.player.artBorder,
    segmentedProgress = skin.player.seekBar == SeekBarStyle.SEGMENTED,
    segments = skin.player.seekSegments,
    seekColor = skin.player.seekColor,
)
