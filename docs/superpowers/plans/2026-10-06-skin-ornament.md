# Skin ornament (Phase 2) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a skin opt into hexagon "No artwork" tiles with borders, a segmented seek bar, corner brackets, a bracketed mini player, a chamfered-block bottom bar and a playing-row edge, with every new field optional and defaulting to today's look.

**Architecture:** New nullable fields flow `SkinJson` → `SkinParser` → `Skin`/`PlayerStyle`. `AppTheme` derives a `LocalOrnament` (default "all off" outside the theme, like `LocalLabelCaps`); composables read it, so screens take no new ornament parameters and non-ornament skins render identically.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 (adaptive navigation suite), kotlinx.serialization, JUnit/Truth/Robolectric, Roborazzi.

**Spec:** `docs/superpowers/specs/2026-10-06-skin-ornament-design.md` (builds on `2026-10-06-skin-shape-and-type-design.md`, already merged).

Paths are under `app/src/main/java/io/github/aceattacker77/nakedmusicplayer/` (main) or `app/src/test/java/io/github/aceattacker77/nakedmusicplayer/` (test). Build env: `JAVA_HOME=/c/Users/Admin/tools/jdk/jdk-17.0.20.1+1`, `ANDROID_HOME=/c/Users/Admin/AppData/Local/Android/Sdk`; run `./gradlew` from `C:\Users\Admin\Android apps\music-player`. Work on a branch `skin-ornament` off `master` and keep a ledger.

## Global Constraints

- Skin `format` stays `1`; every new field is optional and its default reproduces current behaviour; a skin without the fields is pixel-identical (existing Roborazzi screenshots pass unchanged; never re-record existing ones).
- Fields: `player.artPlaceholder` `note`|`hexagon` (default `note`); `player.artBorder` boolean (false); `player.seekBar` gains `segmented`; `player.seekSegments` integer 12–60 inclusive (40); `player.seekColor` `primary`|`tertiary` (`primary`); `components.brackets`, `components.segmentedMeters`, `components.rowEdge` booleans (false); `components.navStyle` `material`|`block` (`material`).
- Errors use the existing form `invalid value '<value>' for '<field>'` with the dotted path (e.g. `invalid value 'cube' for 'player.artPlaceholder'`, `invalid value '5' for 'player.seekSegments'`); a wrong JSON type keeps `skin.json is not valid JSON`.
- Geometry: segmented bar 16 dp tall, 3 dp gaps, 1 dp `outline` cell border, 32 dp touch area, no per-frame animation; brackets 14 dp long, 2 dp stroke, top-left and bottom-right only, drawn outside the content by half the stroke; mini player inset 12 dp with tonal elevation 0 when brackets are on; hexagons pointy-top, outer `outlineVariant`, middle `outline` (1 dp), inner `primary` (2 dp), note glyph `primary`; playing-row bar 2 dp `primary`; track code `TRK` plus the one-based queue index zero-padded to four digits (`TRK 0002`); position text one decimal with trailing zero kept (`31.3 %`).
- The chamfered bottom bar applies only at compact width (`NavigationSuiteType.NavigationBar`); medium and expanded widths keep Material's rail. Navigation keeps `popUpTo`/`launchSingleTop`/`restoreState` exactly.
- Decorative captions and ornaments are hidden from accessibility; touch targets stay at least 48 dp.
- TDD: each task's failing test is written and run first. Baseline suite is 371 tests. No real name anywhere (credit "Ace Attacker"); commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Do not push; the user approves pushes.

## Review Focus

