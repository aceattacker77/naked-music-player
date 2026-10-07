package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinAccent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import io.github.aceattacker77.nakedmusicplayer.ui.player.segmentsFilled
import io.github.aceattacker77.nakedmusicplayer.ui.player.positionPercentText
import io.github.aceattacker77.nakedmusicplayer.ui.components.themedButtonShape
import io.github.aceattacker77.nakedmusicplayer.ui.components.StatusTag
import io.github.aceattacker77.nakedmusicplayer.ui.components.StatusKind
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoSwitch
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoPanel
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.material3.SliderState
import androidx.compose.material3.Button
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinText
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.ScreenTitle
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqCapabilities
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqState
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.PresetRef
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.playlists.PlaylistNameDialog
import java.util.Locale
import kotlin.math.roundToInt

private const val SLIDER_HEIGHT_DP = 180
private const val BAND_COLUMN_WIDTH_DP = 56
private const val MAX_BASS_BOOST = 1000f
private const val MIN_PREAMP_DB = -6f
private const val BAND_CELLS = 12
private const val METER_CELLS = 20

/** Centre frequency as a short number for the kHz label: 3600 -> "3.6", 14000 -> "14". */
fun kiloHertzText(hz: Int): String = String.format(Locale.ROOT, "%.1f", hz / 1000f).removeSuffix(".0")

/** Level in dB with an explicit sign: 300 mB -> "+3", -150 mB -> "-1.5". */
fun decibelText(levelMb: Int): String =
    String.format(Locale.ROOT, "%+.1f", levelMb / 100f).removeSuffix(".0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    controller: EqualizerController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val capabilities by controller.capabilities.collectAsStateWithLifecycle()
    val state by controller.state.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().testTag("equalizer-screen")) {
        val titleCards = LocalOrnament.current.titleCards
        TopAppBar(
            title = { if (!titleCards) Text(stringResource(R.string.equalizer)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        if (titleCards) {
            ScreenTitle(skinText("eq_kicker", stringResource(R.string.kicker_eq)), stringResource(R.string.equalizer))
        }
        val caps = capabilities
        if (caps == null) {
            EmptyState(
                title = stringResource(R.string.eq_unavailable_title),
                message = stringResource(R.string.eq_unavailable_message),
                action = null,
            )
        } else {
            EqualizerControls(caps, state, controller)
        }
    }
}

@Composable
private fun EqualizerControls(caps: EqCapabilities, state: EqState, controller: EqualizerController) {
    var savingPreset by remember { mutableStateOf(false) }
    val bands = List(caps.bandCount) { state.bandLevelsMb.getOrElse(it) { 0 } }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        val ornament = LocalOrnament.current
        ListItem(
            headlineContent = { Text(stringResource(R.string.equalizer)) },
            supportingContent = if (ornament.statusTags && state.enabled) {
                {
                    val tag = skinText("eq_enabled_tag", stringResource(R.string.eq_enabled_tag))
                    StatusTag(StatusKind.GOOD, tag.english, tag.kana)
                }
            } else {
                null
            },
            trailingContent = {
                if (ornament.squareSwitch) {
                    GeoSwitch(state.enabled, controller::setEnabled, Modifier.testTag("eq-switch"))
                } else {
                    Switch(
                        checked = state.enabled,
                        onCheckedChange = controller::setEnabled,
                        modifier = Modifier.testTag("eq-switch"),
                    )
                }
            },
            modifier = Modifier.clickable { controller.setEnabled(!state.enabled) },
        )
        PresetPicker(caps, state, controller)
        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        val bandsRow: @Composable () -> Unit = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                bands.forEachIndexed { index, level ->
                    BandColumn(
                        index = index,
                        level = level,
                        freqHz = caps.centerFreqsHz.getOrElse(index) { 0 },
                        range = caps.levelRangeMb,
                        cells = if (ornament.segmentedMeters) BAND_CELLS else null,
                        onLevel = { controller.setBand(index, it) },
                    )
                }
            }
        }
        if (ornament.panelHeader) {
            GeoPanel(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                tone = MaterialTheme.colorScheme.secondary,
                title = stringResource(R.string.eq_bands),
                code = bandsCode(caps.bandCount),
                content = bandsRow,
            )
        } else {
            bandsRow()
        }

        if (caps.bassBoostSupported) {
            SliderRow(
                label = stringResource(R.string.eq_bass_boost),
                valueText = "${(state.bassBoost / MAX_BASS_BOOST * 100).roundToInt()}%",
                value = state.bassBoost.toFloat(),
                range = 0f..MAX_BASS_BOOST,
                tag = "eq-bass",
                segmented = ornament.segmentedMeters,
                onValueChange = { controller.setBassBoost(it.roundToInt()) },
            )
        }
        val preampLabel = stringResource(R.string.eq_preamp)
        if (ornament.segmentedMeters) {
            Readout(
                label = preampLabel,
                value = readoutText(state.preampDb),
                unit = stringResource(R.string.eq_unit_db),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Slider(
                value = state.preampDb,
                onValueChange = controller::setPreamp,
                valueRange = MIN_PREAMP_DB..0f,
                modifier = Modifier.padding(horizontal = 16.dp).testTag("eq-preamp").semantics { contentDescription = preampLabel },
            )
        } else {
            SliderRow(
                label = preampLabel,
                valueText = stringResource(R.string.eq_db, String.format(Locale.ROOT, "%.1f", state.preampDb)),
                value = state.preampDb,
                range = MIN_PREAMP_DB..0f,
                tag = "eq-preamp",
                onValueChange = controller::setPreamp,
            )
        }

        val saveLabel = stringResource(R.string.eq_save_preset)
        val saveModifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        val buttonShape = if (ornament.accents) themedButtonShape() else ButtonDefaults.outlinedShape
        if (ornament.accents) {
            val kana = skinAccent("save_kana")
            Button(onClick = { savingPreset = true }, shape = buttonShape, modifier = saveModifier) {
                Text(saveLabel)
                if (kana != null) Text(" $kana", modifier = Modifier.clearAndSetSemantics {})
            }
        } else {
            OutlinedButton(onClick = { savingPreset = true }, shape = buttonShape, modifier = saveModifier) { Text(saveLabel) }
        }

        SystemEqualizerRow()
    }

    if (savingPreset) {
        PlaylistNameDialog(
            title = stringResource(R.string.eq_save_preset),
            confirmLabel = stringResource(R.string.save),
            initialName = "",
            label = stringResource(R.string.eq_preset_name),
            onConfirm = {
                controller.saveCustomPreset(it)
                savingPreset = false
            },
            onDismiss = { savingPreset = false },
        )
    }
}

