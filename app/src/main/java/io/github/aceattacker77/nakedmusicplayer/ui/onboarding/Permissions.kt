package io.github.aceattacker77.nakedmusicplayer.ui.onboarding

import android.Manifest
import android.os.Build

/** The permission that grants read access to the user's audio files on [sdkInt]. */
fun requiredAudioPermission(sdkInt: Int): String =
    if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/** Notifications (the playback notification) need a runtime permission from Android 13. */
fun needsNotificationPermission(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU
