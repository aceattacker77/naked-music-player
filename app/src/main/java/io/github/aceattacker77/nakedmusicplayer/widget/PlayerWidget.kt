package io.github.aceattacker77.nakedmusicplayer.widget

import kotlin.math.roundToInt
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.player.segmentsFilled
import io.github.aceattacker77.nakedmusicplayer.ui.player.segmentCountFor
import androidx.glance.layout.height
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.Player
import io.github.aceattacker77.nakedmusicplayer.MainActivity
import io.github.aceattacker77.nakedmusicplayer.MusicApp
import io.github.aceattacker77.nakedmusicplayer.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private val SMALL = DpSize(250.dp, 50.dp) // 4 x 1
private val MEDIUM = DpSize(250.dp, 110.dp) // 4 x 2
private val TALL_THRESHOLD = 100.dp
private const val ART_PX = 192
private const val SEGMENT_BAR_HEIGHT_DP = 12

/** The home-screen widget: a 4x1 strip, resizable to a 4x2 card with progress, shuffle and repeat. */
class PlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as MusicApp).container
        // The real current settings, so the first frame is right (AppSettings() defaults dynamic colour on).
        val initialSettings = container.settingsRepository.settings.first()
        provideContent {
            // Glance keeps this session alive and recomposes it on every update, so everything the widget shows
            // is read here, inside the composition. Values captured before provideContent would go stale.
            val skin by container.skinManager.active.collectAsState()
            val settings by container.settingsRepository.settings.collectAsState(initial = initialSettings)
            val dynamic = widgetUsesDynamicColors(skin.colorMode, settings.dynamicColor, Build.VERSION.SDK_INT)
            val albumId = WidgetState.readFrom(currentState<Preferences>()).albumId
            val art by produceState<Bitmap?>(initialValue = null, albumId) {
                value = albumId?.let { loadArtwork(context, it) }
            }
            if (dynamic) {
                GlanceTheme { StatefulPlayerWidgetContent(art, skin.cornerRadiusDp.dp, widgetStyleOf(skin)) }
            } else {
                GlanceTheme(colors = widgetColorProviders(skin)) { StatefulPlayerWidgetContent(art, skin.cornerRadiusDp.dp, widgetStyleOf(skin)) }
            }
        }
    }
}

/** The widget body for the widget's current saved state (re-read on every recomposition). */
@Composable
internal fun StatefulPlayerWidgetContent(art: Bitmap?, cornerRadius: Dp = 16.dp, style: WidgetStyle = WidgetStyle.PLAIN) {
    PlayerWidgetContent(WidgetState.readFrom(currentState<Preferences>()), art, cornerRadius, style)
}

/** Small thumbnail of an album's art, or null when it has none. */
private suspend fun loadArtwork(context: Context, albumId: Long): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val album = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
            context.contentResolver.loadThumbnail(album, Size(ART_PX, ART_PX), null)
        } else {
            context.contentResolver.openInputStream(Uri.parse("content://media/external/audio/albumart/$albumId"))
                ?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()
}

@Composable
fun PlayerWidgetContent(state: WidgetState, art: Bitmap?, cornerRadius: Dp = 16.dp, style: WidgetStyle = WidgetStyle.PLAIN) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val open = actionStartActivity(Intent(context, MainActivity::class.java))

    if (!style.brackets) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(colors.widgetBackground)
                .cornerRadius(cornerRadius)
                .padding(8.dp)
                .clickable(open),
            verticalAlignment = Alignment.CenterVertically,
        ) { WidgetBody(state, art, style, inset = 16.dp) }
        return
    }

    // A 1 dp frame (an outline-coloured box with the surface box inside it) and corner brackets in the primary colour.
    // RemoteViews cannot clip to a path, so the silhouette stays a rectangle.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(colors.outline)
            .cornerRadius(cornerRadius)
            .padding(1.dp)
            .semantics { testTag = "widget-frame" },
    ) {
        Box(modifier = GlanceModifier.fillMaxSize().background(colors.widgetBackground).cornerRadius(cornerRadius)) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(8.dp).clickable(open),
                verticalAlignment = Alignment.CenterVertically,
            ) { WidgetBody(state, art, style, inset = 18.dp) }
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                Image(
                    provider = ImageProvider(R.drawable.widget_bracket_tl),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp).semantics { testTag = "widget-bracket-tl" },
                    colorFilter = ColorFilter.tint(colors.primary),
                )
            }
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Image(
                    provider = ImageProvider(R.drawable.widget_bracket_br),
                    contentDescription = null,
                    modifier = GlanceModifier.size(14.dp).semantics { testTag = "widget-bracket-br" },
                    colorFilter = ColorFilter.tint(colors.primary),
                )
            }
        }
    }
}

