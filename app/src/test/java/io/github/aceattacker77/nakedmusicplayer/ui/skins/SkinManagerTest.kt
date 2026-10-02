package io.github.aceattacker77.nakedmusicplayer.ui.skins

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SkinManagerTest {
    @get:Rule val tmp = TemporaryFolder()

    private val default = Skin.FALLBACK
    private val vinyl = Skin.FALLBACK.copy(id = "builtin.vinyl", name = "Vinyl")
    private val reader = SkinArchiveReader(Skin.FALLBACK, { 10 to 10 }, { true })

    private class Fixture(val manager: SkinManager, val settings: SettingsRepository)

    private fun TestScope.fixture(): Fixture {
        val settings = SettingsRepository(InMemoryPreferencesStore())
        val store = SkinStore(File(tmp.root, "skins")) { SkinParser.parse(it, Skin.FALLBACK) }
        val manager = SkinManager(
            builtIns = listOf(default, vinyl),
            store = store,
            reader = reader,
            settings = settings,
            scope = backgroundScope,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        return Fixture(manager, settings)
    }

    private fun zipFor(id: String, name: String) = ByteArrayInputStream(
        zipOf(listOf("skin.json" to """{"format":1,"id":"$id","name":"$name"}""".toByteArray())),
    )

    @Test fun active_followsSetting() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        assertThat(f.manager.active.value.id).isEqualTo("builtin.default")
        f.settings.update { it.copy(activeSkinId = "builtin.vinyl") }
        assertThat(f.manager.active.value.id).isEqualTo("builtin.vinyl")
    }

    @Test fun activeMissing_fallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        f.settings.update { it.copy(activeSkinId = "x.gone") }
        assertThat(f.manager.active.value.id).isEqualTo("builtin.default")
    }

    @Test fun apply_setsActiveSkin_andIgnoresUnknownIds() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        f.manager.apply("builtin.vinyl")
        assertThat(f.manager.active.value.id).isEqualTo("builtin.vinyl")
        f.manager.apply("nope.nope")
        assertThat(f.manager.active.value.id).isEqualTo("builtin.vinyl")
        assertThat(f.settings.settings.first().activeSkinId).isEqualTo("builtin.vinyl")
    }

    @Test fun deleteActive_switchesToDefault() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        assertThat(f.manager.import(zipFor("x.y", "Mine"), replace = false)).isInstanceOf(ImportOutcome.Installed::class.java)
        f.manager.apply("x.y")
        assertThat(f.manager.active.value.id).isEqualTo("x.y")
        f.manager.delete("x.y")
        assertThat(f.manager.active.value.id).isEqualTo("builtin.default")
        assertThat(f.settings.settings.first().activeSkinId).isEqualTo("builtin.default")
        assertThat(f.manager.available.value.map { it.id }).doesNotContain("x.y")
    }

    @Test fun import_failed_leavesActiveUnchanged() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        f.manager.apply("builtin.vinyl")
        val outcome = f.manager.import(ByteArrayInputStream("not a zip".toByteArray()), replace = false)
        assertThat(outcome).isEqualTo(ImportOutcome.Failed("skin.json not found"))
        assertThat(f.manager.active.value.id).isEqualTo("builtin.vinyl")
        assertThat(f.manager.available.value).hasSize(2)
    }

    @Test fun available_isBuiltInsThenImportedByName() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        f.manager.import(zipFor("z.z", "Zed"), replace = false)
        f.manager.import(zipFor("a.a", "alpha"), replace = false)
        assertThat(f.manager.available.value.map { it.name }).containsExactly("Default", "Vinyl", "alpha", "Zed").inOrder()
    }

    @Test fun import_sameId_asksBeforeReplacing() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        f.manager.import(zipFor("a.a", "First"), replace = false)
        assertThat(f.manager.import(zipFor("a.a", "Second"), replace = false)).isEqualTo(ImportOutcome.AlreadyExists("a.a"))
        assertThat(f.manager.import(zipFor("a.a", "Second"), replace = true)).isInstanceOf(ImportOutcome.Installed::class.java)
        assertThat(f.manager.available.value.map { it.name }).contains("Second")
        assertThat(f.manager.available.value.map { it.name }).doesNotContain("First")
    }

    @Test fun import_builtInId_isRejected() = runTest(UnconfinedTestDispatcher()) {
        val f = fixture()
        val outcome = f.manager.import(zipFor("builtin.default", "Fake"), replace = true)
        assertThat(outcome).isEqualTo(ImportOutcome.Failed("id 'builtin.default' is reserved for built-in skins"))
        assertThat(f.manager.available.value).hasSize(2)
    }
}
