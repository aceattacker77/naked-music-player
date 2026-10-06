package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.em
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinFonts
import org.junit.Test

class TypographyMappingTest {
    private val base = Typography()
    private val fonts = SkinFonts(heading = FontFamily.Serif, body = FontFamily.Monospace, label = FontFamily.Cursive)
    private val mapped = base.withFontFamilies(fonts)

    @Test fun displayHeadlineTitle_useHeadingFont() {
        listOf(
            mapped.displayLarge, mapped.displayMedium, mapped.displaySmall,
            mapped.headlineLarge, mapped.headlineMedium, mapped.headlineSmall,
            mapped.titleLarge, mapped.titleMedium, mapped.titleSmall,
        ).forEach { assertThat(it.fontFamily).isEqualTo(FontFamily.Serif) }
    }

    @Test fun body_usesBodyFont() {
        listOf(mapped.bodyLarge, mapped.bodyMedium, mapped.bodySmall)
            .forEach { assertThat(it.fontFamily).isEqualTo(FontFamily.Monospace) }
    }

    @Test fun label_usesLabelFont() {
        listOf(mapped.labelLarge, mapped.labelMedium, mapped.labelSmall)
            .forEach { assertThat(it.fontFamily).isEqualTo(FontFamily.Cursive) }
    }

    @Test fun nullRole_keepsDefaultStyle() {
        val partial = base.withFontFamilies(SkinFonts(heading = FontFamily.Serif, body = null, label = null))
        assertThat(partial.bodyLarge).isEqualTo(base.bodyLarge)
        assertThat(partial.labelSmall).isEqualTo(base.labelSmall)
        assertThat(partial.titleLarge.fontFamily).isEqualTo(FontFamily.Serif)
    }

    @Test fun allNull_leavesTypographyUntouched() {
        assertThat(base.withFontFamilies(SkinFonts(null, null, null))).isEqualTo(base)
    }

    private val noFonts = SkinFonts(null, null, null)

    @Test fun headingScale_reachesOnlyHeadings() {
        val t = base.withFontFamilies(noFonts, headingScaleX = 0.8f)
        listOf(
            t.displayLarge, t.displayMedium, t.displaySmall, t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
        ).forEach { assertThat(it.textGeometricTransform?.scaleX).isEqualTo(0.8f) }
        assertThat(t.bodyLarge).isEqualTo(base.bodyLarge)
        assertThat(t.labelMedium).isEqualTo(base.labelMedium)
    }

    @Test fun labelSpacing_reachesOnlyLabels() {
        val t = base.withFontFamilies(noFonts, labelLetterSpacingEm = 0.14f)
        listOf(t.labelLarge, t.labelMedium, t.labelSmall).forEach { assertThat(it.letterSpacing).isEqualTo(0.14.em) }
        assertThat(t.titleLarge).isEqualTo(base.titleLarge)
        assertThat(t.bodyMedium).isEqualTo(base.bodyMedium)
    }

    @Test fun defaults_leaveTypographyEqual_andKeepMaterialLabelSpacing() {
        assertThat(base.withFontFamilies(noFonts)).isEqualTo(base)
        assertThat(base.withFontFamilies(noFonts).labelLarge.letterSpacing).isEqualTo(base.labelLarge.letterSpacing)
    }

    @Test fun scaleOne_leavesGeometricTransformUntouched() {
        val t = base.withFontFamilies(noFonts, headingScaleX = 1f)
        assertThat(t.headlineSmall.textGeometricTransform).isEqualTo(base.headlineSmall.textGeometricTransform)
    }
}
