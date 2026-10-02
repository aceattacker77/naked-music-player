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
