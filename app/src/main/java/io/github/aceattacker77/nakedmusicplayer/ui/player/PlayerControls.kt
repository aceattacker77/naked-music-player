package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlSize
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlsStyle

private data class ControlDimens(val play: Dp, val side: Dp, val playIcon: Dp, val sideIcon: Dp)

private fun ControlSize.dimens() = when (this) {
    ControlSize.SMALL -> ControlDimens(play = 44.dp, side = 40.dp, playIcon = 24.dp, sideIcon = 22.dp)
    ControlSize.MEDIUM -> ControlDimens(play = 60.dp, side = 48.dp, playIcon = 32.dp, sideIcon = 28.dp)
    ControlSize.LARGE -> ControlDimens(play = 76.dp, side = 56.dp, playIcon = 40.dp, sideIcon = 34.dp)
}

/** Previous / play-pause / next in the skin's control style and size. */
@Composable
fun PlayerControls(
    style: ControlsStyle,
    size: ControlSize,
    isPlaying: Boolean,
    glow: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val d = size.dimens()
    val glowColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(style, d.side, R.drawable.ic_skip_previous, stringResource(R.string.previous), d.sideIcon, primary = false, onClick = onPrevious)
        Box(
            modifier = Modifier.then(
                if (glow) {
                    Modifier.drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(listOf(glowColor.copy(alpha = 0.45f), Color.Transparent)),
                            radius = this.size.minDimension * 0.9f,
                        )
                    }
                } else {
                    Modifier
                },
            ),
        ) {
            ControlButton(
                style = style,
                size = d.play,
                icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
                description = stringResource(if (isPlaying) R.string.pause else R.string.play),
                iconSize = d.playIcon,
                primary = true,
                onClick = onPlayPause,
            )
        }
        ControlButton(style, d.side, R.drawable.ic_skip_next, stringResource(R.string.next), d.sideIcon, primary = false, onClick = onNext)
    }
}

@Composable
private fun ControlButton(
    style: ControlsStyle,
    size: Dp,
    icon: Int,
    description: String,
    iconSize: Dp,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit = {
        Icon(painterResource(icon), contentDescription = description, modifier = Modifier.size(iconSize))
    }
    val modifier = Modifier.size(size)
    when (style) {
        ControlsStyle.FILLED ->
            if (primary) {
                FilledIconButton(onClick = onClick, modifier = modifier, content = content)
            } else {
                FilledTonalIconButton(onClick = onClick, modifier = modifier, content = content)
            }
        ControlsStyle.OUTLINED -> OutlinedIconButton(onClick = onClick, modifier = modifier, content = content)
        ControlsStyle.ICON_ONLY -> IconButton(onClick = onClick, modifier = modifier, content = content)
    }
}

/** Title, then artist and album. */
@Composable
fun TrackInfo(
    title: String,
    artist: String,
    album: String,
    modifier: Modifier = Modifier,
    centered: Boolean = true,
) {
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(modifier.fillMaxWidth(), horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = align,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )
        Text(
            text = "${artistLabel(artist)} · ${albumLabel(album)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = align,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shuffle, equalizer (when the device has one), add-to-playlist and repeat. */
@Composable
fun SecondaryControls(
    shuffle: Boolean,
    repeatMode: Int,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenEqualizer: (() -> Unit)? = null,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                painter = painterResource(R.drawable.ic_shuffle),
                contentDescription = stringResource(if (shuffle) R.string.shuffle_on else R.string.shuffle_off),
                tint = if (shuffle) active else inactive,
            )
        }
        if (onOpenEqualizer != null) {
            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    painter = painterResource(R.drawable.ic_equalizer),
                    contentDescription = stringResource(R.string.equalizer),
                    tint = inactive,
                )
            }
        }
        IconButton(onClick = onAddToPlaylist) {
            Icon(
                painter = painterResource(R.drawable.ic_playlist_add),
                contentDescription = stringResource(R.string.add_to_playlist),
                tint = inactive,
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                painter = painterResource(if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat),
                contentDescription = stringResource(
                    when (repeatMode) {
                        Player.REPEAT_MODE_ALL -> R.string.repeat_all
                        Player.REPEAT_MODE_ONE -> R.string.repeat_one
                        else -> R.string.repeat_off
                    },
                ),
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) inactive else active,
            )
        }
    }
}

/** The row at the bottom of Now Playing that opens the queue. */
@Composable
fun QueueHandle(onOpenQueue: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("queue-handle")
            .clickable(onClick = onOpenQueue)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_queue_music), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.queue), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
