package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * A rectangle with its top-right and bottom-left corners cut at 45 degrees. It is a [CornerBasedShape]
 * because Material's `Shapes` only accepts those; the four corner sizes are always the same cut, and
 * `copy` keeps the shape as it is, so components that zero one corner (menus, sheets) still get the chamfer.
 */
class ChamferShape(val cut: Dp) : CornerBasedShape(CornerSize(cut), CornerSize(cut), CornerSize(cut), CornerSize(cut)) {
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val points = chamferPoints(size, topStart)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize): CornerBasedShape = this

    override fun equals(other: Any?) = other is ChamferShape && other.cut == cut

    override fun hashCode() = cut.hashCode()

    override fun toString() = "ChamferShape(cut=$cut)"
}

/** The outline polygon, clockwise from the top-left; the cut never exceeds half the shorter side. */
internal fun chamferPoints(size: Size, cut: Float): List<Offset> {
    val c = cut.coerceAtMost(minOf(size.width, size.height) / 2f)
    val (w, h) = size
    return listOf(Offset(0f, 0f), Offset(w - c, 0f), Offset(w, c), Offset(w, h), Offset(c, h), Offset(0f, h - c))
}

private val CUT_FACTORS = listOf(0.3f, 0.5f, 0.8f, 1f, 1.4f)

/** The five Material shape sizes (extraSmall to extraLarge) as cuts scaled from [chamferDp]. */
internal fun chamferCutsDp(chamferDp: Int): List<Dp> = CUT_FACTORS.map { (chamferDp * it).dp }

/** [themed] when the active theme is a chamfer one, otherwise the call site's own [fallback] shape. */
fun shapeOr(fallback: Shape, themed: Shape): Shape = if (themed is ChamferShape) themed else fallback
