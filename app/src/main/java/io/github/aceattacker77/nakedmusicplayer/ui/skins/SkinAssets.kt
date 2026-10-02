package io.github.aceattacker77.nakedmusicplayer.ui.skins

import androidx.compose.ui.text.font.FontFamily
import io.github.aceattacker77.nakedmusicplayer.ui.theme.loadSkinFontFamily
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Resolves a skin's font and image files, caching the parsed font per installed skin version. */
object SkinAssets {
    private data class Key(val id: String, val fontPath: String?, val dir: String?, val modified: Long)

    private val fonts = ConcurrentHashMap<Key, FontFamily>()

    fun fontFamily(skin: Skin): FontFamily? {
        if (skin.fontPath == null || skin.baseDir == null) return null
        val key = Key(skin.id, skin.fontPath, skin.baseDir.path, File(skin.baseDir, skin.fontPath).lastModified())
        fonts[key]?.let { return it }
        return loadSkinFontFamily(skin)?.also { fonts[key] = it }
    }

    fun imageModel(skin: Skin, path: String): File =
        File(requireNotNull(skin.baseDir) { "skin '${skin.id}' has no files" }, path)
}