1. `segmentsFilled` at 0, 1, partial and clamped inputs and at `seekSegments` 12 and 60; tap at the very start and end of the bar still seeks to 0 and to the full duration (Task 3).
2. Hexagon and bracket drawing at zero and tiny sizes (a 0 × 0 tile, a 4 dp tile) must not crash or produce NaN geometry (Tasks 4 and 5).
3. A skin with none of the new fields renders exactly as before, both with and without `AppTheme` (Tasks 2, 8).
4. `GeoNavigationBar` exposes selected-tab semantics, keeps a 48 dp touch target, is absent on non-top-level screens and on medium/expanded widths, and navigates identically (Task 7).
5. The track code for an empty queue (index -1) and a large queue, and captions hidden from TalkBack (Task 4).

---

### Task 1: Schema and parser (without `segmented`)

**Files:**
- Modify: `ui/skins/Skin.kt`, `ui/skins/SkinJson.kt`, `ui/skins/SkinParser.kt`
- Test: `ui/skins/SkinParserTest.kt` (helpers `ok(json)`, `error(json)`, `minimal(extra)`)

**Interfaces:**
- Produces: `enum class ArtPlaceholder { NOTE, HEXAGON }`, `enum class SeekColor { PRIMARY, TERTIARY }`, `enum class NavStyle { MATERIAL, BLOCK }`; `PlayerStyle` gains, after `controlShape`: `artPlaceholder: ArtPlaceholder = NOTE`, `artBorder: Boolean = false`, `seekSegments: Int = 40`, `seekColor: SeekColor = PRIMARY`; `Skin` gains, after `labelLetterSpacingEm`: `brackets: Boolean = false`, `segmentedMeters: Boolean = false`, `navStyle: NavStyle = NavStyle.MATERIAL`, `rowEdge: Boolean = false`. `SkinJson` gains `components: ComponentsJson?` (`brackets`, `segmentedMeters`, `navStyle: String?`, `rowEdge`); `PlayerJson` gains `artPlaceholder`, `artBorder`, `seekSegments: Int?`, `seekColor`.

- [ ] **Step 1: Write failing tests in `SkinParserTest`:** `ornamentFields_defaultToToday` (a `minimal()` skin: `NOTE`, false, 40, `PRIMARY`, and the four component fields false/`MATERIAL`); `ornamentFields_areParsed` (one JSON setting all nine, asserting each); `seekSegments_boundaries` (12 and 60 accepted); `ornamentErrors_haveExactMessages` (`artPlaceholder` `cube`, `seekColor` `blue`, `navStyle` `rail`, `seekSegments` 11 → `invalid value '11' for 'player.seekSegments'` and 61); `ornamentFields_inheritFromDefaults` (parse an empty skin against `Skin.FALLBACK.copy(brackets = true, navStyle = NavStyle.BLOCK)`); `wrongType_isNotValidJson` for `"seekSegments":"many"`.
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*SkinParserTest'`.** Expected: compile errors for the missing API (red).
- [ ] **Step 3: Implement** the enums and fields (defaults as above), the JSON fields, and parsing in `SkinParser.build` with `enumIndex`/`requireIn` like the Phase 1 fields; `seekSegments` range 12..60.
- [ ] **Step 4: Re-run the same command.** Expected: PASS.
- [ ] **Step 5: Run the full suite `./gradlew :app:testDebugUnitTest`.** Expected: all pass. Commit `skins: parse ornament fields`.

### Task 2: Ornament local

**Files:**
- Create: `ui/theme/Ornament.kt`
- Modify: `ui/theme/AppTheme.kt` (provide `LocalOrnament`)
- Test: `ui/theme/OrnamentTest.kt` (new)

**Interfaces:**
- Consumes: Task 1's skin fields.
- Produces: `data class Ornament(val artPlaceholder: ArtPlaceholder, val artBorder: Boolean, val brackets: Boolean, val segmentedMeters: Boolean, val navBlock: Boolean, val rowEdge: Boolean)` with `companion object { val OFF }`; `fun ornamentOf(skin: Skin): Ornament`; `val LocalOrnament = staticCompositionLocalOf { Ornament.OFF }`.

