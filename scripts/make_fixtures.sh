#!/usr/bin/env bash
# Generates 2 s 440 Hz sine fixtures in every supported format, plus a corrupt mp3.
# Requires ffmpeg on PATH. Output: app/src/androidTest/assets/fixtures/
set -euo pipefail
out="$(dirname "$0")/../app/src/androidTest/assets/fixtures"
mkdir -p "$out"
src="sine=frequency=440:duration=2"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a libmp3lame -b:a 64k "$out/tone.mp3"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a flac "$out/tone.flac"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a libvorbis -q:a 2 "$out/tone.ogg"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a libopus -b:a 48k "$out/tone.opus"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a aac -b:a 64k "$out/tone.m4a"
ffmpeg -y -loglevel error -f lavfi -i "$src" -c:a pcm_s16le "$out/tone.wav"
head -c 4096 /dev/urandom > "$out/corrupt.mp3"
ls -l "$out"
