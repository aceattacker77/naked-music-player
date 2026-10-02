plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "io.github.aceattacker77.nakedmusicplayer.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Baseline profile generation needs API 28+; the app itself still supports API 26.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

kotlin { jvmToolchain(17) }

baselineProfile {
    // Profiles are generated on a connected device or emulator.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.uiautomator)
}
