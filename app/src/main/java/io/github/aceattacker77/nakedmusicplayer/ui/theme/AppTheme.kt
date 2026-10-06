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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinAssets
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinFonts
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
    val fonts = remember(
        skin.id, skin.baseDir, skin.fontPath, skin.headingFontPath, skin.bodyFontPath, skin.labelFontPath,
    ) { SkinAssets.fonts(skin) }
    val typography = remember(fonts) { Typography().withFontFamilies(fonts) }
    val shapes = remember(skin.cornerRadiusDp) { shapesFor(skin.cornerRadiusDp) }

    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
    }
}

/** Loads a font file from a skin directory, or null when it is missing or cannot be read. */
internal fun loadSkinFontFamily(dir: File, path: String): FontFamily? {
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

/** Display, headline and title text use the heading font; body and label text use their own. */
internal fun Typography.withFontFamilies(fonts: SkinFonts): Typography {
    fun TextStyle.with(family: FontFamily?) = if (family == null) this else copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.with(fonts.heading),
        displayMedium = displayMedium.with(fonts.heading),
        displaySmall = displaySmall.with(fonts.heading),
        headlineLarge = headlineLarge.with(fonts.heading),
        headlineMedium = headlineMedium.with(fonts.heading),
        headlineSmall = headlineSmall.with(fonts.heading),
        titleLarge = titleLarge.with(fonts.heading),
        titleMedium = titleMedium.with(fonts.heading),
        titleSmall = titleSmall.with(fonts.heading),
        bodyLarge = bodyLarge.with(fonts.body),
        bodyMedium = bodyMedium.with(fonts.body),
        bodySmall = bodySmall.with(fonts.body),
        labelLarge = labelLarge.with(fonts.label),
        labelMedium = labelMedium.with(fonts.label),
        labelSmall = labelSmall.with(fonts.label),
    )
}
