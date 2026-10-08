# Releasing

How to produce a signed release and publish it. Nothing here is automated end to end: the signing key and the Google
Play account belong to the maintainer.

## 1. One-time: the upload keystore

Create it outside the repository and back it up in two places (a lost key is painful even with Play App Signing):

```bash
keytool -genkeypair -v -keystore naked-music-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Copy [`keystore.properties.example`](../keystore.properties.example) to `keystore.properties` in the repository root and
fill it in. The file and any `*.jks` or `*.keystore` are git-ignored. Without `keystore.properties` the release build
is left unsigned, so a fresh clone, the tests and CI build exactly as before.

## 2. Each release

1. Raise `versionCode` (always) and `versionName` in `app/build.gradle.kts`. Play rejects a `versionCode` it has seen.
2. Run the checks: `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`.
3. Build:

   ```bash
   ./gradlew :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab  (what Google Play takes)
   ./gradlew :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk     (GitHub releases, sideloading)
   ```

4. Check the APK: `scripts/check_release.sh` (size under 6 MB, no `INTERNET` permission) and
   `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` from the SDK build tools.
5. **Install the release build on a phone and use it.** R8 keep rules for the JSON that skins and presets use are not
   covered by unit tests. Exercise skin import and export, the equalizer, playlists, search and the widget.
6. Tag and publish the APK if wanted:

   ```bash
   git tag v0.1.0 && git push origin v0.1.0
   gh release create v0.1.0 app/build/outputs/apk/release/app-release.apk --title "0.1.0" --notes "..."
   ```

## 3. Google Play

These steps happen in the Play Console and need the maintainer's account. Limits and rules change, so check the Console.

1. Register a developer account (one-time fee, identity verification). An individual account shows the legal name
   publicly; an organisation account does not.
2. Create the app (free, "App") and enrol in Play App Signing when uploading the first bundle. Your keystore becomes
   the upload key.
3. Store listing: title (30 characters), short description (80), full description (4,000), a 512 by 512 icon, a
   1024 by 500 feature graphic and at least two phone screenshots. Ready-made screenshots are in
   [`play-store/`](play-store/) (1233 by 2400, opaque 24-bit PNG, longest side under twice the shortest).
4. App content: privacy policy URL (the app collects nothing and has no network access), Data safety (no data collected or
   shared), ads (none), content rating, target audience.
5. Permissions: a foreground-service declaration for media playback (Android 14 and later). `READ_MEDIA_AUDIO` is fine;
   the photo and video policy does not cover audio. Review `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and
   `RECEIVE_BOOT_COMPLETED`, which come from libraries, before answering the forms.
6. Upload the `.aab` to Internal testing first. Newer personal accounts have needed a closed test with at least 12 testers
   for 14 days before production access; check whether that applies.
7. Create the production release and send it for review.

## Refreshing the screenshots

```bash
./gradlew :app:recordRoborazziDebug --tests "*DocScreenshotsTest*" --tests "*PlayStoreScreenshotsTest*"
scripts/flatten_screenshots.sh     # drops the alpha channel from docs/play-store/
```

Both sets use the dark theme, the project's baseline.
