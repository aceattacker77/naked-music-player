package io.github.aceattacker77.nakedmusicplayer.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {
    @Test fun duration_underOneHour_isMinutesAndSeconds() {
        assertThat(formatDuration(0)).isEqualTo("0:00")
        assertThat(formatDuration(5_000)).isEqualTo("0:05")
        assertThat(formatDuration(65_000)).isEqualTo("1:05")
        assertThat(formatDuration(599_999)).isEqualTo("9:59")
    }

    @Test fun duration_overOneHour_includesHours() {
        assertThat(formatDuration(3_600_000)).isEqualTo("1:00:00")
        assertThat(formatDuration(3_725_000)).isEqualTo("1:02:05")
    }

    @Test fun duration_negativeOrUnknown_clampsToZero() {
        assertThat(formatDuration(-1)).isEqualTo("0:00")
    }

    @Test fun indexLetter_usesFirstLetterUppercased() {
        assertThat(indexLetter("beta")).isEqualTo('B')
        assertThat(indexLetter("Alpha")).isEqualTo('A')
    }

    @Test fun indexLetter_stripsDiacritics() {
        assertThat(indexLetter("Étoile")).isEqualTo('E')
    }

    @Test fun indexLetter_nonLetters_groupUnderHash() {
        assertThat(indexLetter("1999")).isEqualTo('#')
        assertThat(indexLetter("")).isEqualTo('#')
        assertThat(indexLetter("  ")).isEqualTo('#')
        assertThat(indexLetter("!Hey")).isEqualTo('#')
    }
}
