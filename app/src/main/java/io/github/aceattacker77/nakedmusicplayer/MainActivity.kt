package io.github.aceattacker77.nakedmusicplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import io.github.aceattacker77.nakedmusicplayer.ui.AppRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MusicApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) { AppRoot() }
        }
    }
}
