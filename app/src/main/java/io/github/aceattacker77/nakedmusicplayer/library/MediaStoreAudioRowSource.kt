package io.github.aceattacker77.nakedmusicplayer.library

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class MediaStoreAudioRowSource(context: Context) : AudioRowSource {
    private val resolver = context.applicationContext.contentResolver
    private val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private val projection: Array<String> = buildList {
        add(MediaStore.Audio.Media._ID)
        add(MediaStore.Audio.Media.TITLE)
        add(MediaStore.Audio.Media.ARTIST)
        add(MediaStore.Audio.Media.ALBUM)
        add(MediaStore.Audio.Media.ALBUM_ID)
        add(MediaStore.Audio.Media.ARTIST_ID)
        add(MediaStore.Audio.Media.DURATION)
        add(MediaStore.Audio.Media.TRACK)
        add(MediaStore.Audio.Media.DATE_ADDED)
        add(MediaStore.Audio.Media.DISPLAY_NAME)
        add(MediaStore.Audio.Media.YEAR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION") add(MediaStore.Audio.Media.DATA)
        }
    }.toTypedArray()

    override suspend fun query(): List<AudioRow> = withContext(Dispatchers.IO) {
        val cursor = resolver.query(
            collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null,
        ) ?: return@withContext emptyList()
        cursor.use { readRows(it) }
    }

    private fun readRows(c: Cursor): List<AudioRow> {
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val pathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION") c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        }

        val out = ArrayList<AudioRow>(c.count)
        while (c.moveToNext()) {
            val displayName = c.getString(nameCol).orEmpty()
            val rawPath = c.getString(pathCol).orEmpty()
            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                rawPath
            } else {
                derivePath(rawPath, displayName)
            }
            out += AudioRow(
                id = c.getLong(idCol),
                title = c.getString(titleCol),
                artist = c.getString(artistCol),
                album = c.getString(albumCol),
                albumId = c.getLong(albumIdCol),
                artistId = c.getLong(artistIdCol),
                durationMs = c.getLong(durationCol),
                track = c.getInt(trackCol),
                dateAddedSec = c.getLong(addedCol),
                relativePath = relativePath,
                displayName = displayName,
                year = c.getInt(yearCol).takeIf { it > 0 },
            )
        }
        return out
    }

    /** Pre-API-29: strip the storage root and the file name from the absolute DATA path. */
    private fun derivePath(data: String, displayName: String): String =
        data.removePrefix("/storage/emulated/0/").removeSuffix(displayName)

    override fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(collection, true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }
}
