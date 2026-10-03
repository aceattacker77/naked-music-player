# Skins are parsed from JSON by kotlinx.serialization; keep the generated serializers and the DTOs
# they describe so R8 cannot rename fields the JSON format depends on.
-keepclassmembers class io.github.aceattacker77.nakedmusicplayer.ui.skins.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.aceattacker77.nakedmusicplayer.ui.skins.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.aceattacker77.nakedmusicplayer.ui.skins.**$$serializer { *; }

# Equalizer settings are stored as JSON too (EqState, PresetRef).
-keepclassmembers class io.github.aceattacker77.nakedmusicplayer.playback.eq.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.aceattacker77.nakedmusicplayer.playback.eq.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.aceattacker77.nakedmusicplayer.playback.eq.**$$serializer { *; }

# Navigation routes are @Serializable and looked up by type.
-keepclassmembers class io.github.aceattacker77.nakedmusicplayer.ui.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.aceattacker77.nakedmusicplayer.ui.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.github.aceattacker77.nakedmusicplayer.ui.**$$serializer { *; }

# WorkManager (pulled in by the Glance widget) creates input mergers and workers by reflection.
# Without these, R8 strips their constructors and every widget render fails with
# "Could not create Input Merger", leaving the widget on its loading spinner.
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Glance builds ActionCallback classes (the widget buttons) by reflection from their class name.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }
