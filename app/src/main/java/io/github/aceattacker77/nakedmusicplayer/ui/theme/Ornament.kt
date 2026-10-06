package io.github.aceattacker77.nakedmusicplayer.ui.theme

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
) {
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
)

/** Off outside [AppTheme], so composables rendered without a theme (tests, previews) look as before. */
val LocalOrnament = staticCompositionLocalOf { Ornament.OFF }
