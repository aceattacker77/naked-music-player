package io.github.aceattacker77.nakedmusicplayer.ui.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ArtColorsTest {
    private val gray = 0x808080
    private val darkGray = 0x303030
    private val vividRed = 0xE53935
    private val vividBlue = 0x1E88E5
    private val nearWhite = 0xFAFAFA

    @Test fun prefersVibrant() {
        // The gray dominates by population, but only the red is vibrant.
        assertThat(ArtColors.pickAccent(listOf(gray to 1_000, vividRed to 50, darkGray to 300))).isEqualTo(vividRed)
    }

    @Test fun morePopulatedVibrantSwatchWins() {
        assertThat(ArtColors.pickAccent(listOf(vividRed to 50, vividBlue to 400))).isEqualTo(vividBlue)
    }

    @Test fun fallsBackToDominant() {
        assertThat(ArtColors.pickAccent(listOf(gray to 100, darkGray to 700, nearWhite to 300))).isEqualTo(darkGray)
    }

    @Test fun emptyReturnsNull() {
        assertThat(ArtColors.pickAccent(emptyList())).isNull()
    }

    @Test fun nearBlackAndNearWhiteAreNotVibrant() {
        // Saturated but far too dark / too light to work as an accent, so the dominant swatch is used.
        val veryDarkBlue = 0x000030
        val veryLightPink = 0xFFE6E6
        assertThat(ArtColors.pickAccent(listOf(veryDarkBlue to 10, veryLightPink to 20, gray to 500))).isEqualTo(gray)
    }
}
