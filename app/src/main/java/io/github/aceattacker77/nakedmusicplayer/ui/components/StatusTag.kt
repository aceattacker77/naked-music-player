package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalKanaFont
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinText
import io.github.aceattacker77.nakedmusicplayer.ui.theme.withKanaFont

/** What a [StatusTag] reports; the word always says it too, so colour is never the only carrier. */
enum class StatusKind { GOOD, BAD, PENDING, INFO }

/** Playing, enabled and active are `tertiary`; unplayable is `error`; pending is `primary`; neutral facts are `secondary`. */
fun StatusKind.color(scheme: ColorScheme): Color = when (this) {
    StatusKind.GOOD -> scheme.tertiary
    StatusKind.BAD -> scheme.error
    StatusKind.PENDING -> scheme.primary
    StatusKind.INFO -> scheme.secondary
}

/** A title is struck through only when the song cannot play and the skin uses status tags. */
fun unplayableDecoration(unplayable: Boolean, statusTags: Boolean): TextDecoration? =
    if (unplayable && statusTags) TextDecoration.LineThrough else null

/**
 * A lamp (the only round element), the status word in tracked caps and an optional kana accent after a divider.
 * The whole tag is one accessibility node named [accessibleName]; the kana is decoration.
 */
@Composable
fun StatusTag(
    kind: StatusKind,
    word: String,
    kana: String?,
    modifier: Modifier = Modifier,
    accessibleName: String = word,
) {
    val colour = kind.color(MaterialTheme.colorScheme)
    Row(
        modifier = modifier.testTag("status-tag").clearAndSetSemantics { contentDescription = accessibleName },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(colour, CircleShape))
        Text(skinLabel(word), style = MaterialTheme.typography.labelSmall, color = colour, maxLines = 1)
        if (kana != null) {
            Box(Modifier.width(1.dp).height(10.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Text(withKanaFont(kana, LocalKanaFont.current), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

/** The "can't play" marker: the red icon, or with `statusTags` an Unplayable tag that keeps the same accessible name. */
@Composable
fun UnplayableMarker(modifier: Modifier = Modifier) {
    val name = stringResource(R.string.cant_play)
    if (LocalOrnament.current.statusTags) {
        val text = skinText("unplayable_tag", stringResource(R.string.unplayable_tag))
        StatusTag(StatusKind.BAD, text.english, text.kana, modifier, accessibleName = name)
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_error_outline),
            contentDescription = name,
            tint = MaterialTheme.colorScheme.error,
            modifier = modifier.size(20.dp),
        )
    }
}
