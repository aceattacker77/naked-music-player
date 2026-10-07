package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KanaFontTest {
    private val mincho = FontFamily.Serif

    @Test fun japaneseCharacters_takeTheHeadingFamily_andLatinKeepsTheSurroundingStyle() {
        val text = withKanaFont("Library // 曲目 ひらカナ", mincho)
        assertThat(text.text).isEqualTo("Library // 曲目 ひらカナ")
        val styled = text.spanStyles.filter { it.item.fontFamily == mincho }
        assertThat(styled.map { text.text.substring(it.start, it.end) }).containsExactly("曲目", "ひらカナ")
    }

    @Test fun noHeadingFamily_leavesTheTextUnstyled() {
        assertThat(withKanaFont("Library // 曲目", null).spanStyles).isEmpty()
    }

    @Test fun textWithoutJapanese_isUnstyled() {
        assertThat(withKanaFont("Playing", mincho).spanStyles).isEmpty()
    }
}
