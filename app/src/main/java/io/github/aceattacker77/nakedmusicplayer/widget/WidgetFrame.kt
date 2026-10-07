package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How far in from the frame the corner brackets sit. On Android 12 and later Glance clips a rounded box to its outline, and a corner of
 * radius r is cut away up to about 0.29 r along each edge, so brackets at the very corner would vanish; 0.3 r clears the curve.
 */
internal fun bracketInset(cornerRadius: Dp): Dp = (cornerRadius.value * 0.3f).coerceAtLeast(0f).dp
