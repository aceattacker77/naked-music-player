package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EqualizerGeometryTest {
    @Test fun cells_rangeEndsAndMiddle() {
        assertThat(bandCellsFilled(-1500, -1500..1500, 12)).isEqualTo(0)
        assertThat(bandCellsFilled(1500, -1500..1500, 12)).isEqualTo(12)
        assertThat(bandCellsFilled(0, -1500..1500, 12)).isEqualTo(6)
    }

    @Test fun cells_sameRelativePositionGivesTheSameCountWhateverTheRange() {
        assertThat(bandCellsFilled(600, -1200..1200, 12)).isEqualTo(bandCellsFilled(750, -1500..1500, 12))
        assertThat(bandCellsFilled(600, -1200..1200, 12)).isEqualTo(9)
    }

    @Test fun cells_narrowAndOffsetRanges() {
        assertThat(bandCellsFilled(50, 0..100, 12)).isEqualTo(6)
        assertThat(bandCellsFilled(-300, -600..0, 12)).isEqualTo(6)
    }

    @Test fun cells_clampOutOfRangeLevels() {
        assertThat(bandCellsFilled(-9000, -1500..1500, 12)).isEqualTo(0)
        assertThat(bandCellsFilled(9000, -1500..1500, 12)).isEqualTo(12)
    }

    @Test fun cells_emptyOrZeroWidthRangeIsZero() {
        assertThat(bandCellsFilled(0, 0..0, 12)).isEqualTo(0)
        assertThat(bandCellsFilled(0, 5..-5, 12)).isEqualTo(0)
    }

    @Test fun readout_keepsSignAndTrailingZero() {
        assertThat(readoutText(-3f)).isEqualTo("-3.0")
        assertThat(readoutText(0f)).isEqualTo("0.0")
        assertThat(readoutText(-0.5f)).isEqualTo("-0.5")
        assertThat(readoutText(2.25f)).isEqualTo("2.3")
    }

    @Test fun readout_neverShowsNegativeZero() {
        assertThat(readoutText(-0f)).isEqualTo("0.0")
        assertThat(readoutText(-0.04f)).isEqualTo("0.0")
    }

    @Test fun bandsCode_isZeroPaddedToTwoDigits() {
        assertThat(bandsCode(5)).isEqualTo("05 CH")
        assertThat(bandsCode(3)).isEqualTo("03 CH")
        assertThat(bandsCode(12)).isEqualTo("12 CH")
    }
}
