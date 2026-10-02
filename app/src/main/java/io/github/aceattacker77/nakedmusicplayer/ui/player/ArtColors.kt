package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumArtModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object ArtColors {
    private const val MIN_VIBRANT_SATURATION = 0.4f
    private const val MIN_VIBRANT_LIGHTNESS = 0.25f
    private const val MAX_VIBRANT_LIGHTNESS = 0.8f

    /**
     * Picks an accent from artwork swatches (rgb, population): the most saturated-and-common
     * mid-tone colour if there is one, otherwise the most common colour, otherwise null.
     */
    fun pickAccent(swatches: List<Pair<Int, Int>>): Int? {
        if (swatches.isEmpty()) return null
        val vibrant = swatches.filter { (rgb, _) ->
            val (saturation, lightness) = saturationAndLightness(rgb)
            saturation >= MIN_VIBRANT_SATURATION && lightness in MIN_VIBRANT_LIGHTNESS..MAX_VIBRANT_LIGHTNESS
        }
        return if (vibrant.isNotEmpty()) {
            vibrant.maxBy { (rgb, population) -> population * saturationAndLightness(rgb).first }.first
        } else {
            swatches.maxBy { it.second }.first
        }
    }

    /** HSL saturation and lightness of an `0xRRGGBB` colour (alpha ignored), both in 0..1. */
    private fun saturationAndLightness(rgb: Int): Pair<Float, Float> {
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val lightness = (hi + lo) / 2f
        val delta = hi - lo
        val saturation = if (delta == 0f) 0f else delta / (1f - abs(2f * lightness - 1f))
        return saturation to lightness
    }
}

/** The accent colour of an album's artwork, or null while loading, when there is no art, or on failure. */
@Composable
fun rememberArtAccent(albumId: Long?): Int? {
    val context = LocalContext.current
    val accent by produceState<Int?>(initialValue = null, albumId) {
        value = if (albumId == null) null else withContext(Dispatchers.Default) { loadAccent(context, albumId) }
    }
    return accent
}

private suspend fun loadAccent(context: Context, albumId: Long): Int? {
    val request = ImageRequest.Builder(context)
        .data(albumArtModel(albumId))
        .size(PALETTE_SOURCE_PX)
        .allowHardware(false) // Palette needs readable pixels
        .build()
    val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult ?: return null
    val palette = Palette.from(result.image.toBitmap()).generate()
    return ArtColors.pickAccent(palette.swatches.map { it.rgb to it.population })
}

private const val PALETTE_SOURCE_PX = 128