- [ ] **Step 1: Write failing tests:** `off_isAllDefaults` (`Ornament.OFF` equals `ornamentOf(Skin.FALLBACK)`); `ornamentOf_mapsEveryField` (a skin with hexagon, border, brackets, meters, `NavStyle.BLOCK`, rowEdge → matching `Ornament`, `navBlock` true).
- [ ] **Step 2: Run `--tests '*OrnamentTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `Ornament`, `ornamentOf` and the local; in `AppTheme` add `LocalOrnament provides ornamentOf(skin)` (remember it on the skin's ornament fields) to the existing `CompositionLocalProvider`.
- [ ] **Step 4: Run `--tests '*OrnamentTest'`, then the full suite and `./gradlew :app:verifyRoborazziDebug`.** Expected: pass, no screenshot diff. Commit `theme: provide an ornament local`.

### Task 3: Segmented seek bar

**Files:**
- Modify: `ui/skins/Skin.kt` (`SeekBarStyle.SEGMENTED` appended), `ui/skins/SkinParser.kt` (`SEEK_BARS` gains `"segmented"` last), `ui/player/SeekBars.kt`, `ui/player/NowPlayingScreen.kt` (the call that builds `SeekBarWithTimes`)
- Test: `ui/player/SeekBarGeometryTest.kt` (new), `ui/skins/SkinParserTest.kt`

**Interfaces:**
- Consumes: Task 1's `seekSegments`, `seekColor`; Task 2's `LocalOrnament.segmentedMeters`.
- Produces: `internal fun segmentsFilled(fraction: Float, segments: Int): Int` (floor of `fraction × segments`, clamped to `0..segments`); `internal fun positionPercentText(fraction: Float): String` (`"31.3 %"`, `Locale.ROOT`, one decimal, clamped 0–1); `SeekBar(..., segments: Int = 40, seekColor: SeekColor = SeekColor.PRIMARY)`; `SeekBarWithTimes(..., segments: Int = 40, seekColor: SeekColor = SeekColor.PRIMARY, showHeader: Boolean = false)`.

- [ ] **Step 1: Write failing tests:** `SeekBarGeometryTest`: `segmentsFilled(0f, 40) == 0`, `(1f, 40) == 40`, `(0.5f, 40) == 20`, `(0.3125f, 40) == 12`, `(-1f, 40) == 0`, `(2f, 40) == 40`, `(0.99f, 12) == 11`, `(1f, 60) == 60`; `positionPercentText(0.3125f) == "31.3 %"`, `(0f) == "0.0 %"`, `(1f) == "100.0 %"`, `(1.5f) == "100.0 %"`. In `SkinParserTest`: `seekBar_segmentedIsParsed`.
- [ ] **Step 2: Run `--tests '*SeekBarGeometryTest' --tests '*SkinParserTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** the two pure functions; `SEGMENTED` in the enum and parser list; a `SEGMENTED` branch in the `SeekBar` height `when` (32 dp touch area) and a `drawSegmented` canvas routine (16 dp bar, 3 dp gaps, 1 dp `outline` cell border, filled cells in the `seekColor` role colour, using `segmentsFilled`); no animation for it. `SeekBarWithTimes` shows the header row (`POSITION` left, `positionPercentText` right, label style, `onSurfaceVariant`) above the bar when `showHeader`. The `NowPlayingScreen` call passes `style.seekSegments`, `style.seekColor` and `showHeader = LocalOrnament.current.segmentedMeters`. Gesture and semantics code stays untouched.
- [ ] **Step 4: Run the same command.** Expected: PASS.
- [ ] **Step 5: Run the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `player: segmented seek bar`.

### Task 4: Hexagon tiles, borders and captions

**Files:**
- Create: `ui/components/HexagonPlaceholder.kt`
- Modify: `ui/components/AlbumArt.kt`, `ui/player/NowPlayingScreen.kt` (the `Artwork` composable at the `AlbumArt(...)` call), `res/values/strings.xml` and every `res/values-*/strings.xml` that exists (add `no_artwork` = "No artwork"; locales without a translation may rely on the default)
- Test: `ui/components/HexagonGeometryTest.kt` (new)

