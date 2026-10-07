package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqCapabilities
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.FakeAudioEffectsBackend
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class EqualizerScreenTest {
    @get:Rule val compose = createComposeRule()

    private val backend = FakeAudioEffectsBackend()
    private val scope = TestScope(UnconfinedTestDispatcher())
    private val volumes = mutableListOf<Float>()

    private fun launch(
        capabilities: EqCapabilities? = FakeAudioEffectsBackend.DEFAULT_CAPS,
        skin: Skin = Skin.FALLBACK,
    ): EqualizerController {
        backend.capabilities = capabilities
        val controller = EqualizerController(backend, EqRepository(InMemoryPreferencesStore()), scope.backgroundScope) { volumes += it }
        controller.onAudioSessionId(1)
        compose.setContent {
            AppTheme(skin, AppSettings(dynamicColor = false)) { EqualizerScreen(controller, onBack = {}) }
        }
        compose.waitForIdle()
        return controller
    }

    @Test fun showsOneLabelledSliderPerBand() {
        launch()
        listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        (0..4).forEach { compose.onNodeWithTag("eq-band-$it").assertExists() }
    }

    @Test fun enableSwitch_enablesTheEffect() {
        launch()
        compose.onNodeWithTag("eq-switch").performClick()
        compose.waitForIdle()
        assertThat(backend.appliedEnabled).isTrue()
    }

    @Test fun bandSlider_changesBandLevel() {
        val controller = launch()
        compose.onNodeWithTag("eq-band-1").performSemanticsAction(SemanticsActions.SetProgress) { it(750f) }
        compose.waitForIdle()
        assertThat(controller.state.value.bandLevelsMb[1]).isEqualTo(750)
        assertThat(backend.appliedBands!![1]).isEqualTo(750)
    }

    @Test fun presetMenu_appliesDevicePreset() {
        val controller = launch()
        compose.onNodeWithTag("eq-preset-button").performClick()
        compose.onNodeWithText("Rock").performClick()
        compose.waitForIdle()
        assertThat(backend.appliedBands).containsExactlyElementsIn(FakeAudioEffectsBackend.PRESETS.getValue(1)).inOrder()
        assertThat(controller.state.value.preset).isNotNull()
    }

    @Test fun saveAsPreset_storesCurrentLevelsUnderTheNameAndListsIt() {
        val controller = launch()
        controller.setBand(0, 400)
        compose.onNodeWithText("Save as preset").performClick()
        compose.onNodeWithTag("playlist-name-field").performTextInput("Warm")
        compose.onNodeWithText("Save").performClick()
        compose.waitForIdle()
        assertThat(controller.state.value.customPresets.keys).containsExactly("Warm")
        assertThat(controller.state.value.customPresets.getValue("Warm")[0]).isEqualTo(400)
        compose.onNodeWithText("Preset: Warm").assertIsDisplayed()
    }

    @Test fun bassBoostSlider_onlyWhenSupported() {
        launch()
        compose.onNodeWithTag("eq-bass").assertExists()
    }

    @Test fun bassBoostSlider_hiddenWhenUnsupported() {
        launch(FakeAudioEffectsBackend.DEFAULT_CAPS.copy(bassBoostSupported = false))
        compose.onNodeWithTag("eq-bass").assertDoesNotExist()
    }

    @Test fun preampSlider_attenuatesWhileEnabled() {
        launch()
        compose.onNodeWithTag("eq-switch").performClick()
        compose.onNodeWithTag("eq-preamp").performSemanticsAction(SemanticsActions.SetProgress) { it(-6f) }
        compose.waitForIdle()
        assertThat(volumes.last()).isWithin(0.001f).of(0.501f)
    }

    @Test fun unsupportedDevice_showsUnavailableMessage() {
        launch(capabilities = null)
        compose.onNodeWithText("Equalizer unavailable").assertIsDisplayed()
        compose.onNodeWithTag("eq-switch").assertDoesNotExist()
    }

    @Test fun labelCaps_doesNotUppercaseTheUnits() {
        launch(skin = Skin.FALLBACK.copy(labelCaps = true))
        listOf("60 Hz", "3.6 kHz").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }
}
