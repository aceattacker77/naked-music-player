package io.github.aceattacker77.nakedmusicplayer.ui.skins

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SkinParserTest {
    private val defaults = Skin.FALLBACK

    // Spec section 5.3 example, verbatim.
    private val example = """
        {
          "format": 1,
          "id": "com.example.neon",
          "name": "Neon",
          "author": "Someone",
          "version": "1.0",
          "colors": {
            "mode": "dark",
            "dark": { "primary": "#00E5FF", "background": "#0A0A12", "surface": "#141420", "onPrimary": "#000000" }
          },
          "typography": { "fontFamily": "fonts/Orbitron.ttf" },
          "shapes": { "cornerRadiusDp": 20 },
          "player": {
            "background": { "type": "artGradient" },
            "artShape": { "type": "rounded", "radiusDp": 28 },
            "artSpin": false,
            "seekBar": "wavy",
            "controls": "filled",
            "controlSize": "large",
            "glow": true,
            "useArtColors": false
          },
          "layout": { "type": "classic", "slots": { "artPosition": "top" } }
        }
    """.trimIndent()

    private fun ok(json: String) = (SkinParser.parse(json, defaults) as SkinParseResult.Ok)
    private fun error(json: String) = (SkinParser.parse(json, defaults) as SkinParseResult.Error).message

    private fun minimal(extra: String = "") = """{"format":1,"id":"a.b","name":"X"$extra}"""

    @Test fun parsesSpecExample() {
        val result = ok(example)
        val skin = result.skin
        assertThat(skin.id).isEqualTo("com.example.neon")
        assertThat(skin.name).isEqualTo("Neon")
        assertThat(skin.author).isEqualTo("Someone")
        assertThat(skin.version).isEqualTo("1.0")
        assertThat(skin.colorMode).isEqualTo(ColorMode.DARK)
        assertThat(skin.dark!!.primary).isEqualTo(Color(0xFF00E5FF))
        assertThat(skin.dark!!.background).isEqualTo(Color(0xFF0A0A12))
        assertThat(skin.dark!!.onPrimary).isEqualTo(Color(0xFF000000))
        assertThat(skin.cornerRadiusDp).isEqualTo(20)
        assertThat(skin.fontPath).isEqualTo("fonts/Orbitron.ttf")
        assertThat(skin.player.background).isEqualTo(BackgroundStyle.ArtGradient)
        assertThat(skin.player.artShape).isEqualTo(ArtShape.Rounded(28))
        assertThat(skin.player.artSpin).isFalse()
        assertThat(skin.player.seekBar).isEqualTo(SeekBarStyle.WAVY)
        assertThat(skin.player.controls).isEqualTo(ControlsStyle.FILLED)
        assertThat(skin.player.controlSize).isEqualTo(ControlSize.LARGE)
        assertThat(skin.player.glow).isTrue()
        assertThat(skin.player.useArtColors).isFalse()
        assertThat(skin.layout).isEqualTo(LayoutSpec(LayoutType.CLASSIC, ArtPosition.TOP))
        assertThat(result.referencedFiles).containsExactly("fonts/Orbitron.ttf")
    }

    @Test fun partialSkin_inheritsDefaults() {
        val skin = ok(minimal()).skin
        assertThat(skin.player).isEqualTo(defaults.player)
        assertThat(skin.layout).isEqualTo(defaults.layout)
        assertThat(skin.cornerRadiusDp).isEqualTo(defaults.cornerRadiusDp)
        assertThat(skin.colorMode).isEqualTo(defaults.colorMode)
        assertThat(ok(minimal()).referencedFiles).isEmpty()
    }

    @Test fun unlistedColorRoles_derivedFromPrimary() {
        val skin = ok(minimal(""","colors":{"mode":"light","light":{"primary":"#C0392B"}}""")).skin
        val light = skin.light!!
        assertThat(light.primary).isEqualTo(Color(0xFFC0392B))
        assertThat(light.secondary).isNotEqualTo(Color.Unspecified)
        assertThat(light.surface).isNotEqualTo(Color.Unspecified)
        assertThat(light.secondary).isNotEqualTo(defaults.light!!.secondary)
    }

    @Test fun argbColour_isAccepted() {
        val skin = ok(minimal(""","colors":{"mode":"dark","dark":{"primary":"#80FF0000"}}""")).skin
        assertThat(skin.dark!!.primary).isEqualTo(Color(0x80FF0000))
    }

    @Test fun backgroundImage_isParsedAndReferenced() {
        val result = ok(minimal(""","player":{"background":{"type":"image","path":"images/bg.webp"}}"""))
        assertThat(result.skin.player.background).isEqualTo(BackgroundStyle.Image("images/bg.webp"))
        assertThat(result.referencedFiles).containsExactly("images/bg.webp")
    }

    @Test fun unknownExtraFields_areIgnored() {
        assertThat(SkinParser.parse(minimal(""","futureThing":{"a":1},"player":{"newKnob":true}"""), defaults))
            .isInstanceOf(SkinParseResult.Ok::class.java)
    }

    @Test fun missingFormat_isError() {
        assertThat(error("""{"id":"a.b","name":"X"}""")).isEqualTo("missing field 'format'")
    }

    @Test fun unsupportedFormat_isError() {
        assertThat(error("""{"format":2,"id":"a.b","name":"X"}""")).isEqualTo("unsupported format 2")
    }

    @Test fun invalidId_isError() {
        assertThat(error("""{"format":1,"id":"Bad Id","name":"X"}""")).isEqualTo("invalid id 'Bad Id'")
        val tooLong = "a".repeat(65)
        assertThat(error("""{"format":1,"id":"$tooLong","name":"X"}""")).isEqualTo("invalid id '$tooLong'")
    }

    @Test fun invalidColour_isError() {
        assertThat(error(minimal(""","colors":{"dark":{"primary":"#GGG"}}""")))
            .isEqualTo("invalid colour 'primary': '#GGG'")
    }

    @Test fun malformedJson_isError() {
        assertThat(error("{ not json")).isEqualTo("skin.json is not valid JSON")
    }

    @Test fun unknownEnum_isError() {
        assertThat(error(minimal(""","player":{"seekBar":"zigzag"}""")))
            .isEqualTo("invalid value 'zigzag' for 'player.seekBar'")
        assertThat(error(minimal(""","layout":{"type":"hologram"}""")))
            .isEqualTo("invalid value 'hologram' for 'layout.type'")
        assertThat(error(minimal(""","colors":{"mode":"neon"}""")))
            .isEqualTo("invalid value 'neon' for 'colors.mode'")
    }

    @Test fun roleFonts_areParsedAndReferenced() {
        val json = minimal(""","typography":{"fontFamily":"fonts/a.ttf","headingFontFamily":"fonts/h.ttf","bodyFontFamily":"fonts/b.otf","labelFontFamily":"fonts/l.ttf"}""")
        val result = ok(json)
        assertThat(result.skin.fontPath).isEqualTo("fonts/a.ttf")
        assertThat(result.skin.headingFontPath).isEqualTo("fonts/h.ttf")
        assertThat(result.skin.bodyFontPath).isEqualTo("fonts/b.otf")
        assertThat(result.skin.labelFontPath).isEqualTo("fonts/l.ttf")
        assertThat(result.referencedFiles).containsExactly("fonts/a.ttf", "fonts/h.ttf", "fonts/b.otf", "fonts/l.ttf")
    }

    @Test fun roleFonts_unspecifiedStayNull_andFallBackToFontFamily() {
        val skin = ok(minimal(""","typography":{"fontFamily":"fonts/a.ttf","headingFontFamily":"fonts/h.ttf"}""")).skin
        assertThat(skin.bodyFontPath).isNull()
        assertThat(skin.labelFontPath).isNull()
        assertThat(skin.fontPathFor(FontRole.HEADING)).isEqualTo("fonts/h.ttf")
        assertThat(skin.fontPathFor(FontRole.BODY)).isEqualTo("fonts/a.ttf")
        assertThat(skin.fontPathFor(FontRole.LABEL)).isEqualTo("fonts/a.ttf")
    }

    @Test fun noFonts_resolveToNull() {
        val skin = ok(minimal()).skin
        FontRole.entries.forEach { assertThat(skin.fontPathFor(it)).isNull() }
    }

    @Test fun surfaceContainerRoles_areSettable() {
        val roles = listOf(
            "surfaceDim", "surfaceBright", "surfaceContainerLowest", "surfaceContainerLow",
            "surfaceContainer", "surfaceContainerHigh", "surfaceContainerHighest", "surfaceTint",
        )
        val colours = roles.mapIndexed { i, r -> r to "#%02X%02X%02X".format(i + 1, i + 2, i + 3) }
        val body = colours.joinToString(",") { (r, c) -> "\"$r\":\"$c\"" }
        val dark = ok(minimal(""","colors":{"dark":{$body}}""")).skin.dark!!
        assertThat(dark.surfaceDim).isEqualTo(Color(0xFF010203))
        assertThat(dark.surfaceBright).isEqualTo(Color(0xFF020304))
        assertThat(dark.surfaceContainerLowest).isEqualTo(Color(0xFF030405))
        assertThat(dark.surfaceContainerLow).isEqualTo(Color(0xFF040506))
        assertThat(dark.surfaceContainer).isEqualTo(Color(0xFF050607))
        assertThat(dark.surfaceContainerHigh).isEqualTo(Color(0xFF060708))
        assertThat(dark.surfaceContainerHighest).isEqualTo(Color(0xFF070809))
        assertThat(dark.surfaceTint).isEqualTo(Color(0xFF08090A))
    }

    @Test fun invalidContainerColour_isError() {
        assertThat(error(minimal(""","colors":{"light":{"surfaceContainerHigh":"x"}}""")))
            .isEqualTo("invalid colour 'surfaceContainerHigh': 'x'")
    }

    @Test fun newFields_defaultToToday() {
        val skin = ok(minimal()).skin
        assertThat(skin.cornerStyle).isEqualTo(CornerStyle.ROUND)
        assertThat(skin.chamferDp).isEqualTo(10)
        assertThat(skin.headingScaleX).isEqualTo(1f)
        assertThat(skin.labelCaps).isFalse()
        assertThat(skin.labelLetterSpacingEm).isEqualTo(0f)
        assertThat(skin.player.controlShape).isEqualTo(ControlShape.CIRCLE)
        assertThat(skin.player.controls).isEqualTo(ControlsStyle.FILLED)
    }

    @Test fun newFields_areParsed() {
        val json = minimal(
            ""","shapes":{"cornerRadiusDp":28,"cornerStyle":"chamfer","chamferDp":12},""" +
                """"typography":{"headingScaleX":0.8,"labelCaps":true,"labelLetterSpacingEm":0.14},""" +
                """"player":{"controls":"mixed","controlShape":"theme"}""",
        )
        val skin = ok(json).skin
        assertThat(skin.cornerStyle).isEqualTo(CornerStyle.CHAMFER)
        assertThat(skin.chamferDp).isEqualTo(12)
        assertThat(skin.cornerRadiusDp).isEqualTo(28)
        assertThat(skin.headingScaleX).isEqualTo(0.8f)
        assertThat(skin.labelCaps).isTrue()
        assertThat(skin.labelLetterSpacingEm).isEqualTo(0.14f)
        assertThat(skin.player.controls).isEqualTo(ControlsStyle.MIXED)
        assertThat(skin.player.controlShape).isEqualTo(ControlShape.THEME)
    }

    @Test fun boundaries_areAccepted() {
        listOf(
            ""","typography":{"headingScaleX":0.5,"labelLetterSpacingEm":0.0}""",
            ""","typography":{"headingScaleX":1.0,"labelLetterSpacingEm":0.5}""",
            ""","shapes":{"chamferDp":1}""",
            ""","shapes":{"chamferDp":32}""",
        ).forEach { ok(minimal(it)) }
    }

    @Test fun outOfRange_isError() {
        assertThat(error(minimal(""","typography":{"headingScaleX":0.49}""")))
            .isEqualTo("invalid value '0.49' for 'typography.headingScaleX'")
        assertThat(error(minimal(""","typography":{"headingScaleX":1.01}""")))
            .isEqualTo("invalid value '1.01' for 'typography.headingScaleX'")
        assertThat(error(minimal(""","shapes":{"chamferDp":0}""")))
            .isEqualTo("invalid value '0' for 'shapes.chamferDp'")
        assertThat(error(minimal(""","shapes":{"chamferDp":33}""")))
            .isEqualTo("invalid value '33' for 'shapes.chamferDp'")
        assertThat(error(minimal(""","typography":{"labelLetterSpacingEm":0.51}""")))
            .isEqualTo("invalid value '0.51' for 'typography.labelLetterSpacingEm'")
        assertThat(error(minimal(""","typography":{"labelLetterSpacingEm":-0.1}""")))
            .isEqualTo("invalid value '-0.1' for 'typography.labelLetterSpacingEm'")
    }

    @Test fun unknownNewEnums_areErrors() {
        assertThat(error(minimal(""","shapes":{"cornerStyle":"smooth"}""")))
            .isEqualTo("invalid value 'smooth' for 'shapes.cornerStyle'")
        assertThat(error(minimal(""","player":{"controlShape":"square"}""")))
            .isEqualTo("invalid value 'square' for 'player.controlShape'")
    }

    @Test fun wrongType_isNotValidJson() {
        assertThat(error(minimal(""","typography":{"headingScaleX":"wide"}""")))
            .isEqualTo("skin.json is not valid JSON")
    }

    @Test fun newFields_inheritFromDefaults() {
        val base = defaults.copy(cornerStyle = CornerStyle.CHAMFER, labelCaps = true, chamferDp = 7)
        val skin = (SkinParser.parse(minimal(), base) as SkinParseResult.Ok).skin
        assertThat(skin.cornerStyle).isEqualTo(CornerStyle.CHAMFER)
        assertThat(skin.labelCaps).isTrue()
        assertThat(skin.chamferDp).isEqualTo(7)
    }
}
