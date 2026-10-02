package io.github.aceattacker77.nakedmusicplayer.playback

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.playback.SkipPolicy.Action
import org.junit.Test

class SkipPolicyTest {
    @Test fun skipPolicy_firstTwoErrorsSkip_thirdStops() {
        val policy = SkipPolicy()
        assertThat(policy.onError()).isEqualTo(Action.SKIP)
        assertThat(policy.onError()).isEqualTo(Action.SKIP)
        assertThat(policy.onError()).isEqualTo(Action.STOP)
    }

    @Test fun skipPolicy_successResetsCount() {
        val policy = SkipPolicy()
        policy.onError()
        policy.onError()
        policy.onItemStartedSuccessfully()
        assertThat(policy.onError()).isEqualTo(Action.SKIP)
        assertThat(policy.onError()).isEqualTo(Action.SKIP)
        assertThat(policy.onError()).isEqualTo(Action.STOP)
    }

    @Test fun customLimit_isHonoured() {
        val policy = SkipPolicy(maxConsecutiveFailures = 1)
        assertThat(policy.onError()).isEqualTo(Action.STOP)
    }

    @Test fun afterStop_startsFresh() {
        val policy = SkipPolicy()
        policy.onError(); policy.onError(); policy.onError()
        assertThat(policy.onError()).isEqualTo(Action.SKIP)
    }
}
