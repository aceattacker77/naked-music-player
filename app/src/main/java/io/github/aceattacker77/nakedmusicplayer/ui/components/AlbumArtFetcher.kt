package io.github.aceattacker77.nakedmusicplayer.ui.components

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.key.Keyer
import coil3.request.Options
import coil3.size.pxOrElse

/** Coil model for an album's artwork; resolved with `ContentResolver.loadThumbnail` on Android 10+. */
data class AlbumArtModel(val albumId: Long)

class AlbumArtKeyer : Keyer<AlbumArtModel> {
    override fun key(data: AlbumArtModel, options: Options): String = "album-art:${data.albumId}"
}

class AlbumArtFetcher(
    private val context: Context,
    private val data: AlbumArtModel,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        // Only Android 10+ has loadThumbnail; older versions load the legacy album-art URI instead.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, data.albumId)
        val width = options.size.width.pxOrElse { DEFAULT_PX }
        val height = options.size.height.pxOrElse { DEFAULT_PX }
        // Albums without art throw; returning null lets the caller keep its placeholder.
        val bitmap = try {
            context.contentResolver.loadThumbnail(albumUri, Size(width, height), null)
        } catch (e: Exception) {
            return null
        }
        return ImageFetchResult(image = bitmap.asImage(), isSampled = true, dataSource = DataSource.DISK)
    }

    class Factory(private val context: Context) : Fetcher.Factory<AlbumArtModel> {
        override fun create(data: AlbumArtModel, options: Options, imageLoader: ImageLoader): Fetcher =
            AlbumArtFetcher(context, data, options)
    }

    private companion object {
        const val DEFAULT_PX = 256
    }
}
