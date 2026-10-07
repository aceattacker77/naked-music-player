package io.github.aceattacker77.nakedmusicplayer.ui

import androidx.compose.foundation.layout.WindowInsetsSides
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppScaffoldInsetsTest {
    @Test fun materialBar_keepsTheScaffoldDefaultInsets() {
        assertThat(scaffoldContentInsetSides(useGeoBar = false)).isNull()
    }

    @Test fun geoBar_leavesTheBottomInsetToTheBar() {
        // The custom bar pads the navigation-bar inset itself, so the scaffold body must not pad it again.
        assertThat(scaffoldContentInsetSides(useGeoBar = true))
            .isEqualTo(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    }

    @Test fun titleCardBar_padsTheTopAndBothSidesLikeTheMaterialBar() {
        assertThat(titleBarInsetSides()).isEqualTo(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    }
}
