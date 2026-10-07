package io.github.aceattacker77.nakedmusicplayer.ui.theme

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.NavStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Test

class OrnamentTest {
    @Test fun off_isAllDefaults() {
        assertThat(ornamentOf(Skin.FALLBACK)).isEqualTo(Ornament.OFF)
    }

    @Test fun ornamentOf_mapsEveryField() {
        val skin = Skin.FALLBACK.copy(
            player = Skin.FALLBACK.player.copy(artPlaceholder = ArtPlaceholder.HEXAGON, artBorder = true),
            brackets = true,
            segmentedMeters = true,
            navStyle = NavStyle.BLOCK,
            rowEdge = true,
            statusTags = true,
            titleCards = true,
            panelHeader = true,
            squareSwitch = true,
            glow = GlowMode.DARK,
        )
        assertThat(ornamentOf(skin)).isEqualTo(
            Ornament(
                artPlaceholder = ArtPlaceholder.HEXAGON,
                artBorder = true,
                brackets = true,
                segmentedMeters = true,
                navBlock = true,
                rowEdge = true,
                statusTags = true,
                titleCards = true,
                panelHeader = true,
                squareSwitch = true,
                glow = GlowMode.DARK,
            ),
        )
    }

    @Test fun accents_areOnlyThePhaseThreeFields() {
        assertThat(Ornament.OFF.accents).isFalse()
        assertThat(Ornament.OFF.copy(statusTags = true).accents).isTrue()
        assertThat(Ornament.OFF.copy(titleCards = true).accents).isTrue()
        assertThat(Ornament.OFF.copy(panelHeader = true).accents).isTrue()
        assertThat(Ornament.OFF.copy(squareSwitch = true).accents).isTrue()
        // Fields from earlier phases do not switch the Phase 3 restyles on.
        assertThat(Ornament.OFF.copy(brackets = true, segmentedMeters = true, rowEdge = true, navBlock = true).accents).isFalse()
    }
}
