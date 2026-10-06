package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * The four segments of the top-left and bottom-right brackets, pushed [outset] outside the box so the
 * stroke does not overlap the content. The arm length never exceeds half the shorter side.
 */
internal fun bracketLines(size: Size, length: Float, outset: Float): List<Pair<Offset, Offset>> {
    val arm = length.coerceIn(0f, min(size.width, size.height) / 2f)
    val topLeft = Offset(0f - outset, 0f - outset)
    val bottomRight = Offset(size.width + outset, size.height + outset)
    return listOf(
        topLeft to Offset(topLeft.x + arm, topLeft.y),
        topLeft to Offset(topLeft.x, topLeft.y + arm),
        bottomRight to Offset(bottomRight.x - arm, bottomRight.y),
        bottomRight to Offset(bottomRight.x, bottomRight.y - arm),
    )
}

/** Draws corner brackets outside the content bounds, so layout does not shift and a chamfer does not clip them. */
fun Modifier.cornerBrackets(color: Color, length: Dp = 14.dp, stroke: Dp = 2.dp): Modifier = drawWithContent {
    drawContent()
    val strokePx = stroke.toPx()
    bracketLines(size, length.toPx(), strokePx / 2f).forEach { (from, to) ->
        drawLine(color, from, to, strokePx, StrokeCap.Butt)
    }
}
