package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetFrameTest {
    @Test fun bracketInset_isZeroForASquareWidget() {
        assertThat(bracketInset(0.dp)).isEqualTo(0.dp)
    }

    @Test fun bracketInset_clearsTheRoundedCorner() {
        // A corner of radius r is clipped up to about 0.29 r along each edge, so brackets sit 0.3 r in.
        assertThat(bracketInset(16.dp).value).isWithin(0.001f).of(4.8f)
        assertThat(bracketInset(28.dp).value).isWithin(0.001f).of(8.4f)
    }

    @Test fun bracketInset_neverNegative() {
        assertThat(bracketInset((-4).dp)).isEqualTo(0.dp)
    }
}
