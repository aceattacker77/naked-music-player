package io.github.aceattacker77.nakedmusicplayer.ui.skins

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

fun interface ImageProbe {
    /** Width and height of the encoded image, or null if it cannot be decoded. */
    fun size(bytes: ByteArray): Pair<Int, Int>?
}

fun interface FontProbe {
    fun loads(bytes: ByteArray): Boolean
}

sealed interface SkinImportResult {
    data class Valid(val skin: Skin, val entries: Map<String, ByteArray>) : SkinImportResult
    data class Invalid(val message: String) : SkinImportResult
}

/**
 * Validates a `.mskin` archive without touching disk. Every failure yields one specific message and
 * leaves no state behind; limits also protect against zip bombs by counting bytes while streaming.
 */
class SkinArchiveReader(
    private val defaults: Skin,
    private val imageProbe: ImageProbe,
    private val fontProbe: FontProbe,
) {
    private class Rejected(message: String) : Exception(message)

    fun read(input: InputStream): SkinImportResult = try {
        validate(extract(input))
    } catch (e: Rejected) {
        SkinImportResult.Invalid(e.message.orEmpty())
    }

    private fun extract(input: InputStream): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        var total = 0L
        var count = 0
        try {
            ZipInputStream(input).use { zip ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++count > MAX_ENTRIES) throw Rejected("skin has more than $MAX_ENTRIES files")
                    val name = entry.name
                    if (!isSafe(name)) throw Rejected("unsafe path '$name'")
                    if (entry.isDirectory) continue
                    val bytes = ByteArrayOutputStream()
                    while (true) {
                        val n = zip.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) throw Rejected("skin is larger than 10 MB")
                        bytes.write(buffer, 0, n)
                    }
                    entries[name] = bytes.toByteArray()
                }
            }
        } catch (e: ZipException) {
            throw Rejected("skin.json not found")
        } catch (e: IOException) {
            throw Rejected("skin.json not found")
        }
        return entries
    }

    private fun validate(entries: Map<String, ByteArray>): SkinImportResult.Valid {
        val skinJson = entries[SKIN_JSON] ?: throw Rejected("skin.json not found")
        val parsed = when (val result = SkinParser.parse(skinJson.toString(Charsets.UTF_8), defaults)) {
            is SkinParseResult.Error -> throw Rejected(result.message)
            is SkinParseResult.Ok -> result
        }
        parsed.referencedFiles.forEach { path ->
            val extension = path.substringAfterLast('.', "").lowercase()
            if (extension !in IMAGE_EXTENSIONS && extension !in FONT_EXTENSIONS) throw Rejected("unsupported file type '$path'")
            val bytes = entries[path] ?: throw Rejected("missing file '$path'")
            if (extension in IMAGE_EXTENSIONS) {
                val (w, h) = imageProbe.size(bytes) ?: throw Rejected("image '$path' could not be read")
                if (w > MAX_IMAGE_PX || h > MAX_IMAGE_PX) throw Rejected("image '$path' is larger than $MAX_IMAGE_PX px")
            } else if (!fontProbe.loads(bytes)) {
                throw Rejected("font '$path' could not be loaded")
            }
        }
        // Keep only what the skin uses so unreferenced files never reach disk.
        val kept = entries.filterKeys { it == SKIN_JSON || it in parsed.referencedFiles }
        return SkinImportResult.Valid(parsed.skin, kept)
    }

    private fun isSafe(name: String): Boolean =
        name.isNotEmpty() &&
            !name.startsWith("/") &&
            !name.contains('\\') &&
            !DRIVE_PREFIX.containsMatchIn(name) &&
            name.split('/').none { it == ".." }

    private companion object {
        const val SKIN_JSON = "skin.json"
        const val MAX_BYTES = 10L * 1024 * 1024
        const val MAX_ENTRIES = 50
        const val MAX_IMAGE_PX = 2048
        const val BUFFER_SIZE = 8 * 1024
        val IMAGE_EXTENSIONS = setOf("png", "webp")
        val FONT_EXTENSIONS = setOf("ttf", "otf")
        val DRIVE_PREFIX = Regex("^[A-Za-z]:")
    }
}
