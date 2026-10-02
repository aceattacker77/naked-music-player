package io.github.aceattacker77.nakedmusicplayer.ui.skins

import android.graphics.BitmapFactory
import android.graphics.Typeface
import java.io.File

/** Reads image dimensions without decoding pixels. */
class BitmapImageProbe : ImageProbe {
    override fun size(bytes: ByteArray): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0) options.outWidth to options.outHeight else null
    }
}

/** Checks that the platform can load a font; `Typeface.Builder` needs a file, so a temp one is used. */
class TypefaceFontProbe(private val tempDir: File) : FontProbe {
    override fun loads(bytes: ByteArray): Boolean {
        tempDir.mkdirs()
        val file = File.createTempFile("skin-font", ".tmp", tempDir)
        return try {
            file.writeBytes(bytes)
            runCatching { Typeface.Builder(file).build() != null }.getOrDefault(false)
        } finally {
            file.delete()
        }
    }
}
