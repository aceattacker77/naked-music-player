package io.github.aceattacker77.nakedmusicplayer.ui.player

import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import java.util.Locale
import androidx.compose.ui.geometry.Size
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.components.formatDuration
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import kotlin.math.PI
import kotlin.math.sin

/**
 * Seek bar in the skin's style. Tap or drag to seek; [onSeek] fires once on release. The wavy
 * style animates only while [isPlaying], so a paused player does no per-frame work.
 */
@Composable
fun SeekBar(
    style: SeekBarStyle,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    segments: Int = 40,
    seekColor: SeekColor = SeekColor.PRIMARY,
    onDragFraction: (Float?) -> Unit = {},
) {
    val total = durationMs.coerceAtLeast(1L)
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction ?: (positionMs.toFloat() / total).coerceIn(0f, 1f)
    LaunchedEffect(dragFraction) { onDragFraction(dragFraction) }

    val phase = remember { Animatable(0f) }
    LaunchedEffect(style, isPlaying) {
        if (style == SeekBarStyle.WAVY && isPlaying) {
            while (true) {
                phase.animateTo(phase.value + TWO_PI, tween(WAVE_PERIOD_MS, easing = LinearEasing))
                phase.snapTo(phase.value % TWO_PI)
            }
        }
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (seekColor == SeekColor.TERTIARY) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    val cellBorder = MaterialTheme.colorScheme.outline
    val seekLabel = stringResource(R.string.seek)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(
                when (style) {
                    SeekBarStyle.WAVY -> 32.dp
                    SeekBarStyle.FLAT -> 24.dp
                    SeekBarStyle.THIN -> 16.dp
                    SeekBarStyle.SEGMENTED -> 32.dp
                },
            )
            .semantics {
                contentDescription = seekLabel
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
                setProgress { target ->
                    onSeek((target.coerceIn(0f, 1f) * total).toLong())
                    true
                }
            }
            .pointerInput(total) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    dragFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    drag(down.id) { change ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        change.consume()
                    }
                    dragFraction?.let { onSeek((it * total).toLong()) }
                    dragFraction = null
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (style) {
                SeekBarStyle.WAVY -> drawWavy(fraction, phase.value, trackColor, activeColor)
                SeekBarStyle.FLAT -> drawStraight(fraction, 4.dp.toPx(), 7.dp.toPx(), trackColor, activeColor)
                SeekBarStyle.THIN -> drawStraight(fraction, 2.dp.toPx(), 4.dp.toPx(), trackColor, activeColor)
                SeekBarStyle.SEGMENTED -> drawSegmented(fraction, segments, cellBorder, activeColor)
            }
        }
    }
}

/** Elapsed / total time labels shown under a seek bar. */
@Composable
fun SeekTimes(positionMs: Long, durationMs: Long, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(skinLabel(formatDuration(positionMs)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(skinLabel(formatDuration(durationMs)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The standard seek bar slot: bar plus times. */
@Composable
fun SeekBarWithTimes(
    style: SeekBarStyle,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    segments: Int = 40,
    seekColor: SeekColor = SeekColor.PRIMARY,
    showHeader: Boolean = false,
) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column(modifier.fillMaxWidth()) {
        if (showHeader && style == SeekBarStyle.SEGMENTED) {
            val total = durationMs.coerceAtLeast(1L)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(skinLabel(stringResource(R.string.position_label)), Modifier.originalLabel(stringResource(R.string.position_label)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(positionPercentText(dragging ?: (positionMs.toFloat() / total)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SeekBar(style, positionMs, durationMs, onSeek, isPlaying = isPlaying, segments = segments, seekColor = seekColor, onDragFraction = { dragging = it })
        SeekTimes(positionMs, durationMs)
    }
}

/** The position as a percentage with one decimal, trailing zero kept: `31.3 %`. */
internal fun positionPercentText(fraction: Float): String {
    val clamped = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
    // 100.0 % is reserved for the very end, so a nearly-full bar never reads as complete.
    if (clamped < 1f && clamped * 100f >= 99.95f) return "99.9 %"
    return String.format(Locale.ROOT, "%.1f %%", clamped * 100f)
}

private const val TWO_PI = (2 * PI).toFloat()
private const val WAVE_PERIOD_MS = 1_800

private fun DrawScope.drawStraight(
    fraction: Float,
    strokePx: Float,
    thumbRadiusPx: Float,
    trackColor: androidx.compose.ui.graphics.Color,
    activeColor: androidx.compose.ui.graphics.Color,
) {
    val inset = thumbRadiusPx
    val startX = inset
    val endX = size.width - inset
    val x = startX + (endX - startX) * fraction
    val y = size.height / 2
    drawLine(trackColor, Offset(startX, y), Offset(endX, y), strokePx, StrokeCap.Round)
    drawLine(activeColor, Offset(startX, y), Offset(x, y), strokePx, StrokeCap.Round)
    drawCircle(activeColor, thumbRadiusPx, Offset(x, y))
}

private fun DrawScope.drawSegmented(
    fraction: Float,
    segments: Int,
    borderColor: androidx.compose.ui.graphics.Color,
    activeColor: androidx.compose.ui.graphics.Color,
) {
    val gap = 3.dp.toPx()
    val stroke = 1.dp.toPx()
    val barHeight = 16.dp.toPx()
    val count = segmentCountFor(segments, size.width, gap, minCellPx = 4.dp.toPx())
    if (count == 0) return
    val cellWidth = (size.width - gap * (count - 1)) / count
    val top = (size.height - barHeight) / 2
    val filled = segmentsFilled(fraction, count)
    val border = Stroke(stroke)
    repeat(count) { i ->
        val left = i * (cellWidth + gap)
        if (i < filled) drawRect(activeColor, Offset(left, top), Size(cellWidth, barHeight))
        drawRect(
            borderColor,
            Offset(left + stroke / 2, top + stroke / 2),
            Size(cellWidth - stroke, barHeight - stroke),
            style = border,
        )
    }
}

private fun DrawScope.drawWavy(
    fraction: Float,
    phase: Float,
    trackColor: androidx.compose.ui.graphics.Color,
    activeColor: androidx.compose.ui.graphics.Color,
) {
    val strokePx = 4.dp.toPx()
    val amplitude = 4.dp.toPx()
    val wavelength = 28.dp.toPx()
    val thumbRadius = 7.dp.toPx()
    val startX = thumbRadius
    val endX = size.width - thumbRadius
    val x = startX + (endX - startX) * fraction
    val centerY = size.height / 2

    drawLine(trackColor, Offset(x, centerY), Offset(endX, centerY), strokePx, StrokeCap.Round)

    val path = Path()
    var px = startX
    path.moveTo(px, centerY + amplitude * sin(phase))
    while (px < x) {
        px = minOf(px + 2f, x)
        path.lineTo(px, centerY + amplitude * sin(((px - startX) / wavelength) * TWO_PI + phase))
    }
    drawPath(path, activeColor, style = Stroke(strokePx, cap = StrokeCap.Round))
    drawCircle(activeColor, thumbRadius, Offset(x, centerY))
}
