package io.github.aceattacker77.nakedmusicplayer.data.playlists

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import org.junit.Test

class M3uTest {
    private fun song(id: Long, title: String, artist: String, path: String, name: String, durationMs: Long = 200_500) = Song(
        id = id, uri = "content://media/external/audio/media/$id", title = title, artist = artist, album = "Al",
        albumId = 1, artistId = 1, durationMs = durationMs, discNumber = 1, trackNumber = 1, dateAddedSec = 0,
        relativePath = path, displayName = name, year = null,
    )

    @Test fun write_formatsExtinfAndPaths() {
        val text = M3u.write(
            listOf(
                song(1, "Intro", "Ann", "Music/Ann/Album/", "01.mp3", durationMs = 61_900),
                song(2, "Song Two", "Bob", "Music/Bob/", "two.flac"),
            ),
        )
        assertThat(text).isEqualTo(
            "#EXTM3U\n" +
                "#EXTINF:61,Ann - Intro\n" +
                "Music/Ann/Album/01.mp3\n" +
                "#EXTINF:200,Bob - Song Two\n" +
                "Music/Bob/two.flac\n",
        )
        assertThat(text).doesNotContain("\r")
        assertThat(text.startsWith("\uFEFF")).isFalse()
    }

    @Test fun write_emptyList_isJustTheHeader() {
        assertThat(M3u.write(emptyList())).isEqualTo("#EXTM3U\n")
    }

    @Test fun parse_roundTripsWrite() {
        val songs = listOf(
            song(1, "Intro", "Ann", "Music/Ann/Album/", "01.mp3", durationMs = 61_900),
            song(2, "Dash - In Title", "Bob", "Music/Bob/", "two.flac"),
        )
        val entries = M3u.parse(M3u.write(songs))
        assertThat(entries).containsExactly(
            M3uEntry("Music/Ann/Album/01.mp3", 61, "Ann", "Intro"),
            M3uEntry("Music/Bob/two.flac", 200, "Bob", "Dash - In Title"),
        ).inOrder()
    }

    @Test fun parse_windowsStyle() {
        val text = "\uFEFF#EXTM3U\r\n#EXTINF:215,Some Artist - Some Title\r\nC:\\Music\\A\\01.mp3\r\n\r\nD:\\More\\b.mp3\r\n"
        assertThat(M3u.parse(text)).containsExactly(
            M3uEntry("C:\\Music\\A\\01.mp3", 215, "Some Artist", "Some Title"),
            M3uEntry("D:\\More\\b.mp3", null, null, null),
        ).inOrder()
    }

    @Test fun parse_ignoresCommentsAndBlankLines() {
        val text = "#EXTM3U\n# a comment\n\n#EXTGRP:rock\nMusic/a.mp3\n   \n#PLAYLIST:name\nMusic/b.mp3\n"
        assertThat(M3u.parse(text).map { it.path }).containsExactly("Music/a.mp3", "Music/b.mp3").inOrder()
    }

    @Test fun parse_plainListWithoutExtinf() {
        val entries = M3u.parse("Music/a.mp3\nMusic/b.mp3")
        assertThat(entries).containsExactly(
            M3uEntry("Music/a.mp3", null, null, null),
            M3uEntry("Music/b.mp3", null, null, null),
        ).inOrder()
    }

    @Test fun parse_extinfWithoutArtistSeparator_isTitleOnly() {
        val entries = M3u.parse("#EXTINF:100,Just A Title\nMusic/a.mp3\n")
        assertThat(entries.single()).isEqualTo(M3uEntry("Music/a.mp3", 100, null, "Just A Title"))
    }

    @Test fun parse_unknownDuration_isNull() {
        val entries = M3u.parse("#EXTINF:-1,A - B\nMusic/a.mp3\n")
        assertThat(entries.single().durationSec).isNull()
    }

    @Test fun parse_extinfDoesNotLeakToTheNextEntry() {
        val entries = M3u.parse("#EXTINF:100,A - B\nMusic/a.mp3\nMusic/b.mp3\n")
        assertThat(entries[1]).isEqualTo(M3uEntry("Music/b.mp3", null, null, null))
    }

    @Test fun parse_oldMacLineEndings() {
        assertThat(M3u.parse("Music/a.mp3\rMusic/b.mp3\r").map { it.path }).containsExactly("Music/a.mp3", "Music/b.mp3").inOrder()
    }

    @Test fun parse_emptyText_isEmpty() {
        assertThat(M3u.parse("")).isEmpty()
        assertThat(M3u.parse("#EXTM3U\n")).isEmpty()
    }
}
