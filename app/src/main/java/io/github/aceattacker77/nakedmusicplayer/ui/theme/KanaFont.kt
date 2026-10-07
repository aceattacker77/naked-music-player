package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

/** The active skin's heading font, used for its kana and kanji accents; null outside [AppTheme] or when the skin sets none. */
val LocalKanaFont = staticCompositionLocalOf<FontFamily?> { null }

private fun isJapanese(cp: Int) = cp in 0x3040..0x30FF || cp in 0x4E00..0x9FFF

/** [text] with every run of kana and kanji in [family], so the accents use the skin's heading font. */
fun withKanaFont(text: String, family: FontFamily?): AnnotatedString = buildAnnotatedString {
    if (family == null) {
        append(text)
        return@buildAnnotatedString
    }
    var i = 0
    while (i < text.length) {
        val japanese = isJapanese(text.codePointAt(i))
        var j = i
        while (j < text.length && isJapanese(text.codePointAt(j)) == japanese) j += Character.charCount(text.codePointAt(j))
        if (japanese) withStyle(SpanStyle(fontFamily = family)) { append(text.substring(i, j)) } else append(text.substring(i, j))
        i = j
    }
}
