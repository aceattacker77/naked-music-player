package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
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
}
