package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import io.github.aceattacker77.nakedmusicplayer.ui.theme.shapeOr

/**
 * The shape for the app's own buttons. Material's buttons use a fully rounded default that the theme's shape set does not
 * reach, so a chamfer skin passes its small chamfer explicitly; every other skin keeps Material's default.
 */
@Composable
fun themedButtonShape(): Shape = shapeOr(ButtonDefaults.shape, MaterialTheme.shapes.small)