**Interfaces:**
- Consumes: Task 2's `LocalOrnament`.
- Produces: `internal fun hexagonPoints(size: Size, inset: Float): List<Offset>` (six vertices of a pointy-top hexagon centred in the box with radius `min(w,h)/2 − inset`, at angles −90°, −30°, 30°, 90°, 150°, 210°; radius clamped to ≥ 0); `internal fun trackCode(currentIndex: Int): String?` (`null` for index < 0, else `"TRK %04d".format(Locale.ROOT, index + 1)`); `data class ArtCaptions(val top: String, val bottom: String?)`; `AlbumArt(albumId, modifier, shape: Shape? = null, highlighted: Boolean = false, captions: ArtCaptions? = null)`.

- [ ] **Step 1: Write failing tests:** `hexagonPoints(Size(100f, 100f), 0f)` first vertex `Offset(50f, 0f)` and second `Offset(50f + 50f·cos(−30°), 25f)` within 0.01; six distinct vertices; `inset` larger than half the size gives all vertices at the centre without NaN; `Size.Zero` returns six `Offset(0f, 0f)`; `trackCode(1) == "TRK 0002"`, `(0) == "TRK 0001"`, `(-1) == null`, `(9998) == "TRK 9999"`, `(9999) == "TRK 10000"`.
- [ ] **Step 2: Run `--tests '*HexagonGeometryTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** the functions and `HexagonPlaceholder(modifier, captions)` (three nested hexagons on a `Canvas` per the Global Constraints colours and 1/1/2 dp strokes, the existing note icon centred in `primary`, captions drawn top-left and bottom-left in the label style and `onSurfaceVariant`, hidden from semantics). `AlbumArt` draws `HexagonPlaceholder` instead of the note tile when `LocalOrnament.current.artPlaceholder == HEXAGON`; the artwork image stays drawn over it, so captions vanish when art exists. When `artBorder`, draw a 1 dp border following the clip shape: `outline`, or `primary` when `highlighted`. `NowPlayingScreen`'s artwork passes `captions = ArtCaptions(stringResource(R.string.no_artwork), trackCode(state.currentIndex))` when the ornament asks for hexagons.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: hexagon tiles, artwork border and captions`.

### Task 5: Corner brackets, panel and mini player

**Files:**
- Create: `ui/components/CornerBrackets.kt` (the modifier and `bracketLines`), `ui/components/GeoPanel.kt`
- Modify: `ui/player/MiniPlayer.kt`, `ui/player/NowPlayingScreen.kt` (artwork gets brackets)
- Test: `ui/components/BracketGeometryTest.kt` (new)

**Interfaces:**
- Consumes: Task 2's `LocalOrnament.brackets`; Task 4's `AlbumArt(highlighted = …)`.
- Produces: `internal fun bracketLines(size: Size, length: Float, outset: Float): List<Pair<Offset, Offset>>` (four segments: top-left corner at `(−outset, −outset)` running right and down by `length`; bottom-right corner at `(w + outset, h + outset)` running left and up by `length`); `fun Modifier.cornerBrackets(color: Color, length: Dp = 14.dp, stroke: Dp = 2.dp): Modifier`; `@Composable fun GeoPanel(modifier: Modifier = Modifier, tone: Color = MaterialTheme.colorScheme.primary, content: @Composable () -> Unit)`.