@Composable
private fun PresetPicker(caps: EqCapabilities, state: EqState, controller: EqualizerController) {
    var open by remember { mutableStateOf(false) }
    val current = when (val preset = state.preset) {
        is PresetRef.Device -> caps.devicePresets.getOrNull(preset.index)
        is PresetRef.Custom -> preset.name
        null -> null
    } ?: stringResource(R.string.eq_preset_custom)

    Box(Modifier.padding(horizontal = 16.dp)) {
        val presetShape = if (LocalOrnament.current.accents) themedButtonShape() else ButtonDefaults.outlinedShape
        OutlinedButton(onClick = { open = true }, shape = presetShape, modifier = Modifier.testTag("eq-preset-button")) {
            Text("${stringResource(R.string.eq_preset)}: $current")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            caps.devicePresets.forEachIndexed { index, name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; controller.selectDevicePreset(index) })
            }
            state.customPresets.keys.sorted().forEach { name ->
                DropdownMenuItem(text = { Text(name) }, onClick = { open = false; controller.selectCustomPreset(name) })
            }
        }
    }
}

@Composable
private fun BandColumn(index: Int, level: Int, freqHz: Int, range: IntRange, cells: Int?, onLevel: (Int) -> Unit) {
    val frequency = if (freqHz < 1000) {
        stringResource(R.string.eq_freq_hz, freqHz)
    } else {
        stringResource(R.string.eq_freq_khz, kiloHertzText(freqHz))
    }
    Column(Modifier.width(BAND_COLUMN_WIDTH_DP.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.eq_db, decibelText(level)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSlider(
            value = level.toFloat(),
            range = range.first.toFloat()..range.last.toFloat(),
            onValueChange = { onLevel(it.roundToInt()) },
            description = frequency,
            cells = cells,
            modifier = Modifier.height(SLIDER_HEIGHT_DP.dp).testTag("eq-band-$index"),
        )
        // Tracked label fonts are wider than the column; shrink toward 8 sp on one line instead of wrapping or clipping.
        Text(
            text = frequency,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = MaterialTheme.typography.labelMedium.fontSize),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    tag: String,
    onValueChange: (Float) -> Unit,
    segmented: Boolean = false,
) {
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (segmented) skinLabel(label) else label,
                modifier = if (segmented) Modifier.originalLabel(label) else Modifier,
                style = if (segmented) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (segmented) positionPercentText(fraction) else valueText,
                style = if (segmented) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (segmented) {
            val fill = MaterialTheme.colorScheme.secondary
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.testTag(tag).semantics { contentDescription = label },
                thumb = { SquareThumb() },
                track = { SegmentedTrack(segmentsFilled(fraction, METER_CELLS), METER_CELLS, fill) },
            )
            return@Column
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.testTag(tag).semantics { contentDescription = label },
        )
    }
}

/** Opens the system's own audio-effects panel, only when the device has one. */
@Composable
private fun SystemEqualizerRow() {
    val context = LocalContext.current
    val intent = remember {
        Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
            .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    val available = remember { context.packageManager.resolveActivity(intent, 0) != null }
    if (available) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.eq_system_settings)) },
            modifier = Modifier.clickable { runCatching { context.startActivity(intent) } },
        )
    }
}

/** A slider that runs bottom (low) to top (high), for per-band gain. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerticalSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    cells: Int? = null,
) {
    val fill = MaterialTheme.colorScheme.secondary
    val segmented: (@Composable (SliderState) -> Unit)? = cells?.let { count ->
        { state ->
            val filled = bandCellsFilled(state.value.roundToInt(), state.valueRange.start.roundToInt()..state.valueRange.endInclusive.roundToInt(), count)
            SegmentedTrack(filled, count, fill)
        }
    }
    val sliderModifier = modifier
            .semantics { contentDescription = description }
            .graphicsLayer {
                rotationZ = 270f
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .layout { measurable, constraints ->
                // Measure as a horizontal slider as long as the available height, then lay it out turned.
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                    ),
                )
                layout(placeable.height, placeable.width) { placeable.place(-placeable.width, 0) }
            }
    if (segmented != null) {
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = sliderModifier, thumb = { SquareThumb() }, track = segmented)
    } else {
        Slider(value = value, onValueChange = onValueChange, valueRange = range, modifier = sliderModifier)
    }
}
