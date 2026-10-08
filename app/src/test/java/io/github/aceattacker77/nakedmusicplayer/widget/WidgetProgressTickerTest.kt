package io.github.aceattacker77.nakedmusicplayer.widget

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetProgressTickerTest {
    @Test fun delay_isOneBarStepClampedToTwoToTenSeconds() {
        assertThat(liveProgressDelayMs(311_000)).isEqualTo(7_775L) // about one of 40 steps
        assertThat(liveProgressDelayMs(30_000)).isEqualTo(2_000L)   // a short track would otherwise tick faster than 2 s
        assertThat(liveProgressDelayMs(3_600_000)).isEqualTo(10_000L) // a long one would otherwise barely move
        assertThat(liveProgressDelayMs(0)).isEqualTo(5_000L)       // unknown duration
    }

    @Test fun ticks_whileEnabledAndPlaying() = runTest {
        var ticks = 0
        val ticker = WidgetProgressTicker(backgroundScope) { ticks++ }
        ticker.update(enabled = true, playing = true, durationMs = 80_000) // 2 s steps
        advanceTimeBy(6_100)
        assertThat(ticks).isEqualTo(3)
    }

    @Test fun stops_whenPausedOrDisabled() = runTest {
        var ticks = 0
        val ticker = WidgetProgressTicker(backgroundScope) { ticks++ }
        ticker.update(enabled = true, playing = true, durationMs = 80_000)
        advanceTimeBy(2_100)
        ticker.update(enabled = true, playing = false, durationMs = 80_000)
        advanceTimeBy(10_000)
        assertThat(ticks).isEqualTo(1)
        ticker.update(enabled = true, playing = true, durationMs = 80_000)
        advanceTimeBy(2_100)
        ticker.update(enabled = false, playing = true, durationMs = 80_000)
        advanceTimeBy(10_000)
        assertThat(ticks).isEqualTo(2)
    }

    @Test fun neverStartsWhenDisabled() = runTest {
        var ticks = 0
        WidgetProgressTicker(backgroundScope) { ticks++ }.update(enabled = false, playing = true, durationMs = 80_000)
        advanceTimeBy(30_000)
        assertThat(ticks).isEqualTo(0)
    }

    @Test fun repeatedUpdatesWithTheSameInputs_doNotRestartTheClock() = runTest {
        var ticks = 0
        val ticker = WidgetProgressTicker(backgroundScope) { ticks++ }
        ticker.update(enabled = true, playing = true, durationMs = 80_000)
        advanceTimeBy(1_500)
        ticker.update(enabled = true, playing = true, durationMs = 80_000) // an unrelated player event
        advanceTimeBy(700)
        assertThat(ticks).isEqualTo(1)
    }
}
