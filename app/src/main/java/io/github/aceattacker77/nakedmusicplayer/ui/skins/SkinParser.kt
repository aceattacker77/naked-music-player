package io.github.aceattacker77.nakedmusicplayer.ui.skins

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object SkinParser {
    const val FORMAT = 1

    private val json = Json { ignoreUnknownKeys = true }
    private val idPattern = Regex("^[a-z0-9_-]+(\\.[a-z0-9_-]+)*$")
    private val colourPattern = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

    /** Internal control flow only: carries the user-facing message out of the parsing steps. */
    private class SkinError(message: String) : Exception(message)

    fun parse(json: String, defaults: Skin): SkinParseResult {
        val dto = try {
            this.json.decodeFromString(SkinJson.serializer(), json)
        } catch (e: SerializationException) {
            return SkinParseResult.Error("skin.json is not valid JSON")
        } catch (e: IllegalArgumentException) {
            return SkinParseResult.Error("skin.json is not valid JSON")
        }
        return try {
            build(dto, defaults)
        } catch (e: SkinError) {
            SkinParseResult.Error(e.message.orEmpty())
        }
    }

    private fun build(dto: SkinJson, defaults: Skin): SkinParseResult.Ok {
        val format = dto.format ?: throw SkinError("missing field 'format'")
        if (format != FORMAT) throw SkinError("unsupported format $format")
        val id = dto.id ?: throw SkinError("missing field 'id'")
        if (id.length > 64 || !idPattern.matches(id)) throw SkinError("invalid id '$id'")
        val name = dto.name?.takeIf { it.isNotBlank() } ?: throw SkinError("missing field 'name'")

        val referenced = linkedSetOf<String>()

        val colorMode = dto.colors?.mode?.let { enumOf("colors.mode", it, COLOR_MODES) } ?: defaults.colorMode
        val light = dto.colors?.light?.let { scheme(it, isDark = false, base = defaults.light) } ?: defaults.light
        val dark = dto.colors?.dark?.let { scheme(it, isDark = true, base = defaults.dark) } ?: defaults.dark

        val typography = dto.typography
        val fontPath = typography?.fontFamily
        val headingFontPath = typography?.headingFontFamily
        val bodyFontPath = typography?.bodyFontFamily
        val labelFontPath = typography?.labelFontFamily
        listOf(fontPath, headingFontPath, bodyFontPath, labelFontPath).forEach { it?.let(referenced::add) }

        val p = dto.player
        val background = p?.background?.let { bg ->
            when (val type = bg.type) {
                null -> defaults.player.background
                else -> when (enumOf("player.background.type", type, BACKGROUNDS)) {
                    "blurredArt" -> BackgroundStyle.BlurredArt
                    "artGradient" -> BackgroundStyle.ArtGradient
                    "solid" -> BackgroundStyle.Solid
                    else -> {
                        val path = bg.path ?: throw SkinError("missing field 'player.background.path'")
                        referenced += path
                        BackgroundStyle.Image(path)
                    }
                }
            }
        } ?: defaults.player.background

        val artShape = p?.artShape?.type?.let { type ->
            when (enumOf("player.artShape.type", type, ART_SHAPES)) {
                "square" -> ArtShape.Square
                "circle" -> ArtShape.Circle
                else -> ArtShape.Rounded(p.artShape.radiusDp ?: DEFAULT_ART_RADIUS_DP)
            }
        } ?: defaults.player.artShape

        val player = PlayerStyle(
            background = background,
            artShape = artShape,
            artSpin = p?.artSpin ?: defaults.player.artSpin,
            seekBar = p?.seekBar?.let { SeekBarStyle.entries[enumIndex("player.seekBar", it, SEEK_BARS)] }
                ?: defaults.player.seekBar,
            controls = p?.controls?.let { ControlsStyle.entries[enumIndex("player.controls", it, CONTROLS)] }
                ?: defaults.player.controls,
            controlSize = p?.controlSize?.let { ControlSize.entries[enumIndex("player.controlSize", it, CONTROL_SIZES)] }
                ?: defaults.player.controlSize,
            glow = p?.glow ?: defaults.player.glow,
            shadow = p?.shadow ?: defaults.player.shadow,
            useArtColors = p?.useArtColors ?: defaults.player.useArtColors,
            controlShape = p?.controlShape?.let { ControlShape.entries[enumIndex("player.controlShape", it, CONTROL_SHAPES)] }
                ?: defaults.player.controlShape,
            artPlaceholder = p?.artPlaceholder?.let { ArtPlaceholder.entries[enumIndex("player.artPlaceholder", it, ART_PLACEHOLDERS)] }
                ?: defaults.player.artPlaceholder,
            artBorder = p?.artBorder ?: defaults.player.artBorder,
            seekSegments = p?.seekSegments?.also { requireIn("player.seekSegments", it, 12..60) } ?: defaults.player.seekSegments,
            seekColor = p?.seekColor?.let { SeekColor.entries[enumIndex("player.seekColor", it, SEEK_COLORS)] }
                ?: defaults.player.seekColor,
        )

        val layout = LayoutSpec(
            type = dto.layout?.type?.let { LayoutType.entries[enumIndex("layout.type", it, LAYOUT_TYPES)] }
                ?: defaults.layout.type,
            artPosition = dto.layout?.slots?.artPosition
                ?.let { ArtPosition.entries[enumIndex("layout.slots.artPosition", it, ART_POSITIONS)] }
                ?: defaults.layout.artPosition,
        )

        val cornerStyle = dto.shapes?.cornerStyle?.let { CornerStyle.entries[enumIndex("shapes.cornerStyle", it, CORNER_STYLES)] }
            ?: defaults.cornerStyle
        val chamferDp = dto.shapes?.chamferDp?.also { requireIn("shapes.chamferDp", it, 1..32) } ?: defaults.chamferDp
        val headingScaleX = typography?.headingScaleX?.also { requireIn("typography.headingScaleX", it, 0.5..1.0) }?.toFloat()
            ?: defaults.headingScaleX
        val labelSpacing = typography?.labelLetterSpacingEm?.also { requireIn("typography.labelLetterSpacingEm", it, 0.0..0.5) }?.toFloat()
            ?: defaults.labelLetterSpacingEm

        val skin = Skin(
            id = id,
            name = name,
            author = dto.author ?: "",
            version = dto.version ?: "1.0",
            colorMode = colorMode,
            light = light,
            dark = dark,
            fontPath = fontPath ?: defaults.fontPath,
            cornerRadiusDp = dto.shapes?.cornerRadiusDp ?: defaults.cornerRadiusDp,
            player = player,
            layout = layout,
            baseDir = null,
            headingFontPath = headingFontPath ?: defaults.headingFontPath,
            bodyFontPath = bodyFontPath ?: defaults.bodyFontPath,
            labelFontPath = labelFontPath ?: defaults.labelFontPath,
            cornerStyle = cornerStyle,
            chamferDp = chamferDp,
            headingScaleX = headingScaleX,
            labelCaps = typography?.labelCaps ?: defaults.labelCaps,
            labelLetterSpacingEm = labelSpacing,
            brackets = dto.components?.brackets ?: defaults.brackets,
            segmentedMeters = dto.components?.segmentedMeters ?: defaults.segmentedMeters,
            navStyle = dto.components?.navStyle?.let { NavStyle.entries[enumIndex("components.navStyle", it, NAV_STYLES)] }
                ?: defaults.navStyle,
            rowEdge = dto.components?.rowEdge ?: defaults.rowEdge,
        )
        return SkinParseResult.Ok(skin, referenced)
    }

    // Allowed JSON spellings, in the same order as the enum entries they map to (where an enum exists).
    private val COLOR_MODES = mapOf("light" to ColorMode.LIGHT, "dark" to ColorMode.DARK, "both" to ColorMode.BOTH, "system" to ColorMode.SYSTEM)
    private val BACKGROUNDS = listOf("blurredArt", "artGradient", "solid", "image")
    private val ART_SHAPES = listOf("square", "rounded", "circle")
    private val SEEK_BARS = listOf("wavy", "flat", "thin", "segmented")
    private val CONTROLS = listOf("filled", "outlined", "iconOnly", "mixed")
    private val CONTROL_SHAPES = listOf("circle", "theme")
    private val ART_PLACEHOLDERS = listOf("note", "hexagon")
    private val SEEK_COLORS = listOf("primary", "tertiary")
    private val NAV_STYLES = listOf("material", "block")
    private val CORNER_STYLES = listOf("round", "chamfer")
    private val CONTROL_SIZES = listOf("small", "medium", "large")
    private val LAYOUT_TYPES = listOf("classic", "vinyl", "minimal", "cassette", "compact")
    private val ART_POSITIONS = listOf("top", "left", "center")
    private const val DEFAULT_ART_RADIUS_DP = 28

    private fun enumOf(field: String, value: String, options: Map<String, ColorMode>): ColorMode =
        options[value] ?: throw SkinError("invalid value '$value' for '$field'")

    private fun enumOf(field: String, value: String, options: List<String>): String {
        if (value !in options) throw SkinError("invalid value '$value' for '$field'")
        return value
    }

    private fun <T : Comparable<T>> requireIn(field: String, value: T, range: ClosedRange<T>) {
        if (value !in range) throw SkinError("invalid value '$value' for '$field'")
    }

    private fun enumIndex(field: String, value: String, options: List<String>): Int {
        val index = options.indexOf(value)
        if (index < 0) throw SkinError("invalid value '$value' for '$field'")
        return index
    }

    private fun parseColour(role: String, text: String): Color {
        if (!colourPattern.matches(text)) throw SkinError("invalid colour '$role': '$text'")
        val digits = text.substring(1)
        val argb = if (digits.length == 6) 0xFF000000L or digits.toLong(16) else digits.toLong(16)
        return Color(argb.toInt())
    }

    /**
     * Builds a scheme from the roles a skin lists. When `primary` is given the remaining roles are
     * generated from it; otherwise they come from [base]. Listed roles always win.
     */
    private fun scheme(roles: Map<String, String>, isDark: Boolean, base: ColorScheme?): ColorScheme {
        val colours = roles.filterKeys { it in ROLE_NAMES }.mapValues { (role, text) -> parseColour(role, text) }
        val seed = colours["primary"]
        var scheme = when {
            seed != null -> dynamicColorScheme(seedColor = seed, isDark = isDark)
            base != null -> base
            isDark -> darkColorScheme()
            else -> lightColorScheme()
        }
        colours.forEach { (role, colour) -> scheme = scheme.withRole(role, colour) }
        return scheme
    }

    private val ROLE_NAMES = setOf(
        "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
        "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
        "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
        "background", "onBackground", "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
        "outline", "outlineVariant", "error", "onError", "errorContainer", "onErrorContainer",
        "inverseSurface", "inverseOnSurface", "inversePrimary", "scrim",
        "surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow",
        "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest", "surfaceTint",
    )

    private fun ColorScheme.withRole(role: String, c: Color): ColorScheme = when (role) {
        "primary" -> copy(primary = c)
        "onPrimary" -> copy(onPrimary = c)
        "primaryContainer" -> copy(primaryContainer = c)
        "onPrimaryContainer" -> copy(onPrimaryContainer = c)
        "secondary" -> copy(secondary = c)
        "onSecondary" -> copy(onSecondary = c)
        "secondaryContainer" -> copy(secondaryContainer = c)
        "onSecondaryContainer" -> copy(onSecondaryContainer = c)
        "tertiary" -> copy(tertiary = c)
        "onTertiary" -> copy(onTertiary = c)
        "tertiaryContainer" -> copy(tertiaryContainer = c)
        "onTertiaryContainer" -> copy(onTertiaryContainer = c)
        "background" -> copy(background = c)
        "onBackground" -> copy(onBackground = c)
        "surface" -> copy(surface = c)
        "onSurface" -> copy(onSurface = c)
        "surfaceVariant" -> copy(surfaceVariant = c)
        "onSurfaceVariant" -> copy(onSurfaceVariant = c)
        "outline" -> copy(outline = c)
        "outlineVariant" -> copy(outlineVariant = c)
        "error" -> copy(error = c)
        "onError" -> copy(onError = c)
        "errorContainer" -> copy(errorContainer = c)
        "onErrorContainer" -> copy(onErrorContainer = c)
        "inverseSurface" -> copy(inverseSurface = c)
        "inverseOnSurface" -> copy(inverseOnSurface = c)
        "inversePrimary" -> copy(inversePrimary = c)
        "scrim" -> copy(scrim = c)
        "surfaceDim" -> copy(surfaceDim = c)
        "surfaceBright" -> copy(surfaceBright = c)
        "surfaceContainerLowest" -> copy(surfaceContainerLowest = c)
        "surfaceContainerLow" -> copy(surfaceContainerLow = c)
        "surfaceContainer" -> copy(surfaceContainer = c)
        "surfaceContainerHigh" -> copy(surfaceContainerHigh = c)
        "surfaceContainerHighest" -> copy(surfaceContainerHighest = c)
        "surfaceTint" -> copy(surfaceTint = c)
        else -> this
    }
}
