package io.github.aceattacker77.nakedmusicplayer.playback.eq

/** What the device's equalizer offers; bands and preset names come from the hardware at runtime. */
data class EqCapabilities(
    val bandCount: Int,
    val centerFreqsHz: List<Int>,
    val levelRangeMb: IntRange,
    val devicePresets: List<String>,
    val bassBoostSupported: Boolean,
)

/** The platform audio-effect calls the equalizer needs, behind an interface so logic is testable. */
interface AudioEffectsBackend {
    /** Binds effects to [audioSessionId]; null when the device cannot do equalization at all. */
    fun attach(audioSessionId: Int): EqCapabilities?

    fun setEnabled(enabled: Boolean)

    fun setBandLevels(levelsMb: List<Int>)

    /** [strength] is 0..1000. */
    fun setBassBoost(strength: Int)

    fun deviceBandLevels(presetIndex: Int): List<Int>

    fun release()
}
