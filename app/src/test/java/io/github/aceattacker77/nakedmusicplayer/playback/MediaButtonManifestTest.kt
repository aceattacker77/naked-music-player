package io.github.aceattacker77.nakedmusicplayer.playback

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Without a manifest-declared media button receiver Android cannot wake a dead app for a headset "play". */
@RunWith(RobolectricTestRunner::class)
class MediaButtonManifestTest {
    @Test fun mediaButtonBroadcast_isHandledByMedia3Receiver() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).setPackage(context.packageName)

        val receivers = context.packageManager.queryBroadcastReceivers(intent, 0).map { it.activityInfo.name }

        assertThat(receivers).contains("androidx.media3.session.MediaButtonReceiver")
    }
}
