package io.github.aceattacker77.nakedmusicplayer

import androidx.compose.runtime.staticCompositionLocalOf

/** The app's dependencies, provided once at the root; tests provide a container wired to fakes. */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> { error("no AppContainer provided") }
