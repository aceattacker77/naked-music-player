package io.github.aceattacker77.nakedmusicplayer.ui.settings

import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinText
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.StatusTag
import io.github.aceattacker77.nakedmusicplayer.ui.components.StatusKind
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoPanel
import io.github.aceattacker77.nakedmusicplayer.ui.theme.shapeOr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingActions
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingScreen
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerUiState
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme

private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 640

/**
 * A skin picker entry: a miniature, non-interactive Now Playing rendered under the skin's own
 * theme, with the skin's name beneath. Tap applies it; long-press opens its menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkinPreviewCard(
    skin: Skin,
    settings: AppSettings,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = shapeOr(RoundedCornerShape(16.dp), MaterialTheme.shapes.large)
    val ornament = LocalOrnament.current
    val previewBox: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().aspectRatio(PREVIEW_WIDTH_DP.toFloat() / PREVIEW_HEIGHT_DP).clipToBounds()) {
            SkinPreview(skin, settings)
            // The preview is only a picture; this layer on top is what receives the gestures.
            Box(Modifier.fillMaxSize().combinedClickable(onClick = onClick, onLongClick = onLongClick))
        }
    }
    Column(modifier.testTag("skin-card-${skin.id}")) {
        if (selected && ornament.brackets && ornament.accents) {
            // The active card is a bracketed live panel in a skin that uses brackets and the Phase 3 accents.
            GeoPanel(modifier = Modifier.testTag("active-panel"), live = true, content = previewBox)
        } else {
            Surface(
                shape = shape,
                border = BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) { previewBox() }
        }
        Text(
            text = skin.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        )
        if (selected && ornament.statusTags) {
            val active = skinText("skin_active_tag", stringResource(R.string.skin_active_tag))
            StatusTag(StatusKind.GOOD, active.english, active.kana, Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

@Composable
private fun SkinPreview(skin: Skin, settings: AppSettings) {
    val sample = stringResource(R.string.skin_preview_sample)
    val state = remember(sample) {
        PlayerUiState(
            current = MediaItem.Builder()
                .setMediaId("preview")
                .setMediaMetadata(MediaMetadata.Builder().setTitle(sample).setArtist(sample).setAlbumTitle(sample).build())
                .build(),
            isPlaying = false,
            durationMs = 240_000,
            queue = emptyList(),
            currentIndex = 0,
            shuffle = false,
            repeatMode = 0,
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize().clearAndSetSemantics {}) {
        val scale = maxWidth.value / PREVIEW_WIDTH_DP
        // A small card has no room for 40 cells, so the preview draws a coarser segmented bar.
        val shown = remember(skin) { skin.copy(player = skin.player.copy(seekSegments = PREVIEW_CELLS)) }
        AppTheme(shown, settings) {
            Box(
                Modifier
                    // Anchored top-left (and allowed to overflow) so scaling about the origin fits the card.
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .requiredSize(PREVIEW_WIDTH_DP.dp, PREVIEW_HEIGHT_DP.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                NowPlayingScreen(
                    state = state,
                    positionMs = 80_000,
                    actions = NowPlayingActions.None,
                    applyWindowInsets = false,
                )
            }
        }
    }
}

private const val PREVIEW_CELLS = 14
