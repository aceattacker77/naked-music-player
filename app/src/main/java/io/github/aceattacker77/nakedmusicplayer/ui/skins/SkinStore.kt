package io.github.aceattacker77.nakedmusicplayer.ui.skins

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed interface InstallResult {
    data class Installed(val skin: Skin) : InstallResult
    data class AlreadyExists(val id: String) : InstallResult
}

/** Skins installed under `rootDir/<id>/`. */
class SkinStore(
    private val rootDir: File,
    private val parse: (String) -> SkinParseResult,
) {
    fun installed(): List<Skin> =
        rootDir.listFiles { f -> f.isDirectory }.orEmpty().sortedBy { it.name }.mapNotNull { dir ->
            val json = File(dir, SKIN_JSON).takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
            when (val result = json?.let(parse)) {
                is SkinParseResult.Ok -> result.skin.copy(baseDir = dir)
                else -> null
            }
        }

    fun install(valid: SkinImportResult.Valid, replace: Boolean): InstallResult {
        val id = valid.skin.id
        val target = childOf(id) ?: throw IllegalArgumentException("invalid skin id '$id'")
        if (target.exists() && !replace) return InstallResult.AlreadyExists(id)

        // Write beside the target first so a failed write never destroys an installed skin.
        rootDir.mkdirs()
        val staging = File(rootDir, ".staging-$id")
        staging.deleteRecursively()
        try {
            valid.entries.forEach { (name, bytes) ->
                val file = File(staging, name)
                require(file.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "unsafe path '$name'" }
                file.parentFile?.mkdirs()
                file.writeBytes(bytes)
            }
            target.deleteRecursively()
            check(staging.renameTo(target)) { "could not install skin '$id'" }
        } finally {
            staging.deleteRecursively()
        }
        return InstallResult.Installed(valid.skin.copy(baseDir = target))
    }

    fun delete(id: String) {
        childOf(id)?.deleteRecursively()
    }

    fun export(skin: Skin, out: OutputStream) {
        val dir = requireNotNull(skin.baseDir) { "skin '${skin.id}' has no files to export" }
        ZipOutputStream(out).use { zip ->
            dir.walkTopDown().filter { it.isFile }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(dir).invariantSeparatorsPath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /** Resolves [id] under the root, or null if it would escape it. */
    private fun childOf(id: String): File? {
        val child = File(rootDir, id)
        val root = rootDir.canonicalFile
        return child.canonicalFile.takeIf { it.parentFile == root }
    }

    private companion object {
        const val SKIN_JSON = "skin.json"
    }
}
