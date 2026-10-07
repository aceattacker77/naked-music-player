package io.github.aceattacker77.nakedmusicplayer.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProgressPercentTest {
    @Test fun percent_roundsDown_soANearlyFullBarNeverReadsComplete() {
        assertThat(progressPercent(0.999f)).isEqualTo(99)
        assertThat(progressPercent(0.5f)).isEqualTo(50)
        assertThat(progressPercent(1f)).isEqualTo(100)
    }

    @Test fun percent_clampsOutOfRangeAndNaN() {
        assertThat(progressPercent(-0.2f)).isEqualTo(0)
        assertThat(progressPercent(1.7f)).isEqualTo(100)
        assertThat(progressPercent(Float.NaN)).isEqualTo(0)
    }
}
