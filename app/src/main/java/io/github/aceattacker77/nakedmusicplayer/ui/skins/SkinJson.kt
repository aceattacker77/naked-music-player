package io.github.aceattacker77.nakedmusicplayer.ui.skins

import kotlinx.serialization.Serializable

/** Wire format of skin.json. Every field is nullable so that partial skins parse and defaults apply. */
@Serializable
data class SkinJson(
    val format: Int? = null,
    val id: String? = null,
    val name: String? = null,
    val author: String? = null,
    val version: String? = null,
    val colors: ColorsJson? = null,
    val typography: TypographyJson? = null,
    val shapes: ShapesJson? = null,
    val player: PlayerJson? = null,
    val layout: LayoutJson? = null,
)

@Serializable
data class ColorsJson(
    val mode: String? = null,
    val light: Map<String, String>? = null,
    val dark: Map<String, String>? = null,
)

@Serializable
data class TypographyJson(
    val fontFamily: String? = null,
    val headingFontFamily: String? = null,
    val bodyFontFamily: String? = null,
    val labelFontFamily: String? = null,
    val headingScaleX: Float? = null,
    val labelCaps: Boolean? = null,
    val labelLetterSpacingEm: Float? = null,
)

@Serializable
data class ShapesJson(
    val cornerRadiusDp: Int? = null,
    val cornerStyle: String? = null,
    val chamferDp: Int? = null,
)

@Serializable
data class PlayerJson(
    val background: BackgroundJson? = null,
    val artShape: ArtShapeJson? = null,
    val artSpin: Boolean? = null,
    val seekBar: String? = null,
    val controls: String? = null,
    val controlSize: String? = null,
    val glow: Boolean? = null,
    val shadow: Boolean? = null,
    val useArtColors: Boolean? = null,
    val controlShape: String? = null,
)

@Serializable
data class BackgroundJson(val type: String? = null, val path: String? = null)

@Serializable
data class ArtShapeJson(val type: String? = null, val radiusDp: Int? = null)

@Serializable
data class LayoutJson(val type: String? = null, val slots: SlotsJson? = null)

@Serializable
data class SlotsJson(val artPosition: String? = null)
