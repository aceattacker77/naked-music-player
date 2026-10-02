package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin

val LocalSkin = staticCompositionLocalOf<Skin> { error("no skin provided; wrap content in AppTheme") }
