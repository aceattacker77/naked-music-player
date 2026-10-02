package io.github.aceattacker77.nakedmusicplayer.library

import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ALBUM
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ARTIST

object LibraryBuilder {
    private const val MEDIA_STORE_UNKNOWN = "<unknown>"

    fun build(rows: List<AudioRow>, filter: LibraryFilter): Library {
        val songs = rows
            .filter { it.durationMs >= filter.minDurationMs }
            .filter { row -> filter.excludedFolders.none { row.relativePath.startsWith(it) } }
            .map(::toSong)

        val albumOrder = compareBy<Song>({ it.discNumber }, { it.trackNumber == 0 }, { it.trackNumber }, { it.title.lowercase() })

        val albums = songs.groupBy { it.albumId to it.album }.map { (key, group) ->
            val sorted = group.sortedWith(albumOrder)
            Album(
                id = key.first,
                title = key.second,
                artist = sorted.first().artist,
                year = sorted.firstNotNullOfOrNull { it.year },
                songs = sorted,
            )
        }.sortedBy { it.title.lowercase() }

        val artists = songs.groupBy { it.artistId to it.artist }.map { (key, group) ->
            val artistAlbums = albums.filter { album -> album.songs.any { it.artistId == key.first && it.artist == key.second } }
            Artist(id = key.first, name = key.second, albums = artistAlbums, songs = group)
        }.groupBy { it.name }.map { (_, sameName) ->
            // Rows without a usable artist all collapse into the single "Unknown Artist" entry.
            sameName.singleOrNull() ?: Artist(
                id = sameName.first().id,
                name = sameName.first().name,
                albums = sameName.flatMap { it.albums }.distinctBy { it.id to it.title },
                songs = sameName.flatMap { it.songs },
            )
        }.sortedBy { it.name.lowercase() }

        return Library(songs = songs, albums = albums, artists = artists)
    }

    private fun toSong(row: AudioRow): Song {
        val disc = maxOf(1, row.track / 1000)
        return Song(
            id = row.id,
            uri = "content://media/external/audio/media/${row.id}",
            title = row.title.cleaned() ?: row.displayName.substringBeforeLast('.'),
            artist = row.artist.cleaned() ?: UNKNOWN_ARTIST,
            album = row.album.cleaned() ?: UNKNOWN_ALBUM,
            albumId = row.albumId,
            artistId = row.artistId,
            durationMs = row.durationMs,
            discNumber = disc,
            trackNumber = row.track % 1000,
            dateAddedSec = row.dateAddedSec,
            relativePath = row.relativePath,
            displayName = row.displayName,
            year = row.year,
        )
    }

    private fun String?.cleaned(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && !it.equals(MEDIA_STORE_UNKNOWN, ignoreCase = true) }
}
