#!/usr/bin/env bash
# Fills a connected device with 5,000 test songs under Music/Seed/ and triggers a media scan, so
# scrolling, memory and startup can be measured against a realistic library.
# Requires: adb (a device or emulator), ffmpeg, and one-off runtime of a few minutes.
set -euo pipefail

count="${1:-5000}"
here="$(cd "$(dirname "$0")" && pwd)"
tone="$here/../app/src/androidTest/assets/fixtures/tone.mp3"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

[ -f "$tone" ] || { echo "missing fixture $tone (run scripts/make_fixtures.sh)" >&2; exit 1; }

artists=(Ann Bob Cy Dee Eve Fay Gus Hal Ivy Jon)
echo "Generating $count files..."
for n in $(seq 1 "$count"); do
  artist="${artists[$((n % ${#artists[@]}))]}"
  album="Album $((n / 12))"
  # Re-tag a copy of the 2 s tone; -c copy keeps this fast (no re-encode).
  ffmpeg -y -loglevel error -i "$tone" -c copy \
    -metadata title="Track $n" -metadata artist="$artist" -metadata album="$album" \
    -metadata track="$((n % 12 + 1))" "$work/$n.mp3"
done

adb shell mkdir -p /sdcard/Music/Seed
echo "Pushing to the device..."
adb push "$work/." /sdcard/Music/Seed/ >/dev/null
# Songs are 2 s long, below the default 30 s minimum: lower the filter in Settings (Off) before measuring.
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Music/Seed >/dev/null || true
adb shell cmd media.scan --help >/dev/null 2>&1 || true
echo "Done. In the app: Settings > Library > Minimum song length > Off."
