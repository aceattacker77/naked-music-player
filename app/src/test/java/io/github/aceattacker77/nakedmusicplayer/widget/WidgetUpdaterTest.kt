package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetUpdaterTest {
    private val base = WidgetState(
        title = "Song", artist = "Artist", albumId = 7, isPlaying = true, shuffle = false, repeatMode = 0, progress = 0.1f,
    )

    @Test fun firstState_updates() {
        assertThat(WidgetUpdater.shouldUpdate(null, base)).isTrue()
    }

    @Test fun progressOnly_noUpdate() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(progress = 0.9f))).isFalse()
    }

    @Test fun identicalState_noUpdate() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy())).isFalse()
    }

    @Test fun trackChange_updates() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(title = "Other"))).isTrue()
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(artist = "Else"))).isTrue()
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(albumId = 8))).isTrue()
    }

    @Test fun playPause_updates() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(isPlaying = false))).isTrue()
    }

    @Test fun shuffleRepeat_updates() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(shuffle = true))).isTrue()
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(repeatMode = 2))).isTrue()
    }

    @Test fun trackChangeWithProgress_stillUpdates() {
        assertThat(WidgetUpdater.shouldUpdate(base, base.copy(title = "Other", progress = 0f))).isTrue()
    }

    @Test fun state_roundTripsThroughPreferences() {
        val prefs = mutablePreferencesOf()
        base.writeTo(prefs)
        assertThat(WidgetState.readFrom(prefs)).isEqualTo(base)
    }

    @Test fun noTrack_roundTripsAsIdle() {
        val prefs = mutablePreferencesOf()
        WidgetState.IDLE.writeTo(prefs)
        assertThat(WidgetState.readFrom(prefs)).isEqualTo(WidgetState.IDLE)
        assertThat(WidgetState.readFrom(mutablePreferencesOf())).isEqualTo(WidgetState.IDLE)
    }

    @Test fun progressIsClampedToZeroOne() {
        val prefs = mutablePreferencesOf()
        base.copy(progress = 3f).writeTo(prefs)
        assertThat(WidgetState.readFrom(prefs).progress).isEqualTo(1f)
    }
}
