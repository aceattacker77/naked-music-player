package io.github.aceattacker77.nakedmusicplayer.library

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryRepositoryTest {
    private fun row(id: Long, dur: Long = 200_000) =
        AudioRow(id, "T$id", "A", "Al", 1, 1, dur, 1, 0, "Music/", "f$id.mp3", null)

    private fun TestScope.repo(
        source: AudioRowSource,
        filter: Flow<LibraryFilter> = MutableStateFlow(LibraryFilter()),
    ) = LibraryRepository(source, filter, backgroundScope, StandardTestDispatcher(testScheduler))

    @Test fun initialLoad_emitsBuiltLibrary() = runTest {
        val source = FakeAudioRowSource().apply { rows = listOf(row(1), row(2)) }
        val repo = repo(source)
        runCurrent()
        assertThat(repo.library.value.songs.map { it.id }).containsExactly(1L, 2L)
        assertThat(repo.permissionDenied.value).isFalse()
    }

    @Test fun isLoaded_falseUntilFirstQueryCompletes() = runTest {
        val source = FakeAudioRowSource()
        val repo = repo(source)
        assertThat(repo.isLoaded.value).isFalse()
        runCurrent()
        assertThat(repo.isLoaded.value).isTrue()
    }

    @Test fun isLoaded_trueEvenWhenPermissionDenied() = runTest {
        val source = FakeAudioRowSource().apply { throwOnQuery = SecurityException("no") }
        val repo = repo(source)
        runCurrent()
        assertThat(repo.isLoaded.value).isTrue()
    }

    @Test fun changes_areDebounced500ms() = runTest {
        val source = FakeAudioRowSource().apply { rows = listOf(row(1)) }
        repo(source)
        runCurrent()
        assertThat(source.queryCount).isEqualTo(1)

        source.changeFlow.emit(Unit)
        advanceTimeBy(50)
        source.changeFlow.emit(Unit)
        advanceTimeBy(50)
        source.changeFlow.emit(Unit)
        advanceTimeBy(499)
        runCurrent()
        assertThat(source.queryCount).isEqualTo(1)
        advanceTimeBy(2)
        runCurrent()
        assertThat(source.queryCount).isEqualTo(2)
    }

    @Test fun filterChange_rebuildsWithoutRequery() = runTest {
        val source = FakeAudioRowSource().apply { rows = listOf(row(1, dur = 20_000), row(2)) }
        val filter = MutableStateFlow(LibraryFilter(minDurationMs = 0))
        val repo = repo(source, filter)
        runCurrent()
        assertThat(repo.library.value.songs).hasSize(2)

        filter.value = LibraryFilter(minDurationMs = 30_000)
        runCurrent()
        assertThat(repo.library.value.songs.map { it.id }).containsExactly(2L)
        assertThat(source.queryCount).isEqualTo(1)
    }

    @Test fun securityException_emitsEmpty() = runTest {
        val source = FakeAudioRowSource().apply { throwOnQuery = SecurityException("revoked") }
        val repo = repo(source)
        runCurrent()
        assertThat(repo.library.value).isEqualTo(Library.EMPTY)
        assertThat(repo.permissionDenied.value).isTrue()

        source.throwOnQuery = null
        source.rows = listOf(row(1))
        repo.refresh()
        runCurrent()
        assertThat(repo.permissionDenied.value).isFalse()
        assertThat(repo.library.value.songs).hasSize(1)
    }
}
