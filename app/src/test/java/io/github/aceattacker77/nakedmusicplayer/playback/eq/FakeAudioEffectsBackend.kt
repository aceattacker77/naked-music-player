package io.github.aceattacker77.nakedmusicplayer.playback.eq

/** Records every call so tests can assert what reached the "hardware". */
class FakeAudioEffectsBackend(
    var capabilities: EqCapabilities? = DEFAULT_CAPS,
) : AudioEffectsBackend {
    val attachedSessions = mutableListOf<Int>()
    var appliedEnabled: Boolean? = null
    var appliedBands: List<Int>? = null
    var appliedBass: Int? = null
    var released = 0

    override fun attach(audioSessionId: Int): EqCapabilities? {
        attachedSessions += audioSessionId
        // A new attach starts from a fresh effect, like the real thing.
        appliedEnabled = null
        appliedBands = null
        appliedBass = null
        return capabilities
    }

    override fun setEnabled(enabled: Boolean) {
        appliedEnabled = enabled
    }

    override fun setBandLevels(levelsMb: List<Int>) {
        appliedBands = levelsMb
    }

    override fun setBassBoost(strength: Int) {
        appliedBass = strength
    }

    override fun deviceBandLevels(presetIndex: Int): List<Int> = PRESETS.getValue(presetIndex)

    override fun release() {
        released++
    }

    companion object {
        val DEFAULT_CAPS = EqCapabilities(
            bandCount = 5,
            centerFreqsHz = listOf(60, 230, 910, 3_600, 14_000),
            levelRangeMb = -1500..1500,
            devicePresets = listOf("Normal", "Rock", "Jazz"),
            bassBoostSupported = true,
        )
        val PRESETS = mapOf(
            0 to listOf(0, 0, 0, 0, 0),
            1 to listOf(300, 100, -100, 100, 300),
            2 to listOf(200, 0, 0, 100, 300),
        )
    }
}
