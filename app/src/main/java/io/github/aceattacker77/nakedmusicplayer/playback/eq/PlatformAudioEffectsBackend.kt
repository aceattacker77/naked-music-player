package io.github.aceattacker77.nakedmusicplayer.playback.eq

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

/** [AudioEffectsBackend] over the platform `Equalizer` and `BassBoost`; every failure means "unsupported". */
class PlatformAudioEffectsBackend : AudioEffectsBackend {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    override fun attach(audioSessionId: Int): EqCapabilities? {
        release()
        return try {
            val eq = Equalizer(0, audioSessionId)
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val bass = try {
                BassBoost(0, audioSessionId).takeIf { it.strengthSupported } ?: null
            } catch (e: RuntimeException) {
                null
            }
            equalizer = eq
            bassBoost = bass
            EqCapabilities(
                bandCount = bands,
                centerFreqsHz = List(bands) { eq.getCenterFreq(it.toShort()) / MILLIHERTZ_PER_HERTZ },
                levelRangeMb = range[0].toInt()..range[1].toInt(),
                devicePresets = List(eq.numberOfPresets.toInt()) { eq.getPresetName(it.toShort()) },
                bassBoostSupported = bass != null,
            )
        } catch (e: RuntimeException) {
            // Includes UnsupportedOperationException on devices without the effect.
            release()
            null
        }
    }

    override fun setEnabled(enabled: Boolean) {
        guarded {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        }
    }

    override fun setBandLevels(levelsMb: List<Int>) {
        guarded {
            val eq = equalizer ?: return@guarded
            levelsMb.forEachIndexed { band, level -> eq.setBandLevel(band.toShort(), level.toShort()) }
        }
    }

    override fun setBassBoost(strength: Int) {
        guarded { bassBoost?.setStrength(strength.toShort()) }
    }

    override fun deviceBandLevels(presetIndex: Int): List<Int> {
        val eq = equalizer ?: return emptyList()
        return try {
            eq.usePreset(presetIndex.toShort())
            List(eq.numberOfBands.toInt()) { eq.getBandLevel(it.toShort()).toInt() }
        } catch (e: RuntimeException) {
            emptyList()
        }
    }

    override fun release() {
        guarded { equalizer?.release() }
        guarded { bassBoost?.release() }
        equalizer = null
        bassBoost = null
    }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (e: RuntimeException) {
            // An effect that has been reclaimed by the system must not take playback down with it.
        }
    }

    private companion object {
        const val MILLIHERTZ_PER_HERTZ = 1000
    }
}
