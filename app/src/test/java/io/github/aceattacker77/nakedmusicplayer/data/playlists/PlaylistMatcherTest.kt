package io.github.aceattacker77.nakedmusicplayer.data.playlists

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.LibraryBuilder
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import org.junit.Test

class PlaylistMatcherTest {
    private fun row(id: Long, title: String, artist: String, path: String, name: String, durationMs: Long = 200_000) =
        AudioRow(id, title, artist, "Album", 1, 1, durationMs, 1, 0, path, name, null)

    private val library = LibraryBuilder.build(
        listOf(
            row(1, "One", "Ann", "Music/Ann/", "01.mp3"),
            row(2, "Two", "Bob", "Music/Bob/", "a b.mp3"),
            row(3, "Three", "Cy", "Music/Cy/", "03.flac", durationMs = 180_000),
            row(4, "Same Name", "Dee", "Music/Dee/", "track.mp3"),
            row(5, "Same Name", "Eve", "Music/Eve/", "track.mp3"),
        ),
        LibraryFilter(),
    )

    private fun entry(path: String, dur: Int? = null, artist: String? = null, title: String? = null) =
        M3uEntry(path, dur, artist, title)

    private fun ids(vararg entries: M3uEntry) = PlaylistMatcher.match(entries.toList(), library).songIds

    @Test fun match_relativePath() {
        assertThat(ids(entry("Music/Ann/01.mp3"))).containsExactly(1L)
    }

    @Test fun match_absoluteAndUriPaths() {
        val result = PlaylistMatcher.match(
            listOf(
                entry("/storage/emulated/0/Music/Ann/01.mp3"),
                entry("file:///sdcard/Music/Bob/a%20b.mp3"),
                entry("/storage/1234-ABCD/Music/Cy/03.flac"),
                entry("./Music/Ann/01.mp3"),
            ),
            library,
        )
        assertThat(result.songIds).containsExactly(1L, 2L, 3L, 1L).inOrder()
        assertThat(result.matched).isEqualTo(4)
    }

    @Test fun match_windowsPathWithBackslashesAndDrive() {
        assertThat(ids(entry("C:\\Users\\me\\Music\\Ann\\01.mp3"))).containsExactly(1L)
    }

    @Test fun match_isCaseInsensitive() {
        assertThat(ids(entry("music/ann/01.MP3"))).containsExactly(1L)
    }

    @Test fun match_plusSignInUriIsNotASpace() {
        val lib = LibraryBuilder.build(listOf(row(9, "T", "A", "Music/", "a+b.mp3")), LibraryFilter())
        val result = PlaylistMatcher.match(listOf(entry("file:///sdcard/Music/a+b.mp3")), lib)
        assertThat(result.songIds).containsExactly(9L)
    }

    @Test fun match_fallbackTitleArtistDuration() {
        // The file lives somewhere else now; title, artist and length still identify it.
        assertThat(ids(entry("Elsewhere/renamed.mp3", dur = 180, artist = "cy", title = "THREE"))).containsExactly(3L)
    }

    @Test fun match_durationOutside2s_noMatch() {
        assertThat(ids(entry("Elsewhere/x.mp3", dur = 183, artist = "Cy", title = "Three"))).isEmpty()
        assertThat(ids(entry("Elsewhere/x.mp3", dur = 182, artist = "Cy", title = "Three"))).containsExactly(3L)
        assertThat(ids(entry("Elsewhere/x.mp3", dur = 178, artist = "Cy", title = "Three"))).containsExactly(3L)
    }

    @Test fun match_fallbackWithoutKnownDuration_stillMatchesOnTitleAndArtist() {
        assertThat(ids(entry("Elsewhere/x.mp3", artist = "Cy", title = "Three"))).containsExactly(3L)
    }

    @Test fun match_fallbackNeedsBothTitleAndArtist() {
        assertThat(ids(entry("Elsewhere/x.mp3", dur = 180, title = "Three"))).isEmpty()
    }

    @Test fun match_sameFileNameInDifferentFolders_matchesTheRightOne() {
        assertThat(ids(entry("Music/Eve/track.mp3"))).containsExactly(5L)
        assertThat(ids(entry("Music/Dee/track.mp3"))).containsExactly(4L)
    }

    @Test fun match_bareFileName_matchesFirstWithThatName() {
        assertThat(ids(entry("track.mp3"))).containsExactly(4L)
    }

    @Test fun match_reportsCounts_preservesOrder_skipsUnmatched() {
        val result = PlaylistMatcher.match(
            listOf(entry("Music/Cy/03.flac"), entry("Nope/missing.mp3"), entry("Music/Ann/01.mp3")),
            library,
        )
        assertThat(result.songIds).containsExactly(3L, 1L).inOrder()
        assertThat(result.matched).isEqualTo(2)
        assertThat(result.total).isEqualTo(3)
    }

    @Test fun match_emptyInput() {
        val result = PlaylistMatcher.match(emptyList(), library)
        assertThat(result).isEqualTo(MatchResult(emptyList(), 0, 0))
    }
}
