package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EqFormatTest {
    @Test fun kiloHertz_dropsTrailingZero() {
        assertThat(kiloHertzText(14_000)).isEqualTo("14")
        assertThat(kiloHertzText(3_600)).isEqualTo("3.6")
        assertThat(kiloHertzText(1_000)).isEqualTo("1")
    }

    @Test fun decibels_alwaysCarryASign() {
        assertThat(decibelText(300)).isEqualTo("+3")
        assertThat(decibelText(-150)).isEqualTo("-1.5")
        assertThat(decibelText(0)).isEqualTo("+0")
    }
}
