package io.github.aceattacker77.nakedmusicplayer.ui.skins

import androidx.compose.ui.text.font.FontFamily
import io.github.aceattacker77.nakedmusicplayer.ui.theme.loadSkinFontFamily
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** The resolved font family per text role; null leaves the app's default font for that role. */
data class SkinFonts(val heading: FontFamily?, val body: FontFamily?, val label: FontFamily?)

/** Resolves a skin's font and image files, caching each parsed font per installed skin version. */
object SkinAssets {
    private data class Key(val id: String, val fontPath: String, val dir: String, val modified: Long)

    private val fonts = ConcurrentHashMap<Key, FontFamily>()

    fun fontFamily(skin: Skin, role: FontRole): FontFamily? {
        val path = skin.fontPathFor(role) ?: return null
        val dir = skin.baseDir ?: return null
        val key = Key(skin.id, path, dir.path, File(dir, path).lastModified())
        fonts[key]?.let { return it }
        return loadSkinFontFamily(dir, path)?.also { fonts[key] = it }
    }

    fun fonts(skin: Skin) = SkinFonts(
        heading = fontFamily(skin, FontRole.HEADING),
        body = fontFamily(skin, FontRole.BODY),
        label = fontFamily(skin, FontRole.LABEL),
    )

    fun imageModel(skin: Skin, path: String): File =
        File(requireNotNull(skin.baseDir) { "skin '${skin.id}' has no files" }, path)
}
