package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import androidx.glance.unit.ColorProvider
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin

/** Dynamic (wallpaper) colours only where they exist (Android 12+), are enabled, and the skin allows them. */
fun widgetUsesDynamicColors(colorMode: ColorMode, dynamicColorEnabled: Boolean, sdkInt: Int): Boolean =
    colorMode == ColorMode.SYSTEM && dynamicColorEnabled && sdkInt >= 31

/** The skin's light and dark schemes as widget colours; the widget follows the system day/night setting. */
fun widgetColorProviders(skin: Skin): ColorProviders = ColorProviders(
    light = skin.light ?: Skin.FALLBACK.light!!,
    dark = skin.dark ?: Skin.FALLBACK.dark!!,
)

/**
 * The widget's background. For a skin's own colours it is the skin's `surface`: Glance's `widgetBackground` role is a fixed system colour
 * that ignores the skin, so a skin's widget would show a tint from none of its schemes. With dynamic (wallpaper) colours Glance's own role
 * is the wallpaper-tinted container the widget has always used, so it stays.
 */
fun widgetBackground(colors: ColorProviders, dynamic: Boolean = false): ColorProvider =
    if (dynamic) colors.widgetBackground else colors.surface