- [ ] **Step 1: Write failing tests:** `bracketLines(Size(100f, 60f), 14f, 1f)` equals `[(−1,−1)→(13,−1), (−1,−1)→(−1,13), (101,61)→(87,61), (101,61)→(101,47)]`; zero-size input yields four segments with no NaN; `length` is clamped to at most half the shorter side.
- [ ] **Step 2: Run `--tests '*BracketGeometryTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `bracketLines` (clamping length), `cornerBrackets` via `drawWithContent`, and `GeoPanel` (1 dp `outline` border, `surface` fill, theme shape, brackets in `tone`). In `MiniPlayer`, when `LocalOrnament.current.brackets`: wrap the existing row in `GeoPanel` inset 12 dp from the screen edges, set tonal elevation to 0, give the artwork `highlighted = true`, make play/next plain ghost icon buttons, and replace the 4 dp progress indicator with a 2 dp continuous line; behaviour (expand on tap and upward fling, `testTag("mini-player")`) is unchanged. `NowPlayingScreen`'s artwork applies `cornerBrackets(primary)` when the ornament asks. Skins without the flag take the existing code path untouched.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: corner brackets, panel and bracketed mini player`.

### Task 6: Playing-row edge

**Files:**
- Modify: `ui/components/SongRow.kt`
- Test: `ui/components/SongRowOrnamentTest.kt` (new, Robolectric Compose rule in the style of existing UI tests such as `ui/player/PlayerUiTest.kt`)

**Interfaces:**
- Consumes: Task 2's `LocalOrnament.rowEdge`/`artBorder`; Task 4's `AlbumArt(highlighted)`.

- [ ] **Step 1: Write failing tests:** with `CompositionLocalProvider(LocalOrnament provides Ornament.OFF.copy(rowEdge = true))` and a current song, a node tagged `song-edge-<id>` exists; for a non-current song it does not; with `Ornament.OFF` it does not exist for a current song; durations still render in all cases.
- [ ] **Step 2: Run `--tests '*SongRowOrnamentTest'`.** Expected: FAIL (no edge node).
- [ ] **Step 3: Implement** in `SongRow`: when `rowEdge && isCurrent`, draw a 2 dp `primary` bar on the leading edge tagged `song-edge-${song.id}` and reduce the row's leading padding by 2 dp so alignment is unchanged; pass `highlighted = isCurrent && rowEdge` to `AlbumArt`; the duration uses `labelSmall` only when `rowEdge` is on.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: playing-row edge`.

### Task 7: Chamfered-block bottom bar

**Files:**
- Create: `ui/components/GeoNavigationBar.kt`
- Modify: `ui/AppScaffold.kt`
- Test: `ui/components/GeoNavigationBarTest.kt` (new, Robolectric Compose rule)

**Interfaces:**
- Consumes: Task 2's `LocalOrnament.navBlock`; Phase 1's `ChamferShape`/`MaterialTheme.shapes`, `skinLabel`.
- Produces: `data class GeoTab(val iconRes: Int, val label: String, val tag: String)`; `@Composable fun GeoNavigationBar(tabs: List<GeoTab>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier)`.

- [ ] **Step 1: Write failing tests:** four tabs render with their tags; the node at `selectedIndex` reports selected (`assertIsSelected`), the others `assertIsNotSelected`; each tab is at least 48 dp tall (`assertHeightIsAtLeast(48.dp)`); clicking tab 2 calls `onSelect(2)` exactly once; labels follow `skinLabel` (uppercase under a skin with label caps).
- [ ] **Step 2: Run `--tests '*GeoNavigationBarTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `GeoNavigationBar`: a row of four equal-weight items drawn with `selectable(role = Role.Tab)`; the selected item is a `MaterialTheme.shapes.medium` block filled with `primary` holding `onPrimary` icon and label, unselected items use `onSurfaceVariant`, labels are `labelSmall`-based at 12 sp through `skinLabel`; the bar draws on `surfaceContainerLow` and pads for `WindowInsets.navigationBars`. In `AppScaffold`, compute `val layout = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(...)` and `useGeo = topLevel && LocalOrnament.current.navBlock && layout == NavigationSuiteType.NavigationBar`; pass `if (useGeo) NavigationSuiteType.None else layout` to the scaffold and place `GeoNavigationBar` after `miniPlayer()` in the existing column; `onSelect` runs the same `nav.navigate(tab.route) { popUpTo(...) { saveState = true }; launchSingleTop = true; restoreState = true }` block as today (extract it so both bars share it). This resolves the spec's open point: the fallback path is used, no slot overload is needed.
- [ ] **Step 4: Run the same command, then the full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: chamfered-block bottom bar`.

