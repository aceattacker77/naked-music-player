package io.github.aceattacker77.nakedmusicplayer.ui.components

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlbumArtCaptionsTest {
    @Test fun captions_showWhenThereIsNoAlbum() {
        assertThat(captionsVisible(albumId = null, loadFailed = false)).isTrue()
    }

    @Test fun captions_hideWhileArtworkLoads() {
        assertThat(captionsVisible(albumId = 7L, loadFailed = false)).isFalse()
    }

    @Test fun captions_showWhenArtworkFailedToLoad() {
        assertThat(captionsVisible(albumId = 7L, loadFailed = true)).isTrue()
    }
}
