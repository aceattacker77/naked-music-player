package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString

/** The active skin's strings pack; empty outside [AppTheme], so every slot falls back to plain English. */
val LocalSkinStrings = staticCompositionLocalOf<Map<String, SkinString>> { emptyMap() }

/** The skin's entry for [key], or [fallback] (plain English, no kana) when the skin does not set it. */
internal fun resolveSkinString(strings: Map<String, SkinString>, key: String, fallback: String): SkinString =
    strings[key] ?: SkinString(fallback, null)

/** Text for a skin-overridable slot: the skin's `English|Kana` entry for [key], or [fallback]. */
@Composable
fun skinText(key: String, fallback: String): SkinString = resolveSkinString(LocalSkinStrings.current, key, fallback)

/** Whether the glow draws: never when off, always when on, and only in a dark scheme for `dark`. */
fun glowActive(mode: GlowMode, isDark: Boolean): Boolean = when (mode) {
    GlowMode.OFF -> false
    GlowMode.ALWAYS -> true
    GlowMode.DARK -> isDark
}
