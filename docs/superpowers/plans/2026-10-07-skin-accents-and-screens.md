# Skin accents and screens (Phase 3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a skin opt into status tags, title cards, panel headers with a glow, a square switch, a restyled Equalizer and skin picker, and a whitelisted bilingual strings pack, with every new field optional and defaulting to today's look.

**Architecture:** New nullable fields flow `SkinJson` → `SkinParser` → `Skin`. `AppTheme` extends `LocalOrnament` and adds `LocalSkinStrings` (both default to "off"/empty outside the theme). Composables read them, so screens take no new parameters and non-ornament skins render identically.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, kotlinx.serialization, JUnit/Truth/Robolectric, Roborazzi.

**Spec:** `docs/superpowers/specs/2026-10-07-skin-accents-and-screens-design.md` (builds on the merged Phase 1 and 2 specs).

Paths are under `app/src/main/java/io/github/aceattacker77/nakedmusicplayer/` (main) or `app/src/test/java/io/github/aceattacker77/nakedmusicplayer/` (test); string resources in `app/src/main/res/values/strings.xml`. Build env: `JAVA_HOME=/c/Users/Admin/tools/jdk/jdk-17.0.20.1+1`, `ANDROID_HOME=/c/Users/Admin/AppData/Local/Android/Sdk`; run `./gradlew` from `C:\Users\Admin\Android apps\music-player`. Work on a branch `skin-accents` off `master`, keep a ledger. Phone commands use the connected device serial from `adb devices` (the wireless address changes); the scratchpad `smoke.ps1` provides helpers.

## Global Constraints

- Skin `format` stays `1`; every new field is optional and its default reproduces today's look; a skin without them is pixel-identical (existing Roborazzi screenshots pass unchanged; never re-record existing ones, only the new `accents_*` goldens).
- Fields: `components.statusTags`, `titleCards`, `panelHeader`, `squareSwitch` booleans (false); `components.glow` `off`|`always`|`dark` (`off`); `strings` map of whitelisted key → `"English|Kana"`.
- String keys: `now_playing_status`, `now_playing_paused`, `unplayable_tag`, `eq_enabled_tag`, `skin_active_tag`, `library_kicker`, `albums_kicker`, `artists_kicker`, `playlists_kicker`, `eq_kicker`, `skins_kicker`, `queue_kana`, `sort_kana`, `import_kana`, `save_kana`. Limits: at most 20 entries (counted before whitelist filtering), 64 characters per value, English half non-empty, split at the first `|`, keys outside the whitelist ignored.
- Error text: `invalid value '<value>' for '<field>'` (e.g. `'components.glow'`); `strings has more than 20 entries`; `invalid value for 'strings.<key>': longer than 64 characters`; `invalid value '<value>' for 'strings.<key>'` for an empty English half. A wrong JSON type keeps `skin.json is not valid JSON`.
- Tag colour roles: GOOD (playing, enabled, active) `tertiary`, BAD (unplayable) `error`, PENDING `primary`, INFO `secondary`. The English word is always shown and is the accessible name; kana is hidden from accessibility; the existing `cant_play` string stays the accessible name of the unplayable tag.
- Geometry: lamp 8 dp circle; title heading about 44 sp (auto-shrinking toward 24 sp, never clipped at font scale 1.3); rule 1 dp; GeoSwitch track 52×28 dp, thumb 22 dp square, 1 dp border; Equalizer columns 12 cells, 3 dp gaps, 1 dp `outline` cell border, `secondary` fill; picker preview uses 14 cells; glow only on API 28 and later, and for `dark` only in a dark scheme.
- Equalizer: band count, level range and preamp range come from the device/controller (never hard-code five bands or ±12 dB); the existing `Slider`s stay for gestures and semantics and keep their test tags (`eq-switch`, `eq-band-N`, `eq-bass`, `eq-preamp`, `eq-preset-button`) and content descriptions.
- Touch targets stay at least 48 dp. Settings keeps its Material top bar. No warning banner. Do not push; the user approves pushes.
- TDD: each task's failing test is written and run first. Baseline suite is 423 tests. No real name anywhere (credit "Ace Attacker"); commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`.

