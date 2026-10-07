package io.github.aceattacker77.nakedmusicplayer.ui.theme

import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.NavStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin

/** The ornament a skin asks for; every flag off means the app looks as it always did. */
data class Ornament(
    val artPlaceholder: ArtPlaceholder,
    val artBorder: Boolean,
    val brackets: Boolean,
    val segmentedMeters: Boolean,
    val navBlock: Boolean,
    val rowEdge: Boolean,
    val statusTags: Boolean = false,
    val titleCards: Boolean = false,
    val panelHeader: Boolean = false,
    val squareSwitch: Boolean = false,
    val glow: GlowMode = GlowMode.OFF,
) {
    /** True when the skin sets any Phase 3 field (status tags, title cards, panel header, square switch). */
    val accents: Boolean get() = statusTags || titleCards || panelHeader || squareSwitch

    companion object {
        val OFF = Ornament(ArtPlaceholder.NOTE, artBorder = false, brackets = false, segmentedMeters = false, navBlock = false, rowEdge = false)
    }
}

fun ornamentOf(skin: Skin): Ornament = Ornament(
    artPlaceholder = skin.player.artPlaceholder,
    artBorder = skin.player.artBorder,
    brackets = skin.brackets,
    segmentedMeters = skin.segmentedMeters,
    navBlock = skin.navStyle == NavStyle.BLOCK,
    rowEdge = skin.rowEdge,
    statusTags = skin.statusTags,
    titleCards = skin.titleCards,
    panelHeader = skin.panelHeader,
    squareSwitch = skin.squareSwitch,
    glow = skin.glow,
)

/** Off outside [AppTheme], so composables rendered without a theme (tests, previews) look as before. */
val LocalOrnament = staticCompositionLocalOf { Ornament.OFF }
