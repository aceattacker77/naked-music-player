package io.github.aceattacker77.nakedmusicplayer.library

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DocumentPathsTest {
    @Test fun primaryVolume_mapsToInternalStorage() {
        assertThat(DocumentPaths.toFilePath("primary:Music/X")).isEqualTo("/storage/emulated/0/Music/X")
        assertThat(DocumentPaths.toFilePath("primary:Music")).isEqualTo("/storage/emulated/0/Music")
    }

    @Test fun primaryRoot() {
        assertThat(DocumentPaths.toFilePath("primary:")).isEqualTo("/storage/emulated/0")
    }

    @Test fun sdCardVolume_mapsToItsMountPoint() {
        assertThat(DocumentPaths.toFilePath("ABCD-1234:Music")).isEqualTo("/storage/ABCD-1234/Music")
        assertThat(DocumentPaths.toFilePath("ABCD-1234:")).isEqualTo("/storage/ABCD-1234")
        assertThat(DocumentPaths.toFilePath("1a2b-3c4d:Podcasts/Daily")).isEqualTo("/storage/1a2b-3c4d/Podcasts/Daily")
    }

    @Test fun rawIds_useThePathAfterTheirPrefix() {
        assertThat(DocumentPaths.toFilePath("raw:/storage/emulated/0/Foo")).isEqualTo("/storage/emulated/0/Foo")
    }

    @Test fun trailingSlash_isDropped() {
        assertThat(DocumentPaths.toFilePath("primary:Music/")).isEqualTo("/storage/emulated/0/Music")
    }

    @Test fun opaqueOrUnknownIds_areNull() {
        assertThat(DocumentPaths.toFilePath("msf:12")).isNull()
        assertThat(DocumentPaths.toFilePath("12345")).isNull()
        assertThat(DocumentPaths.toFilePath("")).isNull()
        assertThat(DocumentPaths.toFilePath("home:Documents")).isNull()
        assertThat(DocumentPaths.toFilePath("raw:relative/path")).isNull()
    }
}
