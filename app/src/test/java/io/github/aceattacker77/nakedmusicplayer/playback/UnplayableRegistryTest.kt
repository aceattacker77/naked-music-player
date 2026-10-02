package io.github.aceattacker77.nakedmusicplayer.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnplayableRegistryTest {
    @Test fun mark_addsIdToStateFlow() {
        val id = "song:${System.nanoTime()}"
        assertThat(UnplayableRegistry.ids.value).doesNotContain(id)
        UnplayableRegistry.mark(id)
        assertThat(UnplayableRegistry.ids.value).contains(id)
    }
}
