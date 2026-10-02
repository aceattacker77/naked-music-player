package io.github.aceattacker77.nakedmusicplayer.playback.eq

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the equalizer: loads the saved [state], applies it to the [backend] whenever the audio
 * session changes or the user edits something, and saves every change. All operations are
 * serialized, so rapid slider moves cannot interleave with a session change.
 *
 * [setVolume] receives the preamp as a player volume (1.0 whenever the equalizer is off).
 */
class EqualizerController(
    private val backend: AudioEffectsBackend,
    private val repo: EqRepository,
    private val scope: CoroutineScope,
    private val setVolume: (Float) -> Unit,
) {
    private val _capabilities = MutableStateFlow<EqCapabilities?>(null)
    private val _state = MutableStateFlow(EqState())
    private val lock = Mutex()
    private var loaded = false
    private var attachedSession: Int? = null

    /** Null until attached, and for good on devices without equalizer support. */
    val capabilities: StateFlow<EqCapabilities?> = _capabilities

    val state: StateFlow<EqState> = _state

    init {
        scope.launch { lock.withLock { ensureLoaded() } }
    }

    fun onAudioSessionId(id: Int) {
        if (id <= 0) return
        scope.launch {
            lock.withLock {
                ensureLoaded()
                if (id == attachedSession) return@withLock
                attachedSession = id
                val caps = backend.attach(id)
                _capabilities.value = caps
                if (caps != null) _state.value = normalized(_state.value, caps)
                apply(_state.value)
            }
        }
    }

    fun setEnabled(enabled: Boolean) = mutate { state, _ -> state.copy(enabled = enabled) }

    fun selectDevicePreset(index: Int) = mutate { state, caps ->
        if (caps == null || index !in caps.devicePresets.indices) {
            state
        } else {
            state.copy(preset = PresetRef.Device(index), bandLevelsMb = backend.deviceBandLevels(index))
        }
    }

    fun selectCustomPreset(name: String) = mutate { state, _ ->
        val levels = state.customPresets[name]
        if (levels == null) state else state.copy(preset = PresetRef.Custom(name), bandLevelsMb = levels)
    }

    /** Editing a band means the levels no longer match any preset. */
    fun setBand(index: Int, levelMb: Int) = mutate { state, caps ->
        if (caps == null || index !in 0 until caps.bandCount) {
            state
        } else {
            val bands = bandsOf(state, caps).toMutableList()
            bands[index] = levelMb.coerceIn(caps.levelRangeMb)
            state.copy(preset = null, bandLevelsMb = bands)
        }
    }

    fun setBassBoost(strength: Int) = mutate { state, caps ->
        if (caps?.bassBoostSupported != true) state else state.copy(bassBoost = strength.coerceIn(0, MAX_BASS_BOOST))
    }

    fun setPreamp(db: Float) = mutate { state, _ -> state.copy(preampDb = db.coerceIn(MIN_PREAMP_DB, 0f)) }

    /** Stores the current band levels under [name] and selects that preset. */
    fun saveCustomPreset(name: String) = mutate { state, caps ->
        val clean = name.trim()
        if (clean.isEmpty() || caps == null) {
            state
        } else {
            val bands = bandsOf(state, caps)
            state.copy(preset = PresetRef.Custom(clean), bandLevelsMb = bands, customPresets = state.customPresets + (clean to bands))
        }
    }

    fun release() {
        scope.launch {
            lock.withLock {
                backend.release()
                _capabilities.value = null
                attachedSession = null
            }
        }
    }

    private fun mutate(change: (EqState, EqCapabilities?) -> EqState) {
        scope.launch {
            lock.withLock {
                ensureLoaded()
                val next = change(_state.value, _capabilities.value)
                _state.value = next
                apply(next)
                repo.update { next }
            }
        }
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        _state.value = repo.state.first()
        loaded = true
    }

    private fun apply(state: EqState) {
        setVolume(if (state.enabled) preampToVolume(state.preampDb) else 1f)
        val caps = _capabilities.value ?: return
        backend.setEnabled(state.enabled)
        backend.setBandLevels(bandsOf(state, caps))
        if (caps.bassBoostSupported) backend.setBassBoost(state.bassBoost)
    }

    /** Saved levels padded / trimmed to the device's band count and clamped to its range. */
    private fun bandsOf(state: EqState, caps: EqCapabilities): List<Int> =
        List(caps.bandCount) { i -> state.bandLevelsMb.getOrElse(i) { 0 }.coerceIn(caps.levelRangeMb) }

    private fun normalized(state: EqState, caps: EqCapabilities) = state.copy(bandLevelsMb = bandsOf(state, caps))

    private companion object {
        const val MAX_BASS_BOOST = 1000
        const val MIN_PREAMP_DB = -6f
    }
}
