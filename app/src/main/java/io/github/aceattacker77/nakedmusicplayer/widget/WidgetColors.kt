package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
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
