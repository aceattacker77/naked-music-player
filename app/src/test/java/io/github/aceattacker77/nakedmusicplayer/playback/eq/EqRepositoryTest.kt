package io.github.aceattacker77.nakedmusicplayer.playback.eq

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class EqRepositoryTest {
    @Test fun defaults_whenNothingSaved() = runTest {
        assertThat(EqRepository(InMemoryPreferencesStore()).state.first()).isEqualTo(EqState())
    }

    @Test fun roundTrip() = runTest {
        val repo = EqRepository(InMemoryPreferencesStore())
        val state = EqState(
            enabled = true,
            preset = PresetRef.Custom("Mine"),
            bandLevelsMb = listOf(100, -200, 0, 300, -400),
            bassBoost = 450,
            preampDb = -2.5f,
            customPresets = mapOf("Mine" to listOf(100, -200, 0, 300, -400), "Flat" to listOf(0, 0, 0, 0, 0)),
        )
        repo.update { state }
        assertThat(repo.state.first()).isEqualTo(state)
    }

    @Test fun roundTrip_devicePreset() = runTest {
        val repo = EqRepository(InMemoryPreferencesStore())
        repo.update { it.copy(preset = PresetRef.Device(3)) }
        assertThat(repo.state.first().preset).isEqualTo(PresetRef.Device(3))
    }

    @Test fun update_transformsTheCurrentState() = runTest {
        val repo = EqRepository(InMemoryPreferencesStore())
        repo.update { it.copy(enabled = true) }
        repo.update { it.copy(bassBoost = 10) }
        val state = repo.state.first()
        assertThat(state.enabled).isTrue()
        assertThat(state.bassBoost).isEqualTo(10)
    }

    @Test fun corruptJson_returnsDefault() = runTest {
        val store = InMemoryPreferencesStore()
        store.edit { it[stringPreferencesKey(EqRepository.KEY)] = "{ definitely not json" }
        assertThat(EqRepository(store).state.first()).isEqualTo(EqState())
    }

    @Test fun jsonOfTheWrongShape_returnsDefault() = runTest {
        val store = InMemoryPreferencesStore()
        store.edit { it[stringPreferencesKey(EqRepository.KEY)] = """{"enabled": "yes", "preset": 7}""" }
        assertThat(EqRepository(store).state.first()).isEqualTo(EqState())
    }

    @Test fun unknownFields_areIgnored() = runTest {
        val store = InMemoryPreferencesStore()
        store.edit { it[stringPreferencesKey(EqRepository.KEY)] = """{"enabled": true, "someFutureField": 1}""" }
        assertThat(EqRepository(store).state.first().enabled).isTrue()
    }
}
