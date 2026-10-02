package io.github.aceattacker77.nakedmusicplayer.ui.skins

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AndroidProbesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun imageProbe_readsRealPngSize() {
        val png = ByteArrayOutputStream().also {
            Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        assertThat(BitmapImageProbe().size(png)).isEqualTo(10 to 10)
    }

    @Test fun imageProbe_randomBytes_isNull() {
        assertThat(BitmapImageProbe().size(Random.nextBytes(512))).isNull()
    }

    @Test fun fontProbe_loadsSystemFont() {
        // Every Android device ships TTFs; using one avoids bundling a font in the test APK.
        val ttf = File("/system/fonts").listFiles { f -> f.extension == "ttf" }!!.first()
        assertThat(TypefaceFontProbe(File(context.cacheDir, "probe")).loads(ttf.readBytes())).isTrue()
    }

    @Test fun fontProbe_randomBytes_isFalse() {
        assertThat(TypefaceFontProbe(File(context.cacheDir, "probe")).loads(Random.nextBytes(512))).isFalse()
    }

    @Test fun fontProbe_leavesNoTempFiles() {
        val dir = File(context.cacheDir, "probe-clean")
        TypefaceFontProbe(dir).loads(Random.nextBytes(64))
        assertThat(dir.listFiles().orEmpty()).isEmpty()
    }
}
