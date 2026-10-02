package io.github.aceattacker77.nakedmusicplayer.data.playlists

import io.github.aceattacker77.nakedmusicplayer.library.model.Song

data class M3uEntry(
    val path: String,
    val durationSec: Int?,
    val artist: String?,
    val title: String?,
)

/** Reading and writing extended M3U (`.m3u` / `.m3u8`) playlists. */
object M3u {
    private const val HEADER = "#EXTM3U"
    private const val EXTINF = "#EXTINF:"
    private const val ARTIST_TITLE_SEPARATOR = " - "

    /** UTF-8 text, `\n` line endings, no BOM; paths are `relativePath + displayName`. */
    fun write(songs: List<Song>): String = buildString {
        append(HEADER).append('\n')
        songs.forEach { song ->
            append(EXTINF).append(song.durationMs / 1000).append(',')
                .append(song.artist).append(ARTIST_TITLE_SEPARATOR).append(song.title).append('\n')
            append(song.relativePath).append(song.displayName).append('\n')
        }
    }

    /** Tolerates BOMs, CRLF / CR line endings, comments and playlists without `#EXTINF` lines. */
    fun parse(text: String): List<M3uEntry> {
        val entries = ArrayList<M3uEntry>()
        var pending: ExtInf? = null
        for (raw in text.removePrefix("﻿").split("\r\n", "\n", "\r")) {
            val line = raw.trim()
            when {
                line.isEmpty() -> Unit
                line.startsWith(EXTINF, ignoreCase = true) -> pending = parseExtInf(line.substring(EXTINF.length))
                line.startsWith('#') -> Unit
                else -> {
                    entries += M3uEntry(line, pending?.durationSec, pending?.artist, pending?.title)
                    pending = null
                }
            }
        }
        return entries
    }

    private class ExtInf(val durationSec: Int?, val artist: String?, val title: String?)

    private fun parseExtInf(rest: String): ExtInf {
        val comma = rest.indexOf(',')
        val durationText = (if (comma >= 0) rest.substring(0, comma) else rest).trim().substringBefore(' ')
        val duration = durationText.toIntOrNull()?.takeIf { it >= 0 }
        val info = if (comma >= 0) rest.substring(comma + 1).trim() else ""
        if (info.isEmpty()) return ExtInf(duration, null, null)
        val split = info.indexOf(ARTIST_TITLE_SEPARATOR)
        return if (split > 0) {
            ExtInf(duration, info.substring(0, split).trim(), info.substring(split + ARTIST_TITLE_SEPARATOR.length).trim())
        } else {
            ExtInf(duration, null, info)
        }
    }
}
