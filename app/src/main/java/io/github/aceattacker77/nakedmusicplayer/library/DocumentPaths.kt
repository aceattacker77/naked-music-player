package io.github.aceattacker77.nakedmusicplayer.library

/** Maps Storage Access Framework document ids to the file paths MediaScanner understands. */
object DocumentPaths {
    private const val INTERNAL_ROOT = "/storage/emulated/0"
    private val sdCardVolume = Regex("^[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}$")

    /**
     * `primary:Music/X` -> `/storage/emulated/0/Music/X`, `ABCD-1234:Music` -> `/storage/ABCD-1234/Music`,
     * `raw:/abs/path` -> `/abs/path`. Opaque ids (`msf:12`, cloud providers...) have no path: null.
     */
    fun toFilePath(treeDocumentId: String): String? {
        if (treeDocumentId.startsWith("raw:")) {
            return treeDocumentId.removePrefix("raw:").takeIf { it.startsWith("/") }?.trimEnd('/')
        }
        val colon = treeDocumentId.indexOf(':')
        if (colon < 0) return null
        val volume = treeDocumentId.substring(0, colon)
        val relative = treeDocumentId.substring(colon + 1).trim('/')
        val root = when {
            volume == "primary" -> INTERNAL_ROOT
            sdCardVolume.matches(volume) -> "/storage/$volume"
            else -> return null
        }
        return if (relative.isEmpty()) root else "$root/$relative"
    }
}
