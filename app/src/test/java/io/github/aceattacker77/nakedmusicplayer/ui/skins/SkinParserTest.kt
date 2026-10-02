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
}
