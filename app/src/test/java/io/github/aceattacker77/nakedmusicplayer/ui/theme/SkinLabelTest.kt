package io.github.aceattacker77.nakedmusicplayer.ui.theme

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class SkinLabelTest {
    @Test fun caps_uppercasesLatin() {
        assertThat(labelText("Songs", caps = true)).isEqualTo("SONGS")
    }

    @Test fun off_leavesTextAlone() {
        assertThat(labelText("Songs", caps = false)).isEqualTo("Songs")
    }

    @Test fun caps_leavesJapaneseAlone() {
        assertThat(labelText("流星City", caps = true)).isEqualTo("流星CITY")
        assertThat(labelText("プレイリスト", caps = true)).isEqualTo("プレイリスト")
    }

    @Test fun caps_isLocaleIndependent() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertThat(labelText("title", caps = true)).isEqualTo("TITLE")
        } finally {
            Locale.setDefault(saved)
        }
    }
}
