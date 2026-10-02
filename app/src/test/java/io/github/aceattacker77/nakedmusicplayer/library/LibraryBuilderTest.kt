package io.github.aceattacker77.nakedmusicplayer.library

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ALBUM
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ARTIST
import org.junit.Test

class LibraryBuilderTest {
    private fun row(
        id: Long,
        title: String? = "T$id",
        artist: String? = "A",
        album: String? = "Al",
        albumId: Long = 1,
        dur: Long = 200_000,
        track: Int = 1,
        path: String = "Music/",
        name: String = "f$id.mp3",
    ) = AudioRow(id, title, artist, album, albumId, 1, dur, track, 0, path, name, null)

    @Test fun build_filtersShortSongs() {
        val lib = LibraryBuilder.build(listOf(row(1, dur = 29_999), row(2, dur = 30_000)), LibraryFilter())
        assertThat(lib.songs.map { it.id }).containsExactly(2L)
    }

    @Test fun build_filtersExcludedFolders() {
        val lib = LibraryBuilder.build(
            listOf(row(1, path = "Music/WhatsApp/"), row(2)),
            LibraryFilter(excludedFolders = setOf("Music/WhatsApp/")),
        )
        assertThat(lib.songs.map { it.id }).containsExactly(2L)
    }

    @Test fun build_decodesDiscAndTrack_andOrdersAlbumTracks() {
        val lib = LibraryBuilder.build(
            listOf(row(1, track = 2003), row(2, track = 1005), row(3, track = 2001)),
            LibraryFilter(),
        )
        val album = lib.albums.single()
        assertThat(album.songs.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
        assertThat(album.songs[1].discNumber to album.songs[1].trackNumber).isEqualTo(2 to 1)
    }

    @Test fun build_unknownTags_groupedUnderUnknown() {
        val lib = LibraryBuilder.build(
            listOf(
                row(1, title = null, artist = "<unknown>", album = null, name = "My Song.mp3"),
                row(2, artist = " ", album = ""),
            ),
            LibraryFilter(),
        )
        assertThat(lib.songs.first { it.id == 1L }.title).isEqualTo("My Song")
        assertThat(lib.artists.map { it.name }).containsExactly(UNKNOWN_ARTIST)
        assertThat(lib.songs.map { it.album }.distinct()).containsExactly(UNKNOWN_ALBUM)
    }

    @Test fun build_trackZero_meansDisc1Track0_sortedLast() {
        val lib = LibraryBuilder.build(listOf(row(1, track = 0), row(2, track = 1), row(3, track = 2)), LibraryFilter())
        val album = lib.albums.single()
        assertThat(album.songs.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
        assertThat(album.songs.last().discNumber to album.songs.last().trackNumber).isEqualTo(1 to 0)
    }
}
