package io.github.aceattacker77.nakedmusicplayer.data.db

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayCountRuleTest {
    @Test fun halfOfShortTrack_counts() = assertThat(PlayCountRule.countsAsPlay(100_000, 200_000)).isTrue()
    @Test fun justUnderHalf_doesNotCount() = assertThat(PlayCountRule.countsAsPlay(99_999, 200_000)).isFalse()
    @Test fun fourMinutesOfLongTrack_counts() = assertThat(PlayCountRule.countsAsPlay(240_000, 3_600_000)).isTrue()
    @Test fun justUnderFourMinutes_doesNotCount() = assertThat(PlayCountRule.countsAsPlay(239_999, 3_600_000)).isFalse()
}
