package io.github.aceattacker77.nakedmusicplayer

import android.app.Application

class MusicApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
