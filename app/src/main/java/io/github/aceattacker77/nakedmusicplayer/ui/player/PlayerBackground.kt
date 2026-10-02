package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumArtModel
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BackgroundStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinAssets

private const val BLUR_RADIUS_DP = 48
private const val SCRIM_ALPHA = 0.6f
private const val GRADIENT_TOP_ALPHA = 0.35f

/** Paints the Now Playing backdrop in the skin's [style]. Always legible: images get a scrim. */
@Composable
fun PlayerBackground(style: BackgroundStyle, skin: Skin, albumId: Long?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (style) {
        BackgroundStyle.Solid -> Box(modifier.fillMaxSize().background(colors.background))

        BackgroundStyle.ArtGradient -> Box(
            modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(colors.primary.copy(alpha = GRADIENT_TOP_ALPHA), colors.background)),
            ),
        )

        BackgroundStyle.BlurredArt -> Box(modifier.fillMaxSize().background(colors.background)) {
            // Blur needs API 31+; older devices keep the plain backdrop with a tinted wash.
            if (albumId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AsyncImage(
                    model = albumArtModel(albumId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(BLUR_RADIUS_DP.dp),
                )
            } else {
                Box(Modifier.fillMaxSize().background(colors.primary.copy(alpha = GRADIENT_TOP_ALPHA / 2)))
            }
            Box(Modifier.fillMaxSize().background(colors.background.copy(alpha = SCRIM_ALPHA)))
        }

        is BackgroundStyle.Image -> Box(modifier.fillMaxSize().background(colors.background)) {
            AsyncImage(
                model = SkinAssets.imageModel(skin, style.path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Transparent).background(colors.background.copy(alpha = SCRIM_ALPHA)))
        }
    }
}
