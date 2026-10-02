package io.github.aceattacker77.nakedmusicplayer.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Records the code paths of the critical user journeys so they are precompiled at install time. */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule val rule = BaselineProfileRule()

    @Test fun generate() = rule.collect(packageName = TARGET_PACKAGE, includeInStartupProfile = true) {
        grantMusicAccess()
        pressHome()
        launchToSongList()
        flingSongs(times = 2)
        openFirstAlbum()
        expandPlayer()
    }
}
