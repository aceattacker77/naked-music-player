package io.github.aceattacker77.nakedmusicplayer.ui.player.layouts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingSlots
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition

/** The artwork sits on a record: grooves around it and a spindle through the middle. */
@Composable
fun VinylLayout(slots: NowPlayingSlots, artPosition: ArtPosition, modifier: Modifier = Modifier) {
    val art: @Composable () -> Unit = { SquareArea(Modifier.fillMaxSize()) { VinylRecord(slots) } }
    if (artPosition == ArtPosition.LEFT) {
        SideBySide(slots, modifier, art)
    } else {
        Stacked(slots, modifier, art)
    }
}

@Composable
private fun VinylRecord(slots: NowPlayingSlots) {
    val disc = MaterialTheme.colorScheme.scrim
    val groove = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val sheen = MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
    val spindle = MaterialTheme.colorScheme.surface

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            drawCircle(disc, radius)
            // Concentric grooves between the label and the rim.
            var r = radius * 0.97f
            while (r > radius * 0.68f) {
                drawCircle(groove, r, style = Stroke(width = 1f))
                r -= radius * 0.035f
            }
            // Light catching the vinyl.
            drawArc(sheen, startAngle = -60f, sweepAngle = 40f, useCenter = true, size = size)
            drawArc(sheen, startAngle = 120f, sweepAngle = 40f, useCenter = true, size = size)
        }
        Box(Modifier.fillMaxSize(LABEL_FRACTION)) { slots.artwork() }
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(spindle, radius = size.minDimension * 0.02f, center = Offset(size.width / 2, size.height / 2))
        }
    }
}

private const val LABEL_FRACTION = 0.62f
