package io.github.aceattacker77.nakedmusicplayer.playback

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PlayTrackerTest {
    private val counted = mutableListOf<Long>()
    private val tracker = PlayTracker { counted += it }

    @Test fun countsOnceAtHalf() {
        tracker.onPlaying("song:1", 200_000, 0)
        tracker.onPaused(100_000)
        assertThat(counted).containsExactly(1L)

        tracker.onPlaying("song:1", 200_000, 200_000)
        tracker.onPaused(300_000)
        assertThat(counted).containsExactly(1L)
    }

    @Test fun belowHalf_doesNotCount() {
        tracker.onPlaying("song:1", 200_000, 0)
        tracker.onPaused(99_999)
        assertThat(counted).isEmpty()
    }

    @Test fun pausedTimeNotCounted() {
        tracker.onPlaying("song:1", 200_000, 0)
        tracker.onPaused(50_000)
        tracker.onPlaying("song:1", 200_000, 1_000_000)
        tracker.onPaused(1_040_000)
        assertThat(counted).isEmpty()

        tracker.onPlaying("song:1", 200_000, 2_000_000)
        tracker.onPaused(2_020_000)
        assertThat(counted).containsExactly(1L)
    }

    @Test fun newItemResetsAccumulation() {
        tracker.onPlaying("song:1", 200_000, 0)
        tracker.onItemChanged(80_000)
        tracker.onPlaying("song:2", 200_000, 80_000)
        tracker.onItemChanged(160_000)
        tracker.onPlaying("song:1", 200_000, 160_000)
        tracker.onItemChanged(240_000)
        assertThat(counted).isEmpty()
    }

    @Test fun itemChangeWhilePlaying_countsFinishedItem() {
        tracker.onPlaying("song:7", 200_000, 0)
        tracker.onItemChanged(200_000)
        assertThat(counted).containsExactly(7L)
    }

    @Test fun fourMinutesCountsForLongTrack() {
        tracker.onPlaying("song:1", 3_600_000, 0)
        tracker.onPaused(239_999)
        assertThat(counted).isEmpty()
        tracker.onPlaying("song:1", 3_600_000, 1_000_000)
        tracker.onPaused(1_000_001)
        assertThat(counted).containsExactly(1L)
    }

    @Test fun nonSongMediaId_isIgnored() {
        tracker.onPlaying("album:1", 200_000, 0)
        tracker.onPaused(200_000)
        assertThat(counted).isEmpty()
    }
}
