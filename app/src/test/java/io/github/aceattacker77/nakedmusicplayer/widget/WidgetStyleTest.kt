package io.github.aceattacker77.nakedmusicplayer.widget

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Test

class WidgetStyleTest {
    private fun skin(
        placeholder: ArtPlaceholder = ArtPlaceholder.NOTE,
        border: Boolean = false,
        seekBar: SeekBarStyle = SeekBarStyle.WAVY,
        brackets: Boolean = false,
    ) = Skin.FALLBACK.copy(
        brackets = brackets,
        player = Skin.FALLBACK.player.copy(artPlaceholder = placeholder, artBorder = border, seekBar = seekBar, seekSegments = 24, seekColor = SeekColor.TERTIARY),
    )

    @Test fun plainSkin_givesThePlainStyle() {
        assertThat(widgetStyleOf(Skin.FALLBACK)).isEqualTo(WidgetStyle.PLAIN)
    }

    @Test fun brackets_turnOnTheFrameAndBrackets() {
        assertThat(widgetStyleOf(skin(brackets = true)).brackets).isTrue()
        assertThat(widgetStyleOf(skin()).brackets).isFalse()
    }

    @Test fun hexagonPlaceholder_turnsOnTheHexagonTile() {
        assertThat(widgetStyleOf(skin(placeholder = ArtPlaceholder.HEXAGON)).hexagonTile).isTrue()
        assertThat(widgetStyleOf(skin()).hexagonTile).isFalse()
    }

    @Test fun artBorder_turnsOnTheTileBorder() {
        assertThat(widgetStyleOf(skin(border = true)).tileBorder).isTrue()
        assertThat(widgetStyleOf(skin()).tileBorder).isFalse()
    }

    @Test fun segmentedSeekBar_turnsOnSegmentedProgress_withTheSkinsCellsAndColour() {
        val style = widgetStyleOf(skin(seekBar = SeekBarStyle.SEGMENTED))
        assertThat(style.segmentedProgress).isTrue()
        assertThat(style.segments).isEqualTo(24)
        assertThat(style.seekColor).isEqualTo(SeekColor.TERTIARY)
        assertThat(widgetStyleOf(skin(seekBar = SeekBarStyle.FLAT)).segmentedProgress).isFalse()
    }
}
