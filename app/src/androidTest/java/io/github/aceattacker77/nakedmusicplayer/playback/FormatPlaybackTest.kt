package io.github.aceattacker77.nakedmusicplayer.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FormatPlaybackTest {
    // Fixtures live in the test APK's assets, so the player must be built with the test context.
    private val testContext = InstrumentationRegistry.getInstrumentation().context
    private val main = Handler(Looper.getMainLooper())
    private var player: ExoPlayer? = null

    private fun asset(name: String) = MediaItem.fromUri("asset:///fixtures/$name")

    private fun onMain(block: () -> Unit) {
        val latch = CountDownLatch(1)
        main.post { block(); latch.countDown() }
        check(latch.await(5, TimeUnit.SECONDS)) { "main thread did not run block" }
    }

    private fun newPlayer(items: List<MediaItem>, listener: Player.Listener, playWhenReady: Boolean = false) = onMain {
        player = ExoPlayer.Builder(testContext).build().also {
            it.addListener(listener)
            it.setMediaItems(items)
            it.playWhenReady = playWhenReady
            it.prepare()
        }
    }

    @After fun release() = onMain { player?.release(); player = null }

    @Test fun playsEachSupportedFormat() {
        listOf("mp3", "flac", "ogg", "opus", "m4a", "wav").forEach { ext ->
            val ready = CountDownLatch(1)
            var error: PlaybackException? = null
            newPlayer(
                listOf(asset("tone.$ext")),
                object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) ready.countDown()
                    }
                    override fun onPlayerError(e: PlaybackException) {
                        error = e; ready.countDown()
                    }
                },
            )
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue()
            assertThat(error).isNull()
            release()
        }
    }

    @Test fun gapless_transitionHasNoError() {
        val transitioned = CountDownLatch(1)
        var error: PlaybackException? = null
        newPlayer(
            listOf(asset("tone.mp3"), asset("tone.mp3")),
            object : Player.Listener {
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) transitioned.countDown()
                }
                override fun onPlayerError(e: PlaybackException) {
                    error = e; transitioned.countDown()
                }
            },
            playWhenReady = true,
        )
        assertThat(transitioned.await(10, TimeUnit.SECONDS)).isTrue()
        assertThat(error).isNull()
    }

    @Test fun corruptFile_raisesPlaybackError() {
        val failed = CountDownLatch(1)
        newPlayer(
            listOf(asset("corrupt.mp3")),
            object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) = failed.countDown()
            },
            playWhenReady = true,
        )
        assertThat(failed.await(5, TimeUnit.SECONDS)).isTrue()
    }
}
