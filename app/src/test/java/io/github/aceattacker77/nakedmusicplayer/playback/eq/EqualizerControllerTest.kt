package io.github.aceattacker77.nakedmusicplayer.playback.eq

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EqualizerControllerTest {
    private class Fixture(
        val backend: FakeAudioEffectsBackend,
        val repo: EqRepository,
        val controller: EqualizerController,
        val volumes: MutableList<Float>,
    ) {
        suspend fun saved() = repo.state.first()
    }

    private fun TestScope.fixture(
        backend: FakeAudioEffectsBackend = FakeAudioEffectsBackend(),
        initial: EqState? = null,
    ): Fixture {
        val repo = EqRepository(InMemoryPreferencesStore())
        val volumes = mutableListOf<Float>()
        val scope = TestScope(UnconfinedTestDispatcher(testScheduler)).backgroundScope
        if (initial != null) kotlinx.coroutines.runBlocking { repo.update { initial } }
        return Fixture(backend, repo, EqualizerController(backend, repo, scope) { volumes += it }, volumes)
    }

    @Test fun unsupported_capabilitiesNull() = runTest {
        val f = fixture(FakeAudioEffectsBackend(capabilities = null))
        f.controller.onAudioSessionId(7)
        assertThat(f.controller.capabilities.value).isNull()
    }

    @Test fun supported_capabilitiesPublished() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(7)
        assertThat(f.controller.capabilities.value).isEqualTo(FakeAudioEffectsBackend.DEFAULT_CAPS)
    }

    @Test fun invalidSessionId_isIgnored() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(0)
        f.controller.onAudioSessionId(-1)
        assertThat(f.backend.attachedSessions).isEmpty()
    }

    @Test fun persistedState_appliedOnFirstAttach() = runTest {
        val saved = EqState(enabled = true, bandLevelsMb = listOf(100, -100, 0, 200, -200), bassBoost = 300, preampDb = -3f)
        val f = fixture(initial = saved)
        f.controller.onAudioSessionId(7)
        assertThat(f.backend.appliedEnabled).isTrue()
        assertThat(f.backend.appliedBands).containsExactly(100, -100, 0, 200, -200).inOrder()
        assertThat(f.backend.appliedBass).isEqualTo(300)
        assertThat(f.volumes.last()).isWithin(0.001f).of(preampToVolume(-3f))
    }

    @Test fun sessionChange_reattachesAndReappliesState() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setEnabled(true)
        f.controller.setBand(2, 400)
        f.controller.setBassBoost(500)

        f.controller.onAudioSessionId(2)
        assertThat(f.backend.attachedSessions).containsExactly(1, 2).inOrder()
        assertThat(f.backend.appliedEnabled).isTrue()
        assertThat(f.backend.appliedBands).containsExactly(0, 0, 400, 0, 0).inOrder()
        assertThat(f.backend.appliedBass).isEqualTo(500)
    }

    @Test fun sameSessionAgain_doesNotReattach() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.onAudioSessionId(1)
        assertThat(f.backend.attachedSessions).containsExactly(1)
    }

    @Test fun bandLevels_clampedToRange() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setBand(0, 99_999)
        f.controller.setBand(1, -99_999)
        assertThat(f.backend.appliedBands!!.take(2)).containsExactly(1500, -1500).inOrder()
        assertThat(f.saved().bandLevelsMb.take(2)).containsExactly(1500, -1500).inOrder()
    }

    @Test fun outOfRangeBandIndex_isIgnored() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setBand(9, 100)
        f.controller.setBand(-1, 100)
        assertThat(f.saved().bandLevelsMb).containsExactly(0, 0, 0, 0, 0)
    }

    @Test fun persistedBandsOfWrongLength_arePaddedOrTrimmed() = runTest {
        val f = fixture(initial = EqState(bandLevelsMb = listOf(100, 200)))
        f.controller.onAudioSessionId(1)
        assertThat(f.backend.appliedBands).containsExactly(100, 200, 0, 0, 0).inOrder()
    }

    @Test fun devicePreset_copiesLevelsAndMarksPreset() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.selectDevicePreset(1)
        assertThat(f.backend.appliedBands).containsExactlyElementsIn(FakeAudioEffectsBackend.PRESETS.getValue(1)).inOrder()
        assertThat(f.saved().preset).isEqualTo(PresetRef.Device(1))
        assertThat(f.saved().bandLevelsMb).containsExactlyElementsIn(FakeAudioEffectsBackend.PRESETS.getValue(1)).inOrder()
    }

    @Test fun editingBand_switchesPresetToNull() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.selectDevicePreset(2)
        f.controller.setBand(0, 50)
        assertThat(f.saved().preset).isNull()
    }

    @Test fun saveCustomPreset_storedAndSelected() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setBand(0, 300)
        f.controller.setBand(4, -300)
        f.controller.saveCustomPreset("Mine")
        val saved = f.saved()
        assertThat(saved.customPresets).containsExactly("Mine", listOf(300, 0, 0, 0, -300))
        assertThat(saved.preset).isEqualTo(PresetRef.Custom("Mine"))
    }

    @Test fun saveCustomPreset_blankName_isIgnored() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.saveCustomPreset("   ")
        assertThat(f.saved().customPresets).isEmpty()
    }

    @Test fun selectCustomPreset_appliesSavedLevels() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setBand(1, 250)
        f.controller.saveCustomPreset("Mine")
        f.controller.selectDevicePreset(0)
        f.controller.selectCustomPreset("Mine")
        assertThat(f.backend.appliedBands).containsExactly(0, 250, 0, 0, 0).inOrder()
        assertThat(f.saved().preset).isEqualTo(PresetRef.Custom("Mine"))
    }

    @Test fun selectCustomPreset_unknownName_isIgnored() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.selectCustomPreset("ghost")
        assertThat(f.saved().preset).isNull()
    }

    @Test fun disable_keepsLevels() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setEnabled(true)
        f.controller.setBand(0, 400)
        f.controller.setEnabled(false)
        assertThat(f.backend.appliedEnabled).isFalse()
        assertThat(f.saved().enabled).isFalse()
        assertThat(f.saved().bandLevelsMb.first()).isEqualTo(400)
    }

    @Test fun preamp_onlyAppliesWhileEnabled() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setPreamp(-6f)
        assertThat(f.volumes.last()).isEqualTo(1f) // EQ off: no attenuation
        f.controller.setEnabled(true)
        assertThat(f.volumes.last()).isWithin(0.001f).of(0.501f)
        f.controller.setEnabled(false)
        assertThat(f.volumes.last()).isEqualTo(1f)
    }

    @Test fun preamp_isClampedToMinusSixToZero() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setPreamp(-20f)
        assertThat(f.saved().preampDb).isEqualTo(-6f)
        f.controller.setPreamp(4f)
        assertThat(f.saved().preampDb).isEqualTo(0f)
    }

    @Test fun bassBoost_clampedAndIgnoredWhenUnsupported() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.setBassBoost(5_000)
        assertThat(f.backend.appliedBass).isEqualTo(1000)

        val noBass = fixture(FakeAudioEffectsBackend(FakeAudioEffectsBackend.DEFAULT_CAPS.copy(bassBoostSupported = false)))
        noBass.controller.onAudioSessionId(1)
        noBass.controller.setBassBoost(500)
        assertThat(noBass.backend.appliedBass).isNull()
    }

    @Test fun enableBeforeAttach_isPersistedAndAppliedLater() = runTest {
        // Band edits need the device's band count, so only band-independent settings can precede attach.
        val f = fixture()
        f.controller.setEnabled(true)
        f.controller.setPreamp(-3f)
        assertThat(f.backend.attachedSessions).isEmpty()
        assertThat(f.volumes.last()).isWithin(0.001f).of(preampToVolume(-3f))
        f.controller.onAudioSessionId(1)
        assertThat(f.backend.appliedEnabled).isTrue()
        assertThat(f.saved().enabled).isTrue()
    }

    @Test fun bandEditsBeforeAttach_areIgnored() = runTest {
        val f = fixture()
        f.controller.setBand(0, 100)
        f.controller.onAudioSessionId(1)
        assertThat(f.backend.appliedBands).containsExactly(0, 0, 0, 0, 0)
    }

    @Test fun preampToVolume_values() {
        assertThat(preampToVolume(-6f)).isWithin(0.001f).of(0.501f)
        assertThat(preampToVolume(0f)).isEqualTo(1f)
        assertThat(preampToVolume(3f)).isEqualTo(1f)
        assertThat(preampToVolume(-60f)).isWithin(0.001f).of(0.501f)
    }

    @Test fun release_releasesBackend() = runTest {
        val f = fixture()
        f.controller.onAudioSessionId(1)
        f.controller.release()
        assertThat(f.backend.released).isAtLeast(1)
        assertThat(f.controller.capabilities.value).isNull()
    }
}
