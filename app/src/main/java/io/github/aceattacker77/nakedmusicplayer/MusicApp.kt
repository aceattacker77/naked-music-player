package io.github.aceattacker77.nakedmusicplayer

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArtFetcher
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArtKeyer
import okio.Path.Companion.toPath

class MusicApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /** Artwork loader with capped memory and disk caches, so scrolling a big library stays within budget. */
    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_PERCENT).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(context.cacheDir.resolve("album_art").absolutePath.toPath())
                .maxSizeBytes(DISK_CACHE_BYTES)
                .build()
        }
        .components {
            add(AlbumArtKeyer())
            add(AlbumArtFetcher.Factory(context))
        }
        .crossfade(true)
        .build()

    private companion object {
        const val MEMORY_CACHE_PERCENT = 0.08
        const val DISK_CACHE_BYTES = 50L * 1024 * 1024
    }
}
