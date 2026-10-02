package io.github.aceattacker77.nakedmusicplayer.ui.skins

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun zipOf(entries: List<Pair<String, ByteArray>>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

internal fun skinJson(extra: String = "") = """{"format":1,"id":"a.b","name":"X"$extra}"""

@RunWith(RobolectricTestRunner::class)
class SkinArchiveReaderTest {
    private var imageSize: Pair<Int, Int>? = 100 to 100
    private var fontLoads = true

    private val reader = SkinArchiveReader(
        defaults = Skin.FALLBACK,
        imageProbe = { imageSize },
        fontProbe = { fontLoads },
    )

    private fun read(entries: List<Pair<String, ByteArray>>) = reader.read(ByteArrayInputStream(zipOf(entries)))
    private fun invalid(entries: List<Pair<String, ByteArray>>) = (read(entries) as SkinImportResult.Invalid).message
    private fun json(text: String) = "skin.json" to text.toByteArray()
    private val bgJson = skinJson(""","player":{"background":{"type":"image","path":"images/bg.webp"}}""")

    @Test fun tooLarge() {
        val big = ByteArray(10 * 1024 * 1024 + 1)
        assertThat(invalid(listOf(json(skinJson()), "big.bin" to big))).isEqualTo("skin is larger than 10 MB")
    }

    @Test fun tooManyEntries() {
        val files = (0..50).map { "f$it.txt" to byteArrayOf(1) }
        assertThat(invalid(listOf(json(skinJson())) + files)).isEqualTo("skin has more than 50 files")
    }

    @Test fun fiftyEntries_isAllowed() {
        val files = (1..49).map { "f$it.txt" to byteArrayOf(1) }
        assertThat(read(listOf(json(skinJson())) + files)).isInstanceOf(SkinImportResult.Valid::class.java)
    }

    @Test fun pathTraversal() {
        listOf("../evil.json", "/abs.png", "images\\x.png", "C:/x", "a/../../b.png").forEach { name ->
            assertThat(invalid(listOf(json(skinJson()), name to byteArrayOf(1)))).isEqualTo("unsafe path '$name'")
        }
    }

    @Test fun missingSkinJson() {
        assertThat(invalid(listOf("images/a.png" to byteArrayOf(1)))).isEqualTo("skin.json not found")
    }

    @Test fun parserErrorPropagates() {
        val bad = skinJson(""","colors":{"dark":{"primary":"#GGG"}}""")
        assertThat(invalid(listOf(json(bad)))).isEqualTo("invalid colour 'primary': '#GGG'")
    }

    @Test fun missingReferencedFile() {
        assertThat(invalid(listOf(json(bgJson)))).isEqualTo("missing file 'images/bg.webp'")
    }

    @Test fun imageTooBig() {
        imageSize = 2049 to 10
        assertThat(invalid(listOf(json(bgJson), "images/bg.webp" to byteArrayOf(1))))
            .isEqualTo("image 'images/bg.webp' is larger than 2048 px")
        imageSize = 10 to 2049
        assertThat(invalid(listOf(json(bgJson), "images/bg.webp" to byteArrayOf(1))))
            .isEqualTo("image 'images/bg.webp' is larger than 2048 px")
    }

    @Test fun imageAtLimit_isAllowed() {
        imageSize = 2048 to 2048
        assertThat(read(listOf(json(bgJson), "images/bg.webp" to byteArrayOf(1)))).isInstanceOf(SkinImportResult.Valid::class.java)
    }

    @Test fun undecodableImage() {
        imageSize = null
        assertThat(invalid(listOf(json(bgJson), "images/bg.webp" to byteArrayOf(1))))
            .isEqualTo("image 'images/bg.webp' could not be read")
    }

    @Test fun badFont() {
        fontLoads = false
        val fontJson = skinJson(""","typography":{"fontFamily":"fonts/x.ttf"}""")
        assertThat(invalid(listOf(json(fontJson), "fonts/x.ttf" to byteArrayOf(1))))
            .isEqualTo("font 'fonts/x.ttf' could not be loaded")
    }

    @Test fun disallowedExtension() {
        val gifJson = skinJson(""","player":{"background":{"type":"image","path":"images/a.gif"}}""")
        assertThat(invalid(listOf(json(gifJson), "images/a.gif" to byteArrayOf(1))))
            .isEqualTo("unsupported file type 'images/a.gif'")
    }

    @Test fun validSkin_returnsEntries() {
        val result = read(listOf(json(bgJson), "images/bg.webp" to byteArrayOf(1, 2, 3), "README.txt" to byteArrayOf(9)))
        val valid = result as SkinImportResult.Valid
        assertThat(valid.skin.id).isEqualTo("a.b")
        assertThat(valid.skin.player.background).isEqualTo(BackgroundStyle.Image("images/bg.webp"))
        assertThat(valid.entries.keys).containsExactly("skin.json", "images/bg.webp")
        assertThat(valid.entries["images/bg.webp"]).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test fun notAZip_isInvalid() {
        val result = reader.read(ByteArrayInputStream("hello".toByteArray()))
        assertThat((result as SkinImportResult.Invalid).message).isEqualTo("skin.json not found")
    }
}
