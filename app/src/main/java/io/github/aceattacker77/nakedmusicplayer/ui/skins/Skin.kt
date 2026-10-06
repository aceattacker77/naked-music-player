package io.github.aceattacker77.nakedmusicplayer.ui.skins

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import java.io.File

/**
 * LIGHT / DARK: always that scheme. BOTH: light or dark per the theme setting.
 * SYSTEM: dynamic colour on API 31+ when enabled, otherwise the Default skin's scheme.
 */
enum class ColorMode { LIGHT, DARK, BOTH, SYSTEM }

sealed interface BackgroundStyle {
    data object BlurredArt : BackgroundStyle
    data object ArtGradient : BackgroundStyle
    data object Solid : BackgroundStyle
    data class Image(val path: String) : BackgroundStyle
}

sealed interface ArtShape {
    data object Square : ArtShape
    data class Rounded(val radiusDp: Int) : ArtShape
    data object Circle : ArtShape
}

enum class SeekBarStyle { WAVY, FLAT, THIN, SEGMENTED }
enum class ControlsStyle { FILLED, OUTLINED, ICON_ONLY, MIXED }
enum class ControlShape { CIRCLE, THEME }
enum class CornerStyle { ROUND, CHAMFER }
enum class ArtPlaceholder { NOTE, HEXAGON }
enum class SeekColor { PRIMARY, TERTIARY }
enum class NavStyle { MATERIAL, BLOCK }
enum class ControlSize { SMALL, MEDIUM, LARGE }
enum class LayoutType { CLASSIC, VINYL, MINIMAL, CASSETTE, COMPACT }
enum class ArtPosition { TOP, LEFT, CENTER }

data class PlayerStyle(
    val background: BackgroundStyle,
    val artShape: ArtShape,
    val artSpin: Boolean,
    val seekBar: SeekBarStyle,
    val controls: ControlsStyle,
    val controlSize: ControlSize,
    val glow: Boolean,
    val shadow: Boolean,
    val useArtColors: Boolean,
    val controlShape: ControlShape = ControlShape.CIRCLE,
    val artPlaceholder: ArtPlaceholder = ArtPlaceholder.NOTE,
    val artBorder: Boolean = false,
    val seekSegments: Int = 40,
    val seekColor: SeekColor = SeekColor.PRIMARY,
)

data class LayoutSpec(val type: LayoutType, val artPosition: ArtPosition)

/** The three text roles a skin can give its own font. */
enum class FontRole { HEADING, BODY, LABEL }

data class Skin(
    val id: String,
    val name: String,
    val author: String,
    val version: String,
    val colorMode: ColorMode,
    val light: ColorScheme?,
    val dark: ColorScheme?,
    val fontPath: String?,
    val cornerRadiusDp: Int,
    val player: PlayerStyle,
    val layout: LayoutSpec,
    /** Directory holding the skin's files; null for a skin that is not installed yet. */
    val baseDir: File?,
    val headingFontPath: String? = null,
    val bodyFontPath: String? = null,
    val labelFontPath: String? = null,
    val cornerStyle: CornerStyle = CornerStyle.ROUND,
    val chamferDp: Int = 10,
    val headingScaleX: Float = 1f,
    val labelCaps: Boolean = false,
    val labelLetterSpacingEm: Float = 0f,
    val brackets: Boolean = false,
    val segmentedMeters: Boolean = false,
    val navStyle: NavStyle = NavStyle.MATERIAL,
    val rowEdge: Boolean = false,
) {
    /** The font file for [role]: its own entry when the skin sets one, else the shared `fontFamily`. */
    fun fontPathFor(role: FontRole): String? = when (role) {
        FontRole.HEADING -> headingFontPath
        FontRole.BODY -> bodyFontPath
        FontRole.LABEL -> labelFontPath
    } ?: fontPath

    companion object {
        /** Hard-coded equivalent of the Default skin; only used to bootstrap parsing of `builtin.default`. */
        val FALLBACK = Skin(
            id = "builtin.default",
            name = "Default",
            author = "",
            version = "1",
            colorMode = ColorMode.SYSTEM,
            light = lightColorScheme(),
            dark = darkColorScheme(),
            fontPath = null,
            cornerRadiusDp = 28,
            player = PlayerStyle(
                background = BackgroundStyle.ArtGradient,
                artShape = ArtShape.Rounded(28),
                artSpin = false,
                seekBar = SeekBarStyle.WAVY,
                controls = ControlsStyle.FILLED,
                controlSize = ControlSize.MEDIUM,
                glow = false,
                shadow = false,
                useArtColors = true,
            ),
            layout = LayoutSpec(LayoutType.CLASSIC, ArtPosition.TOP),
            baseDir = null,
        )
    }
}

sealed interface SkinParseResult {
    data class Ok(val skin: Skin, val referencedFiles: Set<String>) : SkinParseResult
    data class Error(val message: String) : SkinParseResult
}
