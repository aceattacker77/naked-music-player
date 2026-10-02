package io.github.aceattacker77.nakedmusicplayer.library.model

const val UNKNOWN_ARTIST = "Unknown Artist"
const val UNKNOWN_ALBUM = "Unknown Album"

/** Raw MediaStore row. [track] is the raw TRACK column (disc * 1000 + track). */
data class AudioRow(
    val id: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val track: Int,
    val dateAddedSec: Long,
    val relativePath: String,
    val displayName: String,
    val year: Int?,
)

data class Song(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val artistId: Long,
    val durationMs: Long,
    val discNumber: Int,
    val trackNumber: Int,
    val dateAddedSec: Long,
    val relativePath: String,
    val displayName: String,
    val year: Int?,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int?,
    val songs: List<Song>,
)

data class Artist(
    val id: Long,
    val name: String,
    val albums: List<Album>,
    val songs: List<Song>,
)

data class Library(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    companion object {
        val EMPTY = Library(emptyList(), emptyList(), emptyList())
    }
}

/** Folders are compared as [AudioRow.relativePath] prefixes. */
data class LibraryFilter(
    val minDurationMs: Long = 30_000,
    val excludedFolders: Set<String> = emptySet(),
)
