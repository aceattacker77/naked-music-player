package io.github.aceattacker77.nakedmusicplayer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import java.io.File

/**
 * Pure colour-scheme choice, kept free of composition so it can be unit tested.
 * [dynamic] is null below API 31, where dynamic colour does not exist.
 */
fun selectColorScheme(
    skin: Skin,
    settings: AppSettings,
    systemDark: Boolean,
    dynamic: ((dark: Boolean) -> ColorScheme)?,
): ColorScheme {
    val themeDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dark = when (skin.colorMode) {
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
        ColorMode.BOTH, ColorMode.SYSTEM -> themeDark
    }
    if (skin.colorMode == ColorMode.SYSTEM && settings.dynamicColor && dynamic != null) return dynamic(dark)
    val fallback = Skin.FALLBACK
    return if (dark) skin.dark ?: fallback.dark!! else skin.light ?: fallback.light!!
}

@Composable
fun AppTheme(skin: Skin, settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dynamic: ((Boolean) -> ColorScheme)? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        { dark -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) }
    } else {
        null
    }
    val colors = selectColorScheme(skin, settings, systemDark, dynamic)
    val fontFamily = remember(skin.id, skin.fontPath) { loadSkinFontFamily(skin) }
    val typography = remember(fontFamily) { Typography().withFontFamily(fontFamily) }
    val shapes = remember(skin.cornerRadiusDp) { shapesFor(skin.cornerRadiusDp) }

    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}

/** Loads the skin's font file, or null when it has none or the file cannot be read. */
internal fun loadSkinFontFamily(skin: Skin): FontFamily? {
    val path = skin.fontPath ?: return null
    val dir = skin.baseDir ?: return null
    val file = File(dir, path)
    if (!file.isFile) return null
    return runCatching { FontFamily(Font(file)) }.getOrNull()
}

private fun shapesFor(cornerRadiusDp: Int): Shapes {
    fun shape(factor: Float) = RoundedCornerShape((cornerRadiusDp * factor).dp)
    return Shapes(
        extraSmall = shape(0.25f),
        small = shape(0.5f),
        medium = shape(0.75f),
        large = shape(1f),
        extraLarge = shape(1.5f),
    )
}

private fun Typography.withFontFamily(family: FontFamily?): Typography {
    if (family == null) return this
    return copy(
        displayLarge = displayLarge.copy(fontFamily = family),
        displayMedium = displayMedium.copy(fontFamily = family),
        displaySmall = displaySmall.copy(fontFamily = family),
        headlineLarge = headlineLarge.copy(fontFamily = family),
        headlineMedium = headlineMedium.copy(fontFamily = family),
        headlineSmall = headlineSmall.copy(fontFamily = family),
        titleLarge = titleLarge.copy(fontFamily = family),
        titleMedium = titleMedium.copy(fontFamily = family),
        titleSmall = titleSmall.copy(fontFamily = family),
        bodyLarge = bodyLarge.copy(fontFamily = family),
        bodyMedium = bodyMedium.copy(fontFamily = family),
        bodySmall = bodySmall.copy(fontFamily = family),
        labelLarge = labelLarge.copy(fontFamily = family),
        labelMedium = labelMedium.copy(fontFamily = family),
        labelSmall = labelSmall.copy(fontFamily = family),
    )
}
