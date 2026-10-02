package io.github.aceattacker77.nakedmusicplayer.data.playlists

import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import java.net.URLDecoder
import kotlin.math.abs

data class MatchResult(val songIds: List<Long>, val matched: Int, val total: Int)

/** Maps playlist entries from other players onto songs in the library. */
object PlaylistMatcher {
    private const val DURATION_TOLERANCE_SEC = 2
    @Suppress("SdCardPath") // matched as text in foreign playlists; never used to open files
    private val storagePrefixes = listOf("/storage/emulated/0/", "/sdcard/", "./")
    private val volumePrefix = Regex("^/storage/[^/]+/")

    /**
     * Each entry is matched by path first (relative, absolute, `file://`, Windows-style) and then by
     * title + artist (+ duration within 2 s when known). The first hit wins, order is preserved and
     * entries with no hit are skipped.
     */
    fun match(entries: List<M3uEntry>, library: Library): MatchResult {
        val keyed = library.songs.map { it to (it.relativePath + it.displayName).lowercase() }
        val byFileName = keyed.groupBy { (_, key) -> key.substringAfterLast('/') }
        val byTitleArtist = library.songs.groupBy { norm(it.title) to norm(it.artist) }

        val ids = ArrayList<Long>()
        for (entry in entries) {
            val path = normalizePath(entry.path)
            val byPath = byFileName[path.substringAfterLast('/')].orEmpty().firstOrNull { (_, key) ->
                key == path || path.endsWith("/$key") || key.endsWith("/$path")
            }?.first
            val song = byPath ?: matchByTags(entry, byTitleArtist)
            if (song != null) ids += song.id
        }
        return MatchResult(ids, matched = ids.size, total = entries.size)
    }

    private fun matchByTags(entry: M3uEntry, index: Map<Pair<String, String>, List<Song>>): Song? {
        val title = entry.title?.takeIf { it.isNotBlank() } ?: return null
        val artist = entry.artist?.takeIf { it.isNotBlank() } ?: return null
        return index[norm(title) to norm(artist)].orEmpty().firstOrNull { song ->
            entry.durationSec == null || abs(song.durationMs / 1000.0 - entry.durationSec) <= DURATION_TOLERANCE_SEC
        }
    }

    private fun norm(text: String) = text.trim().lowercase()

    private fun normalizePath(raw: String): String {
        var path = raw.trim()
        if (path.startsWith("file:", ignoreCase = true)) {
            path = urlDecode(path.removePrefix(path.substring(0, 5)).removePrefix("//"))
        }
        path = path.replace('\\', '/').lowercase()
        val prefix = storagePrefixes.firstOrNull { path.startsWith(it) }
        path = when {
            prefix != null -> path.removePrefix(prefix)
            volumePrefix.containsMatchIn(path) -> path.replaceFirst(volumePrefix, "")
            else -> path
        }
        return path
    }

    // '+' means a literal plus in a file URI (only form encoding turns it into a space).
    private fun urlDecode(text: String): String = try {
        URLDecoder.decode(text.replace("+", "%2B"), "UTF-8")
    } catch (e: IllegalArgumentException) {
        text
    }
}
