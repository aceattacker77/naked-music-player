package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A framed panel: 1 dp `outline` border, `surface` fill and corner brackets in [tone]. */
@Composable
fun GeoPanel(
    modifier: Modifier = Modifier,
    tone: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.cornerBrackets(tone),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 0.dp,
        content = content,
    )
}