/** The widget's rows, shared by the plain and the framed container; [inset] is the horizontal space the container takes. */
@Composable
private fun WidgetBody(state: WidgetState, art: Bitmap?, style: WidgetStyle, inset: Dp) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val tall = LocalSize.current.height >= TALL_THRESHOLD

    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Artwork(art, style)
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = state.title ?: context.getString(R.string.app_name),
                maxLines = 1,
                style = TextStyle(color = colors.onSurface, fontWeight = FontWeight.Bold),
            )
            if (state.artist != null) {
                Text(text = state.artist, maxLines = 1, style = TextStyle(color = colors.onSurfaceVariant))
            }
        }
        Controls(state)
    }
    if (tall && state.hasTrack) {
        Spacer(GlanceModifier.size(6.dp))
        if (style.segmentedProgress) {
            SegmentedProgress(state.progress, style, widthDp = LocalSize.current.width - inset)
        } else {
            LinearProgressIndicator(
                progress = state.progress,
                modifier = GlanceModifier.fillMaxWidth().semantics { testTag = "widget-progress" },
                color = colors.primary,
                backgroundColor = colors.surfaceVariant,
            )
        }
        Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconAction(
                icon = R.drawable.ic_shuffle,
                description = context.getString(if (state.shuffle) R.string.shuffle_on else R.string.shuffle_off),
                tint = if (state.shuffle) colors.primary else colors.onSurfaceVariant,
                onClick = actionRunCallback<ShuffleAction>(),
            )
            Spacer(GlanceModifier.width(24.dp))
            IconAction(
                icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one else R.drawable.ic_repeat,
                description = context.getString(
                    when (state.repeatMode) {
                        Player.REPEAT_MODE_ALL -> R.string.repeat_all
                        Player.REPEAT_MODE_ONE -> R.string.repeat_one
                        else -> R.string.repeat_off
                    },
                ),
                tint = if (state.repeatMode == Player.REPEAT_MODE_OFF) colors.onSurfaceVariant else colors.primary,
                onClick = actionRunCallback<RepeatAction>(),
            )
        }
    }
}

/** A segmented progress bar drawn to a bitmap (RemoteViews cannot draw custom shapes); redrawn when the progress changes. */
@Composable
private fun SegmentedProgress(progress: Float, style: WidgetStyle, widthDp: Dp) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    val density = context.resources.displayMetrics.density
    val widthPx = (widthDp.value * density).toInt().coerceAtLeast(1)
    val heightPx = (SEGMENT_BAR_HEIGHT_DP * density).toInt()
    val gapPx = 3f * density
    val cells = segmentCountFor(style.segments, widthPx.toFloat(), gapPx, minCellPx = 4f * density)
    val filled = segmentsFilled(progress, cells)
    val fill = (if (style.seekColor == SeekColor.TERTIARY) colors.tertiary else colors.primary).getColor(context).toArgb()
    val border = colors.outline.getColor(context).toArgb()
    val bitmap = remember(widthPx, heightPx, cells, filled, fill, border) {
        renderSegmentedBar(widthPx, heightPx, filled, cells, fill, border, gapPx, strokePx = density)
    }
    Image(
        provider = ImageProvider(bitmap),
        contentDescription = context.getString(R.string.widget_progress, (progress * 100).roundToInt()),
        modifier = GlanceModifier.fillMaxWidth().height(SEGMENT_BAR_HEIGHT_DP.dp).semantics { testTag = "widget-segmented-progress" },
    )
}

/** The artwork tile: the cover, or a placeholder; with a skin border it gets a 1 dp outline, with a hexagon style a hexagon. */
@Composable
private fun Artwork(art: Bitmap?, style: WidgetStyle) {
    val colors = GlanceTheme.colors
    val tile: @Composable () -> Unit = {
        Box(
            modifier = GlanceModifier.fillMaxSize().background(colors.surfaceVariant).cornerRadius(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (art != null) {
                Image(
                    provider = ImageProvider(art),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(8.dp),
                )
            } else if (style.hexagonTile) {
                Image(
                    provider = ImageProvider(R.drawable.ic_hexagon_note),
                    contentDescription = null,
                    modifier = GlanceModifier.size(36.dp).semantics { testTag = "widget-hexagon" },
                    colorFilter = ColorFilter.tint(colors.primary),
                )
            } else {
                Image(
                    provider = ImageProvider(R.drawable.ic_music_note),
                    contentDescription = null,
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = ColorFilter.tint(colors.onSurfaceVariant),
                )
            }
        }
    }
    if (style.tileBorder) {
        Box(
            modifier = GlanceModifier.size(48.dp).background(colors.outline).padding(1.dp).semantics { testTag = "widget-tile-border" },
        ) { tile() }
    } else {
        Box(modifier = GlanceModifier.size(48.dp)) { tile() }
    }
}

@Composable
private fun Controls(state: WidgetState) {
    val context = LocalContext.current
    val colors = GlanceTheme.colors
    if (state.hasTrack) {
        IconAction(R.drawable.ic_skip_previous, context.getString(R.string.previous), colors.onSurface, actionRunCallback<PrevAction>())
    }
    IconAction(
        icon = if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow,
        description = context.getString(if (state.isPlaying) R.string.pause else R.string.play),
        tint = colors.primary,
        onClick = actionRunCallback<PlayPauseAction>(),
    )
    if (state.hasTrack) {
        IconAction(R.drawable.ic_skip_next, context.getString(R.string.next), colors.onSurface, actionRunCallback<NextAction>())
    }
}

@Composable
private fun IconAction(
    icon: Int,
    description: String,
    tint: androidx.glance.unit.ColorProvider,
    onClick: androidx.glance.action.Action,
) {
    Image(
        provider = ImageProvider(icon),
        contentDescription = description,
        modifier = GlanceModifier.size(40.dp).padding(8.dp).clickable(onClick),
        colorFilter = ColorFilter.tint(tint),
    )
}