### Task 8: Screenshot coverage for an ornament skin

**Files:**
- Create: `ui/player/OrnamentScreenshotTest.kt` (follow the pattern in `ui/player/NowPlayingScreenshotTest.kt` and `ui/settings/SkinPreviewScreenshotTest.kt` for Roborazzi setup)
- Test: new goldens only

**Interfaces:**
- Consumes: Tasks 1–7.

- [ ] **Step 1: Write the screenshot tests** for a skin with every ornament field on and `cornerStyle` chamfer, in dark and light: Songs (with a playing row), Now Playing without artwork (hexagon, captions, segmented bar with header, brackets), the mini player and `GeoNavigationBar` with the second tab selected. Each test writes a distinct golden name.
- [ ] **Step 2: Run `./gradlew :app:verifyRoborazziDebug --tests '*OrnamentScreenshotTest'`.** Expected: FAIL for missing goldens only (red for the right reason).
- [ ] **Step 3: Record only the new goldens:** `./gradlew :app:recordRoborazziDebug --tests '*OrnamentScreenshotTest'`; look at each image and confirm it shows the intended ornament; do not re-record any other test.
- [ ] **Step 4: Run `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`.** Expected: all pass, existing goldens unchanged (`git status` shows only new golden files). Commit `test: screenshots for an ornament skin`.

### Task 9: Docs, Geofront skin and device check

**Files:**
- Modify: `docs/skins/FORMAT.md` (new fields, defaults, error table), `docs/design/skin-design-brief.md` (§3 screens, §7, §9 defaults, §13) then regenerate `C:\Users\Admin\Android apps\skin-design-brief.html` with the scratchpad `md2html.py`; `README.md` and `docs/FEATURES.md` test counts; `docs/skins/geofront/README.md` (table rows) and `docs/skins/geofront/geofront.mskin`; `app/src/test/.../ui/skins/SampleSkinTest.kt` (assert the new fields)
- Modify (outside the repo): `C:\Users\Admin\Android apps\geofront.mskin` (identical copy)

**Interfaces:**
- Consumes: Tasks 1–8.

- [ ] **Step 1: Update `SampleSkinTest`** to expect `artPlaceholder` hexagon, `artBorder` true, `seekBar` segmented, `seekColor` tertiary, `brackets`, `segmentedMeters`, `rowEdge` true and `navStyle` block; run it. Expected: FAIL (the sample does not set them yet).
- [ ] **Step 2: Rebuild `geofront.mskin`** (version 1.3, `skin.json` first, the three fonts and the three `licenses/` files kept) with those fields; write it to both locations; re-run `SampleSkinTest`. Expected: PASS.
- [ ] **Step 3: Update the docs** from the Global Constraints and the real test count; run `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`. Expected: all green; record the count.
- [ ] **Step 4: Build `:app:assembleBenchmarkRelease`, `adb install -r` it, push the skin to `/sdcard/Download/`, import it (Settings → Skins → Import skin) using the scratchpad `smoke.ps1`.** Check Songs with a playing row, Now Playing without artwork, the mini player, the bottom bar and a tap and a drag on the seek bar, in dark and light at font scale 1.0 and 1.3. Expected: hexagon tiles with borders, segmented bar with header and green cells, brackets, the bracketed mini player, the chamfered selected tab, no crash. Afterwards restore font scale 1.0, night mode `auto`, pause playback and delete the pushed file.
- [ ] **Step 5: Commit** `docs: describe skin ornament fields and update the Geofront sample`. Do not push; ask first.
