package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.ZeroCornerSize
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
 * because Material's `Shapes` only accepts those. Only the top-end and bottom-start corners carry a cut; the
 * other two stay square. `copy` honours the sizes a component passes, so a bottom sheet that zeroes its bottom
 * corners loses the bottom-left cut as well.
 */
class ChamferShape(
    val cut: Dp,
    val topEndCut: CornerSize = CornerSize(cut),
    val bottomStartCut: CornerSize = CornerSize(cut),
) : CornerBasedShape(ZeroCornerSize, topEndCut, ZeroCornerSize, bottomStartCut) {
    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Outline {
        val points = chamferPoints(size, topRightCut = topEnd, bottomLeftCut = bottomStart)
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
        return Outline.Generic(path)
    }

    override fun copy(topStart: CornerSize, topEnd: CornerSize, bottomEnd: CornerSize, bottomStart: CornerSize): CornerBasedShape =
        ChamferShape(cut, topEnd, bottomStart)

    override fun equals(other: Any?) =
        other is ChamferShape && other.cut == cut && other.topEndCut == topEndCut && other.bottomStartCut == bottomStartCut

    override fun hashCode() = 31 * (31 * cut.hashCode() + topEndCut.hashCode()) + bottomStartCut.hashCode()

    override fun toString() = "ChamferShape(cut=$cut)"
}

/** The outline polygon, clockwise from the top-left; each cut never exceeds half the shorter side. */
internal fun chamferPoints(size: Size, cut: Float): List<Offset> = chamferPoints(size, cut, cut)

internal fun chamferPoints(size: Size, topRightCut: Float, bottomLeftCut: Float): List<Offset> {
    val limit = minOf(size.width, size.height) / 2f
    val tr = topRightCut.coerceIn(0f, limit)
    val bl = bottomLeftCut.coerceIn(0f, limit)
    val (w, h) = size
    return listOf(Offset(0f, 0f), Offset(w - tr, 0f), Offset(w, tr), Offset(w, h), Offset(bl, h), Offset(0f, h - bl))
        .fold(emptyList()) { acc, point -> if (acc.lastOrNull() == point) acc else acc + point }
}

private val CUT_FACTORS = listOf(0.3f, 0.5f, 0.8f, 1f, 1.4f)

/** The five Material shape sizes (extraSmall to extraLarge) as cuts scaled from [chamferDp]. */
internal fun chamferCutsDp(chamferDp: Int): List<Dp> = CUT_FACTORS.map { (chamferDp * it).dp }

/** [themed] when the active theme is a chamfer one, otherwise the call site's own [fallback] shape. */
fun shapeOr(fallback: Shape, themed: Shape): Shape = if (themed is ChamferShape) themed else fallback
