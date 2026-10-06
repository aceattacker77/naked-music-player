package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Whether the active skin asks for uppercase labels; false outside [AppTheme] so previews and tests need no skin. */
val LocalLabelCaps = staticCompositionLocalOf { false }

/** [text] uppercased when [caps], independent of the device locale (Kotlin's `uppercase()` is invariant). */
fun labelText(text: String, caps: Boolean): String = if (caps) text.uppercase() else text

/** The text of an app-drawn label, uppercased when the skin asks for it. */
@Composable
fun skinLabel(text: String): String = labelText(text, LocalLabelCaps.current)
