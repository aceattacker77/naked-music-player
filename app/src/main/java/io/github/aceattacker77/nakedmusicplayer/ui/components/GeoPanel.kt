package io.github.aceattacker77.nakedmusicplayer.ui.components

import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.glowActive
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel

/**
 * A framed panel: 1 dp `outline` border, `surface` fill and corner brackets in [tone]. With a [title] it draws a header
 * strip (title left, [code] right, a rule in [tone] beneath). A [live] panel glows when the skin's glow setting allows.
 */
@Composable
fun GeoPanel(
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.primary,
    title: String? = null,
    code: String? = null,
    live: Boolean = false,
    content: @Composable () -> Unit,
) {
    val shape = MaterialTheme.shapes.small
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glowing = live && glowActive(LocalOrnament.current.glow, dark)
    Surface(
        modifier = modifier.cornerBrackets(tone).liveGlow(tone, glowing, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
    ) {
        Column {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(skinLabel(title), Modifier.originalLabel(title), style = MaterialTheme.typography.labelMedium, color = tone, maxLines = 1)
                    if (code != null) {
                        Text(code, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(tone).testTag("geo-panel-rule"))
            }
            content()
        }
    }
}

/** A soft glow in [tone] around the element, drawn only when [active] and the device supports coloured shadows (API 28+). */
fun Modifier.liveGlow(tone: Color, active: Boolean, shape: Shape = RectangleShape): Modifier =
    if (active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        shadow(elevation = 10.dp, shape = shape, clip = false, ambientColor = tone, spotColor = tone)
    } else {
        this
    }
