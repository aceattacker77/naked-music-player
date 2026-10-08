package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.compose.material3.ColorScheme
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin

/** Dynamic (wallpaper) colours only where they exist (Android 12+), are enabled, and the skin allows them. */
fun widgetUsesDynamicColors(colorMode: ColorMode, dynamicColorEnabled: Boolean, sdkInt: Int): Boolean =
    colorMode == ColorMode.SYSTEM && dynamicColorEnabled && sdkInt >= 31

/**
 * The skin's light and dark schemes as widget colours; the widget follows the system day/night setting. `surface` is forced
 * opaque here: the widget sits on the wallpaper and a translucent one would be unreadable.
 */
fun widgetColorProviders(skin: Skin): ColorProviders {
    fun ColorScheme.opaqueSurface() = copy(surface = surface.copy(alpha = 1f))
    return ColorProviders(
        light = (skin.light ?: Skin.FALLBACK.light!!).opaqueSurface(),
        dark = (skin.dark ?: Skin.FALLBACK.dark!!).opaqueSurface(),
    )
}

/**
 * The widget's background. For a skin's own colours it is the skin's `surface`: Glance's `widgetBackground` role is a fixed system colour
 * that ignores the skin, so a skin's widget would show a tint from none of its schemes. With dynamic (wallpaper) colours Glance's own role
 * is the wallpaper-tinted container the widget has always used, so it stays. The result must stay one of Glance's own providers: a custom
 * [ColorProvider] subclass is not understood when Glance builds the RemoteViews and renders as an unrelated grey.
 */
fun widgetBackground(colors: ColorProviders, dynamic: Boolean = false): ColorProvider =
    if (dynamic) colors.widgetBackground else colors.surface
