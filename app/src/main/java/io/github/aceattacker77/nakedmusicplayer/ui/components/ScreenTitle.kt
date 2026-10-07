package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel

/** `Library // 曲目`, or just the English word when the skin gives no kana. */
internal fun kickerText(kicker: SkinString): String =
    if (kicker.kana == null) kicker.english else "${kicker.english} // ${kicker.kana}"

/**
 * A screen's title card: a small tracked-caps kicker in `primary`, a large heading in the squeezed heading style that
 * shrinks toward 24 sp rather than clipping at large font scales, and a 1 dp rule. Accessibility hears only the English.
 */
@Composable
fun ScreenTitle(kicker: SkinString, title: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = skinLabel(kickerText(kicker)),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics { text = AnnotatedString(kicker.english) },
        )
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 44.sp),
        )
        Box(
            Modifier.fillMaxWidth().padding(top = 8.dp).height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
                .testTag("screen-title-rule"),
        )
    }
}
