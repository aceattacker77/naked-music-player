#!/usr/bin/env bash
# Builds the release APK and fails if it is bigger than 6 MB or requests the INTERNET permission.
set -euo pipefail

cd "$(dirname "$0")/.."
limit=$((6 * 1024 * 1024))

./gradlew :app:assembleRelease

apk="$(ls app/build/outputs/apk/release/*.apk | head -1)"
size="$(stat -c%s "$apk")"
echo "APK: $apk ($size bytes, limit $limit)"
if [ "$size" -gt "$limit" ]; then
  echo "FAIL: APK is larger than 6 MB" >&2
  exit 1
fi

case "$apk" in
  *-unsigned.apk) echo "NOTE: the APK is unsigned (no keystore.properties); it cannot be installed or uploaded until it is signed" ;;
esac

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$LOCALAPPDATA/Android/Sdk}}"
aapt2="$(ls "$sdk"/build-tools/*/aapt2* 2>/dev/null | sort | tail -1)"
[ -n "$aapt2" ] || { echo "aapt2 not found under $sdk/build-tools" >&2; exit 1; }

permissions="$("$aapt2" dump permissions "$apk")"
echo "$permissions"
if echo "$permissions" | grep -q "android.permission.INTERNET"; then
  echo "FAIL: release APK requests INTERNET" >&2
  exit 1
fi
echo "OK: release APK is within budget and requests no network access"