## Review Focus

1. Strings pack limits and splitting: exactly 20 entries accepted and 21 rejected (counting whitelisted and unknown keys), a 64-character value accepted and 65 rejected, `"A|B|C"` splits at the first `|`, `"|x"` rejected, `"Word|"` has no kana (Task 1).
2. Equalizer cell mapping at the range ends, a narrow range, a range other than ±12 dB, and a zero-width range; the band sliders still expose description, progress and `setProgress` (Tasks 7, 8).
3. A skin with none of the new fields renders exactly as before, with and without `AppTheme` (Tasks 2–9); the unplayable icon remains for non-tag skins.
4. Title cards at font scale 1.3 with a long title; action buttons keep 48 dp targets; the Songs track count for 0, 1 and many (Tasks 5, 6).
5. GeoSwitch semantics (toggleable, switch role, state description) and glow gating (`dark` in light scheme, API below 28) (Tasks 2, 4, 7).

---

### Task 1: Schema, strings pack and parser

**Files:**
- Modify: `ui/skins/Skin.kt`, `ui/skins/SkinJson.kt`, `ui/skins/SkinParser.kt`
- Test: `ui/skins/SkinParserTest.kt`

**Interfaces:**
- Produces: `enum class GlowMode { OFF, ALWAYS, DARK }`; `data class SkinString(val english: String, val kana: String?)`; `internal val SKIN_STRING_KEYS: Set<String>` (the 15 keys); `Skin` gains, after `rowEdge`: `statusTags: Boolean = false`, `titleCards: Boolean = false`, `panelHeader: Boolean = false`, `squareSwitch: Boolean = false`, `glow: GlowMode = GlowMode.OFF`, `strings: Map<String, SkinString> = emptyMap()`. `ComponentsJson` gains `statusTags`, `titleCards`, `panelHeader`, `squareSwitch: Boolean?`, `glow: String?`; `SkinJson` gains `strings: Map<String, String>?`.

