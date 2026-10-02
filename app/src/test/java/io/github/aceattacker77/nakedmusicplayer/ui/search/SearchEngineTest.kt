package io.github.aceattacker77.nakedmusicplayer.ui.search

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.LibraryBuilder
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import org.junit.Test

class SearchEngineTest {
    private fun row(id: Long, title: String, artist: String = "Artist", album: String = "Album", albumId: Long = 1, artistId: Long = 1) =
        AudioRow(id, title, artist, album, albumId, artistId, 200_000, 1, 0, "Music/", "f$id.mp3", null)

    private fun library(vararg rows: AudioRow) = LibraryBuilder.build(rows.toList(), LibraryFilter())

    @Test fun blank_returnsEmpty() {
        val lib = library(row(1, "Song"))
        assertThat(SearchEngine.search(lib, "")).isEqualTo(SearchResults(emptyList(), emptyList(), emptyList()))
        assertThat(SearchEngine.search(lib, "   ").songs).isEmpty()
    }

    @Test fun matchesTitleArtistAlbum() {
        val lib = library(
            row(1, "Moonlight", artist = "Ann", album = "Night", albumId = 1, artistId = 1),
            row(2, "Sunrise", artist = "Moonman", album = "Day", albumId = 2, artistId = 2),
            row(3, "Other", artist = "Bob", album = "Moon Landing", albumId = 3, artistId = 3),
            row(4, "Unrelated", artist = "Cy", album = "Zed", albumId = 4, artistId = 4),
        )
        val result = SearchEngine.search(lib, "moon")
        assertThat(result.songs.map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(result.albums.map { it.title }).containsExactly("Moon Landing")
        assertThat(result.artists.map { it.name }).containsExactly("Moonman")
    }

    @Test fun ignoresCaseAndDiacritics() {
        val lib = library(row(1, "Halo", artist = "Beyoncé"))
        assertThat(SearchEngine.search(lib, "beyonce").songs.map { it.id }).containsExactly(1L)
        assertThat(SearchEngine.search(lib, "BEYONCÉ").artists.map { it.name }).containsExactly("Beyoncé")
    }

    @Test fun queryDiacriticsMatchPlainText() {
        val lib = library(row(1, "Cafe del Mar"))
        assertThat(SearchEngine.search(lib, "café").songs.map { it.id }).containsExactly(1L)
    }

    @Test fun trimsQuery() {
        val lib = library(row(1, "Song"))
        assertThat(SearchEngine.search(lib, "  song ").songs.map { it.id }).containsExactly(1L)
    }

    @Test fun capsSongsAt100() {
        val lib = library(*(1L..150L).map { row(it, "Track $it") }.toTypedArray())
        assertThat(SearchEngine.search(lib, "track").songs).hasSize(100)
    }

    @Test fun noMatch_returnsEmptyLists() {
        val lib = library(row(1, "Song"))
        assertThat(SearchEngine.search(lib, "zzz")).isEqualTo(SearchResults(emptyList(), emptyList(), emptyList()))
    }

    @Test fun searchingTwiceWithDifferentLibraries_usesTheCurrentOne() {
        val first = library(row(1, "Alpha"))
        val second = library(row(2, "Alpha"))
        assertThat(SearchEngine.search(first, "alpha").songs.map { it.id }).containsExactly(1L)
        assertThat(SearchEngine.search(second, "alpha").songs.map { it.id }).containsExactly(2L)
    }
}
