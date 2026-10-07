package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel

/** A row of [cells] bordered cells with the first [filled] in [fill]; the cell width follows the available width. */
@Composable
internal fun SegmentedTrack(filled: Int, cells: Int, fill: Color, modifier: Modifier = Modifier) {
    val border = MaterialTheme.colorScheme.outline
    Canvas(modifier.fillMaxWidth().height(14.dp)) {
        if (cells <= 0) return@Canvas
        val gap = 3.dp.toPx()
        val stroke = 1.dp.toPx()
        val cellWidth = (size.width - gap * (cells - 1)) / cells
        if (cellWidth <= stroke) return@Canvas
        val outline = Stroke(stroke)
        repeat(cells) { i ->
            val left = i * (cellWidth + gap)
            if (i < filled) drawRect(fill, Offset(left, 0f), Size(cellWidth, size.height))
            drawRect(border, Offset(left + stroke / 2, stroke / 2), Size(cellWidth - stroke, size.height - stroke), style = outline)
        }
    }
}

/** A caps label, a large label-font value, a unit and a 1 dp rule in [rule]: the preamp's display. */
@Composable
internal fun Readout(label: String, value: String, unit: String, modifier: Modifier = Modifier, rule: Color = MaterialTheme.colorScheme.secondary) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(skinLabel(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = MaterialTheme.typography.labelMedium.fontFamily),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(unit, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(rule))
    }
}

/** A narrow square-edged thumb for the segmented sliders; on the rotated band sliders it reads as a fader cap. */
@Composable
internal fun SquareThumb() {
    Box(Modifier.size(width = 6.dp, height = 22.dp).background(MaterialTheme.colorScheme.onSurface))
}
