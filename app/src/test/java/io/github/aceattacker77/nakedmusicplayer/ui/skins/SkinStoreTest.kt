package io.github.aceattacker77.nakedmusicplayer.ui.skins

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SkinStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val reader = SkinArchiveReader(Skin.FALLBACK, { 10 to 10 }, { true })
    private fun store(root: File = File(tmp.root, "skins")) = SkinStore(root) { SkinParser.parse(it, Skin.FALLBACK) }

    private fun valid(id: String = "a.b", name: String = "X"): SkinImportResult.Valid {
        val json = """{"format":1,"id":"$id","name":"$name","player":{"background":{"type":"image","path":"images/bg.png"}}}"""
        val zip = zipOf(listOf("skin.json" to json.toByteArray(), "images/bg.png" to byteArrayOf(1, 2, 3)))
        return reader.read(ByteArrayInputStream(zip)) as SkinImportResult.Valid
    }

    @Test fun install_writesFiles_andListsSkin() {
        val store = store()
        val result = store.install(valid(), replace = false)
        assertThat(result).isInstanceOf(InstallResult.Installed::class.java)
        val dir = File(tmp.root, "skins/a.b")
        assertThat(File(dir, "skin.json").isFile).isTrue()
        assertThat(File(dir, "images/bg.png").readBytes()).isEqualTo(byteArrayOf(1, 2, 3))
        val installed = store.installed().single()
        assertThat(installed.id).isEqualTo("a.b")
        assertThat(installed.baseDir).isEqualTo(dir)
    }

    @Test fun install_sameId_returnsAlreadyExists_unlessReplace() {
        val store = store()
        store.install(valid(name = "First"), replace = false)
        assertThat(store.install(valid(name = "Second"), replace = false)).isEqualTo(InstallResult.AlreadyExists("a.b"))
        assertThat(store.installed().single().name).isEqualTo("First")

        val replaced = store.install(valid(name = "Second"), replace = true)
        assertThat(replaced).isInstanceOf(InstallResult.Installed::class.java)
        assertThat(store.installed().single().name).isEqualTo("Second")
    }

    @Test fun delete_removesDir() {
        val store = store()
        store.install(valid(), replace = false)
        store.delete("a.b")
        assertThat(File(tmp.root, "skins/a.b").exists()).isFalse()
        assertThat(store.installed()).isEmpty()
    }

    @Test fun delete_cannotEscapeRoot() {
        val outside = tmp.newFolder("outside")
        val store = store()
        store.install(valid(), replace = false)
        store.delete("../outside")
        assertThat(outside.exists()).isTrue()
    }

    @Test fun export_thenRead_roundTrips() {
        val store = store()
        val original = valid()
        val skin = (store.install(original, replace = false) as InstallResult.Installed).skin
        val out = ByteArrayOutputStream()
        store.export(skin, out)
        val reread = reader.read(ByteArrayInputStream(out.toByteArray())) as SkinImportResult.Valid
        assertThat(reread.skin.id).isEqualTo("a.b")
        assertThat(reread.entries.keys).containsExactlyElementsIn(original.entries.keys)
        assertThat(reread.entries["images/bg.png"]).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun installed_skipsCorruptDirs() {
        val store = store()
        store.install(valid(), replace = false)
        File(tmp.root, "skins/broken").apply { mkdirs() }
        File(tmp.root, "skins/broken/skin.json").writeText("{ not json")
        File(tmp.root, "skins/empty").mkdirs()
        assertThat(store.installed().map { it.id }).containsExactly("a.b")
    }

    @Test fun installed_whenRootMissing_isEmpty() {
        assertThat(store(File(tmp.root, "nothing-here")).installed()).isEmpty()
    }
}
