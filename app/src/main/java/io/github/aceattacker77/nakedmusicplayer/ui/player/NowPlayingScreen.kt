package io.github.aceattacker77.nakedmusicplayer.ui.player

import io.github.aceattacker77.nakedmusicplayer.ui.components.cornerBrackets
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.components.trackCode
import io.github.aceattacker77.nakedmusicplayer.ui.components.ArtCaptions
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.LayoutType
import io.github.aceattacker77.nakedmusicplayer.ui.skins.PlayerStyle
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalSkin
import com.materialkolor.dynamicColorScheme

/** What the user can do from Now Playing. Screens stay stateless and just invoke these. */
@Immutable
class NowPlayingActions(
    val onPlayPause: () -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onToggleShuffle: () -> Unit,
    val onCycleRepeat: () -> Unit,
    val onOpenQueue: () -> Unit,
    val onCollapse: () -> Unit,
    val onAddToPlaylist: () -> Unit,
    val onOpenEqualizer: () -> Unit,
) {
    companion object {
        /** No-op actions, for previews and screenshot tests. */
        val None = NowPlayingActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}

private const val SPIN_PERIOD_MS = 12_000
private const val FULL_TURN = 360f

/**
 * The full-screen player. Every visual comes from [LocalSkin] (background, art shape, seek bar,
 * controls, layout) and the active theme; [artworkModifier] lets a host attach a shared-element
 * transition to the artwork.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    positionMs: Long,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    equalizerAvailable: Boolean = false,
    applyWindowInsets: Boolean = true,
) {
    val skin = LocalSkin.current
    val style = skin.player
    val item = state.current
    val albumId = remember(item) { item?.mediaMetadata?.artworkUri?.lastPathSegment?.toLongOrNull() }

    // With useArtColors the whole screen is re-tinted from the artwork's accent.
    val accent = if (style.useArtColors) rememberArtAccent(albumId) else null
    val base = MaterialTheme.colorScheme
    val isDark = base.background.luminance() < 0.5f
    val scheme = remember(accent, base, isDark) {
        accent?.let { dynamicColorScheme(seedColor = Color(it or OPAQUE), isDark = isDark) } ?: base
    }

    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        // Sets the content colour for everything inside, whatever hosts this screen.
        Surface(
            modifier = modifier.fillMaxSize().testTag("now-playing"),
            color = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
            Box(Modifier.fillMaxSize()) {
                PlayerBackground(style.background, skin, albumId)
                Column(Modifier.fillMaxSize().then(if (applyWindowInsets) Modifier.systemBarsPadding() else Modifier)) {
                    CollapseBar(actions.onCollapse)
                    // Layouts that put a small thumbnail beside the title read better left-aligned.
                    val centeredInfo = skin.layout.type != LayoutType.COMPACT &&
                        !(skin.layout.type == LayoutType.MINIMAL && skin.layout.artPosition == ArtPosition.LEFT)
                    val slots = remember(state, positionMs, style, actions, albumId, artworkModifier, centeredInfo, equalizerAvailable) {
                        buildSlots(state, positionMs, style, actions, albumId, artworkModifier, centeredInfo, equalizerAvailable)
                    }
                    NowPlayingLayout(skin.layout, slots, Modifier.weight(1f))
                }
            }
        }
    }
}

private const val OPAQUE = 0xFF000000.toInt()

@Composable
private fun CollapseBar(onCollapse: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCollapse) {
            Icon(painterResource(R.drawable.ic_keyboard_arrow_down), contentDescription = stringResource(R.string.collapse_player))
        }
    }
}

private fun buildSlots(
    state: PlayerUiState,
    positionMs: Long,
    style: PlayerStyle,
    actions: NowPlayingActions,
    albumId: Long?,
    artworkModifier: Modifier,
    centeredInfo: Boolean,
    equalizerAvailable: Boolean,
): NowPlayingSlots {
    val meta = state.current?.mediaMetadata
    return NowPlayingSlots(
        artwork = {
            val captions = if (LocalOrnament.current.artPlaceholder == ArtPlaceholder.HEXAGON) {
                ArtCaptions(stringResource(R.string.no_artwork), trackCode(state.currentIndex))
            } else {
                null
            }
            Artwork(albumId, style, spinning = state.isPlaying, modifier = artworkModifier, captions = captions)
        },
        trackInfo = {
            TrackInfo(
                title = meta?.title?.toString().orEmpty(),
                artist = meta?.artist?.toString().orEmpty(),
                album = meta?.albumTitle?.toString().orEmpty(),
                centered = centeredInfo,
            )
        },
        seekBar = {
            SeekBarWithTimes(
                style.seekBar, positionMs, state.durationMs, actions.onSeek, isPlaying = state.isPlaying,
                segments = style.seekSegments, seekColor = style.seekColor,
                showHeader = LocalOrnament.current.segmentedMeters,
            )
        },
        controls = {
            PlayerControls(
                style = style.controls,
                size = style.controlSize,
                isPlaying = state.isPlaying,
                glow = style.glow,
                onPrevious = actions.onPrevious,
                onPlayPause = actions.onPlayPause,
                onNext = actions.onNext,
                shape = controlShapeFor(style.controlShape, MaterialTheme.shapes),
            )
        },
        secondaryControls = {
            SecondaryControls(
                shuffle = state.shuffle,
                repeatMode = state.repeatMode,
                onToggleShuffle = actions.onToggleShuffle,
                onCycleRepeat = actions.onCycleRepeat,
                onAddToPlaylist = actions.onAddToPlaylist,
                onOpenEqualizer = if (equalizerAvailable) actions.onOpenEqualizer else null,
            )
        },
        queueHandle = { QueueHandle(actions.onOpenQueue) },
    )
}

/** The artwork in the skin's shape, optionally shadowed and slowly spinning while [spinning]. */
@Composable
private fun Artwork(albumId: Long?, style: PlayerStyle, spinning: Boolean, modifier: Modifier = Modifier, captions: ArtCaptions? = null) {
    val shape: Shape = when (val s = style.artShape) {
        ArtShape.Square -> RectangleShape
        ArtShape.Circle -> CircleShape
        is ArtShape.Rounded -> RoundedCornerShape(s.radiusDp.dp)
    }

    // Keeps its angle when paused (the coroutine is just cancelled), so the disc resumes in place.
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(style.artSpin, spinning) {
        if (style.artSpin && spinning) {
            while (true) {
                rotation.animateTo(rotation.value + FULL_TURN, tween(SPIN_PERIOD_MS, easing = LinearEasing))
                rotation.snapTo(rotation.value % FULL_TURN)
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .aspectRatio(1f)
            .then(if (LocalOrnament.current.brackets) Modifier.cornerBrackets(MaterialTheme.colorScheme.primary) else Modifier)
            .then(if (style.shadow) Modifier.shadow(12.dp, shape) else Modifier)
            .graphicsLayer { rotationZ = if (style.artSpin) rotation.value else 0f },
    ) {
        AlbumArt(albumId, Modifier.fillMaxSize(), shape, captions = captions)
    }
}
