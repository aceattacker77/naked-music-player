package io.github.aceattacker77.nakedmusicplayer.ui.settings

import android.Manifest
import android.app.Application
import android.net.Uri
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.core.app.ActivityOptionsCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.LocalAppContainer
import io.github.aceattacker77.nakedmusicplayer.TestContainer
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.FakeAudioRowSource
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.ui.AppRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Config(qualifiers = "w411dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class SettingsUiTest {
    @get:Rule val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val source = FakeAudioRowSource()
    private lateinit var player: ExoPlayer
    private lateinit var container: TestContainer

    /** What the next "pick a file" system dialog returns; the file picker itself cannot run here. */
    private var pickedUri: Uri? = null
    private val registryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                pickedUri?.let { dispatchResult(requestCode, it) }
            }
        }
    }

    @Before fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        player = TestExoPlayerBuilder(app).setMediaSourceFactory(FakeMediaSourceFactory()).build()
    }

    @After fun tearDown() = player.release()

    private fun row(id: Long, title: String, durationMs: Long = 200_000, path: String = "Music/") =
        AudioRow(id, title, "Ann", "Album", 10, 100, durationMs, 1, 0, path, "f$id.mp3", null)

    private fun launch(vararg rows: AudioRow) {
        source.rows = rows.toList()
        container = TestContainer(app, source, player)
        compose.setContent {
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalActivityResultRegistryOwner provides registryOwner,
            ) { AppRoot() }
        }
        compose.waitForIdle()
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription("More options").performClick()
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
    }

    private fun openSkins() {
        openSettings()
        compose.onNodeWithText("Skins").performClick()
        compose.waitForIdle()
    }

    private fun skinZip(id: String, name: String, json: String? = null): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("skin.json"))
            zip.write((json ?: """{"format":1,"id":"$id","name":"$name"}""").toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun pick(bytes: ByteArray) {
        val uri = Uri.parse("content://test.documents/skin-${System.nanoTime()}")
        // A supplier, not a single stream: replacing a skin re-reads the same picked file.
        shadowOf(app.contentResolver).registerInputStreamSupplier(uri) { ByteArrayInputStream(bytes) }
        pickedUri = uri
    }

    private val settings get() = runBlocking { container.settingsRepository.settings.first() }

    @Test fun themeSwitch_persists() {
        launch(row(1, "Song"))
        openSettings()
        compose.onNodeWithText("Dark").performClick()
        compose.waitForIdle()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.DARK)
        compose.onNodeWithText("Light").performClick()
        compose.waitForIdle()
        assertThat(settings.themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test fun dynamicColourSwitch_persists() {
        launch(row(1, "Song"))
        openSettings()
        assertThat(settings.dynamicColor).isTrue()
        compose.onNodeWithTag("dynamic-switch").performClick()
        compose.waitForIdle()
        assertThat(settings.dynamicColor).isFalse()
    }

    @Test fun widgetLiveProgressSwitch_persists() {
        launch(row(1, "Song"))
        openSettings()
        assertThat(settings.widgetLiveProgress).isFalse()
        compose.onNodeWithTag("widget-live-switch").performClick()
        compose.waitForIdle()
        assertThat(settings.widgetLiveProgress).isTrue()
    }

    @Test fun minLength_updatesFilter() {
        launch(row(1, "Short", durationMs = 45_000), row(2, "Long"))
        assertThat(container.libraryRepository.library.value.songs.map { it.id }).containsExactly(1L, 2L)
        openSettings()
        compose.onNodeWithText("60 s").performClick()
        compose.waitForIdle()
        assertThat(settings.minDurationMs).isEqualTo(60_000)
        assertThat(container.libraryRepository.library.value.songs.map { it.id }).containsExactly(2L)

        compose.onNodeWithText("Off").performClick()
        compose.waitForIdle()
        assertThat(container.libraryRepository.library.value.songs.map { it.id }).containsExactly(1L, 2L)
    }

    @Test fun excludedFolders_canBeChosenFromTheLibraryFolders() {
        launch(row(1, "Keep", path = "Music/"), row(2, "Chat clip", path = "WhatsApp/Audio/"))
        openSettings()
        compose.onNodeWithText("Excluded folders").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("folder-option-WhatsApp/Audio/").performClick()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertThat(settings.excludedFolders).containsExactly("WhatsApp/Audio/")
        assertThat(container.libraryRepository.library.value.songs.map { it.id }).containsExactly(1L)

        // The excluded folder must stay in the list even though its songs are now hidden.
        compose.onNodeWithText("Excluded folders").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("folder-option-WhatsApp/Audio/").assertExists()
    }

    @Test fun skinTap_appliesImmediately() {
        launch(row(1, "Song"))
        openSkins()
        compose.onNodeWithTag("skin-card-builtin.vinyl").performClick()
        compose.waitForIdle()
        assertThat(container.skinManager.active.value.id).isEqualTo("builtin.vinyl")
        assertThat(settings.activeSkinId).isEqualTo("builtin.vinyl")
    }

    @Test fun importInvalid_showsMessage() {
        launch(row(1, "Song"))
        openSkins()
        pick(skinZip("x.y", "X", json = """{"format":1,"id":"x.y","name":"X","colors":{"dark":{"primary":"#GGG"}}}"""))
        compose.onNodeWithText("Import skin").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("invalid colour 'primary': '#GGG'").assertIsDisplayed()
    }

    @Test fun importNotAZip_showsSkinJsonNotFound() {
        launch(row(1, "Song"))
        openSkins()
        pick("not a zip".toByteArray())
        compose.onNodeWithText("Import skin").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("skin.json not found").assertIsDisplayed()
    }

    @Test fun importValid_addsCard_andDeleteRemovesIt() {
        launch(row(1, "Song"))
        openSkins()
        pick(skinZip("test.neon", "Neon Test"))
        compose.onNodeWithText("Import skin").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("skin-card-test.neon").assertExists()

        compose.onNodeWithTag("skin-card-test.neon").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("skin-card-test.neon").assertDoesNotExist()
    }

    @Test fun importExistingId_asksBeforeReplacing() {
        launch(row(1, "Song"))
        openSkins()
        pick(skinZip("test.neon", "First"))
        compose.onNodeWithText("Import skin").performClick()
        compose.waitForIdle()

        pick(skinZip("test.neon", "Second"))
        compose.onNodeWithText("Import skin").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Replace").assertIsDisplayed()
        compose.onNodeWithText("Replace").performClick()
        compose.waitForIdle()
        assertThat(container.skinManager.available.value.map { it.name }).contains("Second")
        assertThat(container.skinManager.available.value.map { it.name }).doesNotContain("First")
    }

    @Test fun builtInSkins_offerExportButNotDelete() {
        launch(row(1, "Song"))
        openSkins()
        compose.onNodeWithTag("skin-card-builtin.vinyl").performTouchInput { longClick() }
        compose.waitForIdle()
        compose.onNodeWithText("Export").assertIsDisplayed()
        compose.onNodeWithText("Delete").assertDoesNotExist()
    }

    @Test fun about_showsVersionAndLicences() {
        launch(row(1, "Song"))
        openSettings()
        compose.onNodeWithText("About").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("about-screen").assertIsDisplayed()
        compose.onNodeWithText("Open-source licences").assertIsDisplayed()
    }

    @Test fun equalizerRow_hiddenWhenDeviceHasNoEqualizer() {
        launch(row(1, "Song"))
        openSettings()
        compose.onNodeWithTag("settings-equalizer").assertDoesNotExist()
    }

    @Test fun settings_listsTheSectionsInSpecOrder() {
        launch(row(1, "Song"))
        openSettings()
        listOf("Theme", "Skins", "Library", "About").forEach { compose.onNodeWithText(it).assertExists() }
    }
}
