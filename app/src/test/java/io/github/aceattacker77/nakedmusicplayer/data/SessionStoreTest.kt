package io.github.aceattacker77.nakedmusicplayer.data

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.session.SavedSession
import io.github.aceattacker77.nakedmusicplayer.data.session.SessionStore
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SessionStoreTest {
    private fun newStore() = InMemoryPreferencesStore()

    @Test fun session_roundTrip() = runTest(UnconfinedTestDispatcher()) {
        val store = SessionStore(newStore())
        val session = SavedSession(listOf("song:1", "song:22", "song:3"), index = 1, positionMs = 12_345, shuffle = true, repeatMode = 2)
        store.save(session)
        assertThat(store.load()).isEqualTo(session)
    }

    @Test fun session_loadReturnsNullWhenAbsent() = runTest(UnconfinedTestDispatcher()) {
        assertThat(SessionStore(newStore()).load()).isNull()
    }

    @Test fun session_clearRemovesIt() = runTest(UnconfinedTestDispatcher()) {
        val store = SessionStore(newStore())
        store.save(SavedSession(listOf("song:1"), 0, 0, false, 0))
        store.clear()
        assertThat(store.load()).isNull()
    }
}
