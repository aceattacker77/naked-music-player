package io.github.aceattacker77.nakedmusicplayer.ui.player.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingSlots
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition

/** The artwork sits in the label window of a cassette drawn on a canvas. */
@Composable
fun CassetteLayout(slots: NowPlayingSlots, artPosition: ArtPosition, modifier: Modifier = Modifier) {
    val art: @Composable () -> Unit = { Cassette(slots, Modifier.fillMaxWidth()) }
    if (artPosition == ArtPosition.LEFT) {
        SideBySide(slots, modifier, art)
    } else {
        Stacked(slots, modifier, art)
    }
}

private const val ASPECT = 1.6f

@Composable
private fun Cassette(slots: NowPlayingSlots, modifier: Modifier = Modifier) {
    val body = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val label = MaterialTheme.colorScheme.surface
    val window = MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f)
    val reel = MaterialTheme.colorScheme.onSurfaceVariant

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val width = minOf(maxWidth, maxHeight * ASPECT)
        val height = width / ASPECT
        Box(Modifier.width(width).height(height)) {
            Canvas(Modifier.fillMaxSize()) {
                val corner = CornerRadius(size.height * 0.08f)
                drawRoundRect(body, cornerRadius = corner)
                drawRoundRect(outline, cornerRadius = corner, style = Stroke(width = 3f))

                // Label strip across the top.
                val labelTop = size.height * 0.07f
                val labelHeight = size.height * 0.48f
                drawRoundRect(
                    label,
                    topLeft = Offset(size.width * 0.07f, labelTop),
                    size = Size(size.width * 0.86f, labelHeight),
                    cornerRadius = CornerRadius(size.height * 0.04f),
                )

                // Tape window with the two reels.
                val windowTop = size.height * 0.60f
                val windowHeight = size.height * 0.28f
                drawRoundRect(
                    window,
                    topLeft = Offset(size.width * 0.20f, windowTop),
                    size = Size(size.width * 0.60f, windowHeight),
                    cornerRadius = CornerRadius(windowHeight / 2),
                )
                val reelRadius = windowHeight * 0.34f
                val reelY = windowTop + windowHeight / 2
                listOf(0.32f, 0.68f).forEach { x ->
                    drawCircle(reel, reelRadius, Offset(size.width * x, reelY), style = Stroke(width = 3f))
                    repeat(6) { i ->
                        val angle = Math.toRadians(i * 60.0)
                        val dx = (kotlin.math.cos(angle) * reelRadius).toFloat()
                        val dy = (kotlin.math.sin(angle) * reelRadius).toFloat()
                        drawLine(reel, Offset(size.width * x, reelY), Offset(size.width * x + dx, reelY + dy), strokeWidth = 2f)
                    }
                }

                // Corner screws.
                listOf(0.04f to 0.06f, 0.96f to 0.06f, 0.04f to 0.94f, 0.96f to 0.94f).forEach { (x, y) ->
                    drawCircle(outline, size.height * 0.022f, Offset(size.width * x, size.height * y))
                }
            }
            // The artwork fills the label strip's right-hand square.
            val labelHeight = height * 0.48f
            Box(
                Modifier
                    .offset(x = width * 0.07f + (width * 0.86f - labelHeight) / 2, y = height * 0.07f)
                    .width(labelHeight)
                    .height(labelHeight),
            ) { slots.artwork() }
        }
    }
}
