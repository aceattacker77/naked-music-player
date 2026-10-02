package io.github.aceattacker77.nakedmusicplayer.library

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Asks MediaStore to index audio files in folders the user picked, for files it missed on its own
 * (some `.opus` files, folders it was never told about). Works from a Storage Access Framework
 * tree URI, so no storage permission is needed beyond the picker grant.
 */
class FolderScanner(private val context: Context) {
    /** Number of audio files handed to the media scanner; 0 when the folder has no usable path. */
    suspend fun scan(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (DocumentPaths.toFilePath(DocumentsContract.getTreeDocumentId(treeUri)) == null) {
            return@withContext 0
        }
        val paths = ArrayList<String>()
        collectAudioPaths(treeUri, DocumentsContract.getTreeDocumentId(treeUri), paths)
        if (paths.isEmpty()) return@withContext 0
        awaitScan(paths)
        paths.size
    }

    /** Gives back the persisted read grant when a folder is removed. */
    fun release(treeUri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun collectAudioPaths(treeUri: Uri, documentId: String, out: MutableList<String>) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val subfolders = ArrayList<String>()
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val name = cursor.getString(1).orEmpty()
                val mime = cursor.getString(2)
                when {
                    mime == DocumentsContract.Document.MIME_TYPE_DIR -> subfolders += id
                    isAudioFile(name) -> DocumentPaths.toFilePath(id)?.let(out::add)
                }
            }
        }
        subfolders.forEach { collectAudioPaths(treeUri, it, out) }
    }

    private fun isAudioFile(name: String) = name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS

    private suspend fun awaitScan(paths: List<String>) = suspendCancellableCoroutine { continuation ->
        val remaining = AtomicInteger(paths.size)
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            if (remaining.decrementAndGet() == 0 && continuation.isActive) continuation.resume(Unit)
        }
    }

    companion object {
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "amr", "mka", "webm", "mp4")
    }
}
