package io.github.aceattacker77.nakedmusicplayer.ui.onboarding

import android.Manifest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PermissionsTest {
    @Test fun audioPermission_belowTiramisu_isReadExternalStorage() {
        assertThat(requiredAudioPermission(26)).isEqualTo(Manifest.permission.READ_EXTERNAL_STORAGE)
        assertThat(requiredAudioPermission(32)).isEqualTo(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    @Test fun audioPermission_tiramisuAndUp_isReadMediaAudio() {
        assertThat(requiredAudioPermission(33)).isEqualTo(Manifest.permission.READ_MEDIA_AUDIO)
        assertThat(requiredAudioPermission(36)).isEqualTo(Manifest.permission.READ_MEDIA_AUDIO)
    }

    @Test fun notificationPermission_onlyFromTiramisu() {
        assertThat(needsNotificationPermission(32)).isFalse()
        assertThat(needsNotificationPermission(33)).isTrue()
        assertThat(needsNotificationPermission(36)).isTrue()
    }
}