- [ ] **Step 1: Write failing tests in `SkinParserTest`:** `accentFields_defaultToToday`; `accentFields_areParsed` (all five component fields set, `glow` `"dark"`); `glow_unknownValue` → `invalid value 'sometimes' for 'components.glow'`; `strings_areParsedAndSplitAtTheFirstBar` (`"Playing|再生"`, `"A|B|C"` → english `A`, kana `B|C`, `"Word|"` → kana null, `"Plain"` → kana null); `strings_unknownKeysAreIgnored`; `strings_twentyEntriesAccepted_twentyOneRejected` (mix of whitelisted and unknown keys, message `strings has more than 20 entries`); `strings_valueLengthBoundary` (64 accepted, 65 → `invalid value for 'strings.library_kicker': longer than 64 characters`); `strings_emptyEnglishRejected` (`"|x"` and `"  |x"` → `invalid value '|x' for 'strings.library_kicker'`); `strings_inheritFromDefaults` (absent map keeps the defaults' entries; a present map replaces them); `strings_wrongTypeIsNotValidJson` (`"strings":"x"`).
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*SkinParserTest'`.** Expected: compile errors for the missing API (red).
- [ ] **Step 3: Implement** the types and fields, JSON fields and parsing in `SkinParser.build` (glow via `enumIndex` with `listOf("off","always","dark")`; strings validated as above, entries filtered to `SKIN_STRING_KEYS` after the count check).
- [ ] **Step 4: Re-run the same command.** Expected: PASS.
- [ ] **Step 5: Run `./gradlew :app:testDebugUnitTest`.** Expected: all pass. Commit `skins: parse accent fields and the strings pack`.

### Task 2: Ornament extension, skin strings and glow rule

**Files:**
- Modify: `ui/theme/Ornament.kt`, `ui/theme/AppTheme.kt`
- Create: `ui/theme/SkinStrings.kt`
- Test: `ui/theme/OrnamentTest.kt`, `ui/theme/SkinStringsTest.kt` (new)

**Interfaces:**
- Consumes: Task 1.
- Produces: `Ornament` gains `statusTags`, `titleCards`, `panelHeader`, `squareSwitch: Boolean` and `glow: GlowMode` (and `OFF` has them off); `ornamentOf` maps them; `val LocalSkinStrings = staticCompositionLocalOf<Map<String, SkinString>> { emptyMap() }`; `internal fun resolveSkinString(strings: Map<String, SkinString>, key: String, fallback: String): SkinString` (the entry, else `SkinString(fallback, null)`); `@Composable fun skinText(key: String, fallback: String): SkinString`; `fun glowActive(mode: GlowMode, isDark: Boolean): Boolean` (`OFF` false, `ALWAYS` true, `DARK` = isDark).

- [ ] **Step 1: Write failing tests:** update `OrnamentTest` (`off_isAllDefaults`, `ornamentOf_mapsEveryField` now include the five new fields); `SkinStringsTest`: `resolve_returnsTheSkinEntry`, `resolve_fallsBackToPlainEnglish`, `glowActive_truthTable` (all six combinations), and a Compose test that `AppTheme(skin with strings)` makes `skinText("library_kicker", "Library")` return the skin entry while outside `AppTheme` it returns the fallback.
- [ ] **Step 2: Run `--tests '*OrnamentTest' --tests '*SkinStringsTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement**; `AppTheme` provides `LocalSkinStrings provides skin.strings` and remembers the extended ornament on its new keys.
- [ ] **Step 4: Run the same command, then the full suite and `./gradlew :app:verifyRoborazziDebug`.** Expected: pass, no screenshot diff. Commit `theme: extend the ornament local and provide skin strings`.

### Task 3: Status tags

**Files:**
- Create: `ui/components/StatusTag.kt`
- Modify: `ui/components/SongRow.kt`, `ui/library/AlbumDetailScreen.kt`, `ui/player/QueueSheet.kt`, `ui/playlists/PlaylistDetailScreen.kt` (the four `ic_error_outline` sites), `ui/player/NowPlayingScreen.kt` (tag in the collapse row), `res/values/strings.xml` (`unplayable_tag` "Unplayable", `now_playing_playing` "Playing", `now_playing_paused` "Paused", `eq_enabled_tag` "Enabled", `skin_active_tag` "Active")
- Test: `ui/components/StatusTagTest.kt` (new), `ui/components/SongRowOrnamentTest.kt`

**Interfaces:**
- Consumes: Task 2's `LocalOrnament.statusTags`, `skinText`.
- Produces: `enum class StatusKind { GOOD, BAD, PENDING, INFO }`; `fun StatusKind.color(scheme: ColorScheme): Color`; `@Composable fun StatusTag(kind: StatusKind, word: String, kana: String?, modifier: Modifier = Modifier, accessibleName: String = word)`; `@Composable fun UnplayableMarker(modifier: Modifier = Modifier)` (the icon, or with `statusTags` the BAD tag whose accessible name is `stringResource(R.string.cant_play)`); `fun unplayableDecoration(unplayable: Boolean, statusTags: Boolean): TextDecoration?` (`LineThrough` only when both).

- [ ] **Step 1: Write failing tests:** `StatusKind.color` returns `tertiary`, `error`, `primary`, `secondary` for a known scheme; `unplayableDecoration` truth table; Compose: `StatusTag` shows its word in tracked caps text, exposes `accessibleName` as its content description and hides the kana (kana text has no accessible semantics); `SongRow` with `Ornament.OFF.copy(statusTags = true)` and `unplayable = true` shows a node whose content description is the `cant_play` string and no `ic_error_outline` icon node, and with `Ornament.OFF` still shows the icon (existing behaviour).
- [ ] **Step 2: Run `--tests '*StatusTagTest' --tests '*SongRowOrnamentTest'`.** Expected: compile failure / FAIL (red).
- [ ] **Step 3: Implement** `StatusTag` (lamp `CircleShape` 8 dp, label-style tracked word, optional 1 dp divider and kana, words via `skinText`), `UnplayableMarker`, `unplayableDecoration`; swap the four sites to `UnplayableMarker()` and pass the decoration to the title `Text`; add the Now Playing tag (`GOOD` "Playing"/kana from `now_playing_status`, `INFO` "Paused" from `now_playing_paused`) top right of the collapse row only when `statusTags`.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: status tags`.

### Task 4: Panel header and glow

**Files:**
- Modify: `ui/components/GeoPanel.kt`, `ui/player/MiniPlayer.kt`
- Test: `ui/components/GeoPanelTest.kt` (new)

**Interfaces:**
- Consumes: Task 2's `glowActive`, `LocalOrnament`.
- Produces: `GeoPanel(modifier, tone, title: String? = null, code: String? = null, live: Boolean = false, content)`; `fun Modifier.liveGlow(tone: Color, active: Boolean): Modifier` (a tone-tinted shadow, applied only when `active` and `Build.VERSION.SDK_INT >= 28`).

- [ ] **Step 1: Write failing tests:** with `title`/`code` given, `GeoPanel` shows both texts and a header rule node (test tag `geo-panel-rule`); without them it shows neither (existing look); `live = true` does not change semantics (content still reachable).
- [ ] **Step 2: Run `--tests '*GeoPanelTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** the header strip (title label-style `tone`, code `onSurfaceVariant`, 1 dp rule in `tone`) shown only when `title != null`, and `liveGlow`. In `MiniPlayer`'s bracketed branch pass `live = true` with `active = glowActive(LocalOrnament.current.glow, MaterialTheme.colorScheme.background.luminance() < 0.5f)`.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: panel header and live glow`.

### Task 5: Screen title

**Files:**
- Create: `ui/components/ScreenTitle.kt`
- Test: `ui/components/ScreenTitleTest.kt` (new)

**Interfaces:**
- Consumes: Task 1's `SkinString`.
- Produces: `internal fun kickerText(kicker: SkinString): String` (`"Library // 曲目"`, or just the English when no kana); `@Composable fun ScreenTitle(kicker: SkinString, title: String, modifier: Modifier = Modifier)` (kicker in label font, tracked caps, `primary`; title in the heading style at 44 sp with `TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = 44.sp)`, one line; a 1 dp rule beneath; kana hidden from accessibility).

- [ ] **Step 1: Write failing tests:** `kickerText` with and without kana; Compose at `Density(fontScale = 1.3f)` in a 411 dp width: `ScreenTitle` with titles `"Songs"` and `"Playlists"` and a 30-character title reports `hasVisualOverflow == false` on the title text; the kana segment is absent from the accessibility tree (only the English kicker has a text node).
- [ ] **Step 2: Run `--tests '*ScreenTitleTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run the same command, then the full suite.** Expected: pass. Commit `ui: screen title card`.

### Task 6: Title cards on the screens, Songs header and themed buttons

**Files:**
- Modify: `ui/AppScaffold.kt`, `ui/equalizer/EqualizerScreen.kt` (header only), `ui/settings/SkinPickerScreen.kt` (header only), `ui/library/SongsScreen.kt`, `res/values/strings.xml` (kicker strings `kicker_library` "Library", `kicker_albums` "Albums", `kicker_artists` "Artists", `kicker_playlists` "Playlists", `kicker_eq` "Audio", `kicker_skins` "Appearance")
- Create: `ui/components/ThemedButtons.kt`
- Test: `ui/components/ThemedButtonsTest.kt` (new), `ui/library/SongsHeaderTest.kt` (new); the header layouts themselves are covered by the Task 10 screenshots

**Interfaces:**
- Consumes: Tasks 2, 5.
- Produces: `@Composable fun themedButtonShape(): Shape` (`shapeOr(ButtonDefaults.shape, MaterialTheme.shapes.small)`). Songs reuses the existing `songs_count` plural for the track count.

- [ ] **Step 1: Write the probe and failing tests:** `SongsHeaderTest`: with `Ornament.OFF.copy(titleCards = true)` the Songs `SortBar` shows the count text for 0, 1 and 25 songs using the plural (`0 songs`-style, `1 song`, `25 songs` as the existing resource words them), and shows no count without `titleCards`; `ThemedButtonsTest`: `materialButtonsIgnoreTheChamfer` (under `AppTheme(chamfer skin)`, `ButtonDefaults.shape` is not a `ChamferShape`; this documents the premise and is expected to PASS, and if it FAILS then delete `themedButtonShape` and the explicit shapes from this plan and note a Ruling); `themedButtonShape_isChamferInAChamferSkin` and `_isMaterialDefaultOtherwise`.
- [ ] **Step 2: Run `--tests '*ThemedButtonsTest'`.** Expected: the two `themedButtonShape` tests fail to compile (red).
- [ ] **Step 3: Implement** `themedButtonShape`. In `AppScaffold`, when `LocalOrnament.current.titleCards && topLevel`, replace `LibraryTopBar` with a header: the search and overflow `IconButton`s right-aligned (48 dp targets, same menu), then `ScreenTitle` for the current tab (kicker from `skinText("library_kicker"/…, kicker string)`, title the tab label); without the flag keep `LibraryTopBar` untouched. In the Equalizer and Skins screens keep the `TopAppBar` (back arrow, empty title) and add `ScreenTitle` beneath when `titleCards`. In `SongsScreen`'s `SortBar`, when `titleCards`, show the track count (`pluralStringResource(R.plurals.songs_count, n, n)` in label caps) left and the sort control as an `OutlinedButton` with `themedButtonShape()` and the `sort_kana` accent, same menu and tag as today.
- [ ] **Step 4: Run `--tests '*ThemedButtonsTest'`, the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: title cards, songs header and themed buttons`.

### Task 7: Equalizer building blocks and GeoSwitch

**Files:**
- Create: `ui/equalizer/EqualizerGeometry.kt` (pure functions), `ui/components/GeoSwitch.kt`
- Test: `ui/equalizer/EqualizerGeometryTest.kt` (new), `ui/components/GeoSwitchTest.kt` (new)

**Interfaces:**
- Produces: `internal fun bandCellsFilled(levelMb: Int, range: IntRange, cells: Int): Int` (`round((level − min) / (max − min) × cells)` clamped to `0..cells`; 0 when the range is empty or zero-width); `internal fun readoutText(db: Float): String` (one decimal, trailing zero and sign kept: `-3.0`, `0.0`, `-0.5`; negative zero shows `0.0`); `internal fun bandsCode(count: Int): String` (`"%02d CH"`); `@Composable fun GeoSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write failing tests:** `bandCellsFilled`: min → 0, max → cells, mid of −1500..1500 → 6, −1200..1200 and −1500..1500 give the same fraction at the same relative position, a narrow range 0..100 works, out-of-range inputs clamp, empty/zero-width range → 0; `readoutText(-3f) == "-3.0"`, `(0f) == "0.0"`, `(-0f) == "0.0"`, `(-0.5f) == "-0.5"`, `(2.25f) == "2.3"` or `"2.2"` per `Locale.ROOT` HALF_UP (assert the actual rule); `bandsCode(5) == "05 CH"`, `(12) == "12 CH"`; Compose: `GeoSwitch` is toggleable with switch role (`assertIsToggleable`, `SemanticsProperties.Role == Role.Switch`), `performClick` calls `onCheckedChange(!checked)` once, and reports On/Off state; null callback disables toggling.
- [ ] **Step 2: Run `--tests '*EqualizerGeometryTest' --tests '*GeoSwitchTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** (`GeoSwitch` via `Modifier.toggleable(role = Role.Switch)` drawing the 52×28 dp square track with 1 dp `outline` border and a 22 dp square thumb, `tertiary` when on).
- [ ] **Step 4: Run the same command, then the full suite.** Expected: pass. Commit `ui: equalizer geometry helpers and the square switch`.

### Task 8: Equalizer restyle

**Files:**
- Modify: `ui/equalizer/EqualizerScreen.kt`
- Create: `ui/equalizer/SegmentedControls.kt` (segmented slider track, segmented meter row, `Readout`)
- Test: `ui/equalizer/EqualizerScreenTest.kt`

**Interfaces:**
- Consumes: Tasks 2, 3 (`StatusTag`), 4 (`GeoPanel`), 6 (`themedButtonShape`), 7.
- Produces: `@Composable fun Readout(label: String, value: String, unit: String, modifier: Modifier = Modifier)`; `VerticalSlider` gains `cells: Int? = null` (when non-null the slider's `track` slot draws that many cells from `bandCellsFilled`).

- [ ] **Step 1: Write failing tests in `EqualizerScreenTest`** (launch with `Skin.FALLBACK.copy(squareSwitch = true, panelHeader = true, segmentedMeters = true, statusTags = true, titleCards = true)`, which needs the `launch(skin = …)` parameter that already exists): `restyled_keepsEveryControlAndTag` (`eq-switch`, five `eq-band-N`, `eq-bass`, `eq-preamp`, `eq-preset-button` exist); `restyled_bandsPanelCodeComesFromTheDevice` (the code text is `"05 CH"` for the fake five-band device and `"03 CH"` when `capabilities` has three bands); `restyled_bandSliderKeepsItsSemantics` (content description `60 Hz`, a `ProgressBarRangeInfo`, and `performSemanticsAction(SetProgress)` changes the backend band); `restyled_switchTogglesTheEffect`; `restyled_enabledTagShowsWhenOn`; `restyled_preampReadoutKeepsTrailingZeros` (`-3.0`); `plainSkin_isUnchanged` (default skin shows `Switch`, no `GeoPanel` header).
- [ ] **Step 2: Run `--tests '*EqualizerScreenTest'`.** Expected: FAIL (red).
- [ ] **Step 3: Implement** per the spec: `GeoSwitch` and the `Enabled` `StatusTag` (`GOOD`, `skinText("eq_enabled_tag", …)`) when the matching flags are on; the Bands panel `GeoPanel(title = "Bands", code = bandsCode(caps.bandCount), tone = secondary)` when `panelHeader`; segmented columns (12 cells, value above in the label font, frequency below) and a segmented bass meter with `positionPercentText` when `segmentedMeters`; `Readout("Preamp", readoutText(state.preampDb), "dB")` over the real preamp `Slider` when `segmentedMeters`; preset and reset buttons use `themedButtonShape()` and Save is the one solid `Button` (accent `save_kana`). Every non-flag path stays exactly as today.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: restyled equalizer`.

### Task 9: Skin picker active card and import button

**Files:**
- Modify: `ui/settings/SkinPreviewCard.kt`, `ui/settings/SkinPickerScreen.kt`
- Test: `ui/settings/SkinPickerAccentsTest.kt` (new, in the style of `ui/settings/SettingsUiTest.kt`)

**Interfaces:**
- Consumes: Tasks 3, 4, 6.

- [ ] **Step 1: Write failing tests:** with a skin whose ornament has `statusTags` and `brackets`, the selected card shows the tag text `Active` (node under the card's test tag) and unselected cards do not; with a plain skin no tag exists; the import button still has its text/semantics (`R.string.skin_import`) and in the accents skin shows the `import_kana` accent text when the pack provides it.
- [ ] **Step 2: Run `--tests '*SkinPickerAccentsTest'`.** Expected: FAIL (red).
- [ ] **Step 3: Implement:** the preview shows the previewed skin with `seekSegments` capped at 14 (`skin.copy(player = skin.player.copy(seekSegments = 14))`); the selected card is wrapped in `GeoPanel(live = true)` with the `Active` `StatusTag` (`GOOD`, `skinText("skin_active_tag", …)`) beneath when the active skin's ornament has `statusTags`/`brackets`; unselected cards keep the 1 dp `outlineVariant` border; the import `TextButton` becomes an `OutlinedButton` with `themedButtonShape()` and the kana accent when `titleCards` is on. Plain skins render exactly as today.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: skin picker active card and import button`.

### Task 10: Screenshot coverage

**Files:**
- Create: `ui/equalizer/AccentsScreenshotTest.kt` (follow `ui/player/OrnamentScreenshotTest.kt` for the Roborazzi setup)
- Test: new goldens only, named `accents_*`

- [ ] **Step 1: Write the screenshot tests** for a skin with every Phase 1–3 field on, dark and light: the Equalizer (with the fake five-band controller as in `EqualizerScreenTest`), the skin picker with two skins (one selected), a title card header with a status tag row (Songs header with a playing row and an unplayable row), and Now Playing with the Playing tag. Each writes a distinct golden name.
- [ ] **Step 2: Run `./gradlew :app:verifyRoborazziDebug --tests '*AccentsScreenshotTest'`.** Expected: FAIL for missing goldens only.
- [ ] **Step 3: Record only the new goldens** with `recordRoborazziDebug --tests '*AccentsScreenshotTest'` and look at every image; fix any defect they reveal with a failing test first.
- [ ] **Step 4: Run `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`.** Expected: all pass and `git status` lists only new golden files. Commit `test: screenshots for the accent screens`.

### Task 11: Docs, Geofront sample and device check

**Files:**
- Modify: `docs/skins/FORMAT.md` (the `components` table, the `strings` section with keys and limits, the error table), `docs/design/skin-design-brief.md` (§3 screens, §5, §9 defaults, §13) and regenerate `C:\Users\Admin\Android apps\skin-design-brief.html` with the scratchpad `md2html.py`; `README.md` and `docs/FEATURES.md` test counts; `docs/skins/geofront/README.md` and `docs/skins/geofront/geofront.mskin`; `app/src/test/.../ui/skins/SampleSkinTest.kt`
- Modify (outside the repo): `C:\Users\Admin\Android apps\geofront.mskin` (identical copy)

- [ ] **Step 1: Update `SampleSkinTest`** to expect `statusTags`, `titleCards`, `panelHeader`, `squareSwitch` true, `glow` `DARK`, and the strings pack entries `now_playing_status` (`Playing`, `再生`), `unplayable_tag` (`Unplayable`, `否決`), `eq_enabled_tag` (`Enabled`, `稼働`), `skin_active_tag` (`Active`, `適用`), `library_kicker` (`Library`, `曲目`), `eq_kicker` (`Audio`, `音響`); run it. Expected: FAIL.
- [ ] **Step 2: Rebuild `geofront.mskin`** (version 1.4, `skin.json` first, three fonts and three licence files kept) with those fields and the brief's kana strings, to both locations; re-run `SampleSkinTest`. Expected: PASS.
- [ ] **Step 3: Update the docs** from the Global Constraints and the real test count; run `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`. Expected: all green.
- [ ] **Step 4: Build `:app:assembleBenchmarkRelease`, `adb install -r` it, push and import the skin** (Settings → Skins → Import skin → Replace). Check each tab's title card, Now Playing's tag, the Equalizer (drag a band, toggle the switch, move bass and preamp), the skin picker, in dark and light at font scale 1.0 and 1.3. Expected: tags with kana, title cards that do not clip, segmented columns that track the finger, a square switch that toggles, a bracketed Active card, no crash. Restore font scale 1.0, night mode `auto`, pause playback, delete the pushed file.
- [ ] **Step 5: Commit** `docs: describe skin accent fields and update the Geofront sample`. Do not push; ask first.
