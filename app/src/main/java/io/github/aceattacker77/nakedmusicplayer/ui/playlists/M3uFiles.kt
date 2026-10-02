package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/** M3U files are plain text lists; anything bigger than this is not a playlist. */
private const val MAX_M3U_BYTES = 5 * 1024 * 1024

/** The file's text, or null if it cannot be read or is implausibly large. */
suspend fun readM3uText(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (out.size() + n > MAX_M3U_BYTES) return@withContext null
                out.write(buffer, 0, n)
            }
            out.toString(Charsets.UTF_8.name())
        }
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }
}

/** Writes [text] as UTF-8 (no BOM) to [uri]; returns false on failure. */
suspend fun writeM3uText(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }
}

/** The user-visible file name behind [uri], or its last path segment. */
suspend fun displayNameOf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
}
