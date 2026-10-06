package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.R
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Decorative text drawn on the placeholder: [top] top-left, [bottom] bottom-left. */
data class ArtCaptions(val top: String, val bottom: String?)

/** Six vertices of a pointy-top hexagon centred in [size], with radius `min(w, h) / 2 - inset` (never negative). */
internal fun hexagonPoints(size: Size, inset: Float): List<Offset> {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = (min(size.width, size.height) / 2f - inset).coerceAtLeast(0f)
    return listOf(-90.0, -30.0, 30.0, 90.0, 150.0, 210.0).map { degrees ->
        val angle = Math.toRadians(degrees)
        Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }
}

/** Captions need room: they are skipped on small tiles (the compact layouts) and on circular artwork, whose clip hides the corners. */
internal fun shouldShowCaptions(minSideDp: Float, circular: Boolean): Boolean = !circular && minSideDp >= 160f

/** `TRK 0002` for the second queue entry; null when nothing is playing. */
internal fun trackCode(currentIndex: Int): String? =
    if (currentIndex < 0) null else String.format(Locale.ROOT, "TRK %04d", currentIndex + 1)

/**
 * Three nested hexagons, the note glyph and optional [captions], drawn on a canvas so it scales from
 * list tiles to the full-screen artwork. Purely decorative: hidden from accessibility.
 */
@Composable
fun HexagonPlaceholder(modifier: Modifier = Modifier, captions: ArtCaptions? = null) {
    val outerColor = MaterialTheme.colorScheme.outlineVariant
    val middleColor = MaterialTheme.colorScheme.outline
    val innerColor = MaterialTheme.colorScheme.primary
    BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
        val showCaptions = captions != null && shouldShowCaptions(minOf(maxWidth, maxHeight).value, circular = false)
        Canvas(Modifier.fillMaxSize()) {
            val half = min(size.width, size.height) / 2f
            fun outline(scale: Float, color: Color, widthDp: Float) {
                val points = hexagonPoints(size, half * (1f - scale))
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(path, color, style = Stroke(widthDp.dp.toPx()))
            }
            outline(0.92f, outerColor, 1f)
            outline(0.68f, middleColor, 1f)
            outline(0.44f, innerColor, 2f)
        }
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.24f),
            tint = innerColor,
        )
        if (captions != null && showCaptions) {
            Text(
                text = captions.top,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            )
            captions.bottom?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
        }
    }
}
