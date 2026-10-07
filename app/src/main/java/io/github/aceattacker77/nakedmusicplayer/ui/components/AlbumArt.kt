package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import androidx.compose.foundation.border
import io.github.aceattacker77.nakedmusicplayer.ui.theme.shapeOr
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.aceattacker77.nakedmusicplayer.R

/** What Coil should load for an album's artwork on this Android version. */
fun albumArtModel(albumId: Long): Any =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AlbumArtModel(albumId)
    } else {
        "content://media/external/audio/albumart/$albumId"
    }

/**
 * Album artwork over a themed placeholder. The placeholder stays visible when an album has no art
 * (the image simply never draws), so no separate error state is needed. A null [albumId] means
 * "nothing playing / unknown": only the placeholder shows.
 */
@Composable
fun AlbumArt(
    albumId: Long?,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    highlighted: Boolean = false,
    captions: ArtCaptions? = null,
) {
    val ornament = LocalOrnament.current
    val resolved = shape ?: shapeOr(RoundedCornerShape(8.dp), MaterialTheme.shapes.small)
    Box(
        modifier = modifier.clip(resolved).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        var loadFailed by remember(albumId) { mutableStateOf(false) }
        if (ornament.artPlaceholder == ArtPlaceholder.HEXAGON) {
            HexagonPlaceholder(captions = captions?.takeIf { captionsVisible(albumId, loadFailed) })
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.5f).padding(2.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
        if (albumId != null) {
            val model = remember(albumId) { albumArtModel(albumId) }
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { loadFailed = false },
                onError = { loadFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (ornament.artBorder || highlighted) {
            val color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Box(Modifier.fillMaxSize().border(1.dp, color, resolved).testTag("art-border"))
        }
    }
}

/** The "No artwork" captions belong to a tile with no artwork: no album, or artwork that failed to load (not while it loads). */
internal fun captionsVisible(albumId: Long?, loadFailed: Boolean): Boolean = albumId == null || loadFailed
