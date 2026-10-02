package io.github.aceattacker77.nakedmusicplayer.playback.eq

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PresetRef {
    @Serializable
    @SerialName("device")
    data class Device(val index: Int) : PresetRef

    @Serializable
    @SerialName("custom")
    data class Custom(val name: String) : PresetRef
}

/** The user's equalizer settings; survives the equalizer being switched off and app restarts. */
@Serializable
data class EqState(
    val enabled: Boolean = false,
    val preset: PresetRef? = null,
    val bandLevelsMb: List<Int> = emptyList(),
    val bassBoost: Int = 0,
    val preampDb: Float = 0f,
    val customPresets: Map<String, List<Int>> = emptyMap(),
)
