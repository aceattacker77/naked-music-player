package io.github.aceattacker77.nakedmusicplayer.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

const val TARGET_PACKAGE = "io.github.aceattacker77.nakedmusicplayer"
private const val WAIT_MS = 10_000L

/** Audio permission is granted up front so every run starts on the library, not the rationale screen. */
fun MacrobenchmarkScope.grantMusicAccess() {
    device.executeShellCommand("pm grant $TARGET_PACKAGE android.permission.READ_MEDIA_AUDIO")
    device.executeShellCommand("pm grant $TARGET_PACKAGE android.permission.POST_NOTIFICATIONS")
}

/** Launches the app and waits for the song list (tag `song-list`) to be on screen. */
fun MacrobenchmarkScope.launchToSongList() {
    startActivityAndWait()
    device.wait(Until.hasObject(By.res("song-list")), WAIT_MS)
}

/** Flings the song list down and back up `times` times. */
fun MacrobenchmarkScope.flingSongs(times: Int) {
    val list = device.findObject(By.res("song-list")) ?: return
    list.setGestureMargin(device.displayWidth / 5)
    repeat(times) {
        list.fling(Direction.DOWN)
        device.waitForIdle()
        list.fling(Direction.UP)
        device.waitForIdle()
    }
}

/** Opens the first album from the Albums tab, then returns to the Songs tab. */
fun MacrobenchmarkScope.openFirstAlbum() {
    device.findObject(By.text("Albums"))?.click()
    device.wait(Until.hasObject(By.res("album-grid")), WAIT_MS)
    device.findObject(By.res(Pattern.compile("album-\\d+")))?.click()
    device.wait(Until.hasObject(By.res("album-detail")), WAIT_MS)
    device.pressBack()
    device.findObject(By.text("Songs"))?.click()
    device.wait(Until.hasObject(By.res("song-list")), WAIT_MS)
}

/** Plays the first song and expands then collapses the full-screen player. */
fun MacrobenchmarkScope.expandPlayer() {
    device.findObject(By.res(Pattern.compile("song-\\d+")))?.click()
    if (device.wait(Until.hasObject(By.res("mini-player")), WAIT_MS)) {
        device.findObject(By.res("mini-player")).click()
        device.wait(Until.hasObject(By.res("now-playing")), WAIT_MS)
        device.pressBack()
    }
}
