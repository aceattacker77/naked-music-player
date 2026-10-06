# Skin shape and type language (Phase 1) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a skin opt into chamfered corners, squeezed headings, tracked uppercase labels and theme-shaped control buttons, with every new field optional and defaulting to today's behaviour.

**Architecture:** New nullable fields flow `SkinJson` → `SkinParser` → `Skin`/`PlayerStyle`. `AppTheme` builds `Shapes` and `Typography` from them. Hard-coded shapes and app-drawn labels read the theme, so a skin without the fields renders exactly as before.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, kotlinx.serialization, JUnit/Truth/Robolectric, Roborazzi.

**Spec:** `docs/superpowers/specs/2026-10-06-skin-shape-and-type-design.md`

All paths below are under `app/src/main/java/io/github/aceattacker77/nakedmusicplayer/` (main) or `app/src/test/java/io/github/aceattacker77/nakedmusicplayer/` (test). Build env: `JAVA_HOME=/c/Users/Admin/tools/jdk/jdk-17.0.20.1+1`, `ANDROID_HOME=/c/Users/Admin/AppData/Local/Android/Sdk`; run `./gradlew` from `C:\Users\Admin\Android apps\music-player`.

## Global Constraints

- Skin `format` stays `1`; every new field is optional and its default reproduces current behaviour.
- Field ranges (inclusive): `shapes.chamferDp` integer 1–32 (default 10); `typography.headingScaleX` 0.5–1.0 (default 1.0); `typography.labelLetterSpacingEm` 0–0.5 (default 0).
- Enumerations: `shapes.cornerStyle` `round`|`chamfer` (default `round`); `player.controlShape` `circle`|`theme` (default `circle`); `player.controls` gains `mixed`.
- Error text: `invalid value '<value>' for '<field>'` with the dotted JSON path, e.g. `invalid value 'smooth' for 'shapes.cornerStyle'`, `invalid value '0.2' for 'typography.headingScaleX'`, `invalid value '40' for 'shapes.chamferDp'`. A wrong JSON type keeps `skin.json is not valid JSON`.
- Chamfer cuts at `chamferDp` = D: extraSmall 0.3·D, small 0.5·D, medium 0.8·D, large 1.0·D, extraLarge 1.4·D (3, 5, 8, 10, 14 dp at D = 10). The top-right and bottom-left corners are cut; the cut is clamped to half the shorter side. `cornerRadiusDp` is ignored when `cornerStyle` is `chamfer`.
- `labelLetterSpacingEm` of 0 keeps Material's own label letter spacing (do not overwrite it with 0).
- Caps applies only to app-drawn labels: Songs sort label, bottom-bar tab labels, Queue label, seek-bar times, fast-scroller letters, Equalizer labels. Accessibility descriptions keep the original text. Material dialog/button text is not uppercased.
- Existing Roborazzi screenshots must pass unchanged (`./gradlew :app:verifyRoborazziDebug`); never re-record them.
- TDD: each task's failing test is written and run first. Baseline suite is 341 tests. No real name anywhere (credit "Ace Attacker"); commits end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Do not push; the user approves pushes.

## Review Focus

1. A skin with `cornerStyle: "chamfer"` and a non-zero `cornerRadiusDp` uses the chamfer and ignores the radius (Task 1, Task 2).
2. Boundary values: `headingScaleX` 0.5 and 1.0 accepted, 0.49 and 1.01 rejected; `chamferDp` 1 and 32 accepted, 0 and 33 rejected (Task 1).
3. A chamfer shape on a component smaller than twice the cut does not invert or self-intersect (Task 2).
4. `labelCaps` on Japanese and mixed text leaves CJK untouched and uppercases Latin, and an older skin without the fields still parses (Tasks 1 and 5).
5. A skin without the new fields yields a `Typography` equal to today's, in particular Material's label letter spacing survives (Task 4).

---

### Task 1: Schema and parser

**Files:**
- Modify: `ui/skins/Skin.kt`, `ui/skins/SkinJson.kt`, `ui/skins/SkinParser.kt`
- Test: `ui/skins/SkinParserTest.kt` (existing helpers `ok(json)`, `error(json)`, `minimal(extra)`)

**Interfaces:**
- Produces: `enum class CornerStyle { ROUND, CHAMFER }`, `enum class ControlShape { CIRCLE, THEME }`, `ControlsStyle.MIXED` (appended), `PlayerStyle.controlShape: ControlShape` (last param, default `CIRCLE`), and on `Skin`, after `labelFontPath`: `cornerStyle: CornerStyle = CornerStyle.ROUND`, `chamferDp: Int = 10`, `headingScaleX: Float = 1f`, `labelCaps: Boolean = false`, `labelLetterSpacingEm: Float = 0f`.

- [ ] **Step 1: Write failing tests in `SkinParserTest`:** `newFields_defaultToToday` (a `minimal()` skin has `ROUND`, 10, 1f, false, 0f, `CIRCLE`, controls `FILLED`); `newFields_areParsed` (one JSON setting `cornerStyle":"chamfer"`, `chamferDp":12`, `headingScaleX":0.8`, `labelCaps":true`, `labelLetterSpacingEm":0.14`, `controls":"mixed"`, `controlShape":"theme"` and asserting each field, with `cornerRadiusDp":28` also present to pin that parsing keeps it); `boundaries_areAccepted` (0.5, 1.0, 1 and 32, 0.0 and 0.5 spacing); `outOfRange_isError` with exact messages for `headingScaleX` 0.49 → `invalid value '0.49' for 'typography.headingScaleX'`, 1.01, `chamferDp` 0 → `invalid value '0' for 'shapes.chamferDp'`, 33, `labelLetterSpacingEm` 0.51 and -0.1; `unknownEnum_isError` for `cornerStyle` `smooth`, `controlShape` `square`; `wrongType_isNotValidJson` (`"headingScaleX":"wide"` → `skin.json is not valid JSON`); `newFields_inheritFromDefaults` (parse with `defaults = Skin.FALLBACK.copy(cornerStyle = CornerStyle.CHAMFER, labelCaps = true)` and an empty skin → inherited).
- [ ] **Step 2: Run `./gradlew :app:testDebugUnitTest --tests '*SkinParserTest'`.** Expected: compile errors for the missing API (red for the right reason).
- [ ] **Step 3: Implement.** Add the enums and fields in `Skin.kt` (defaults as in Interfaces). Add nullable fields to `SkinJson.kt`: `ShapesJson.cornerStyle: String?`, `chamferDp: Int?`; `TypographyJson.headingScaleX: Float?`, `labelCaps: Boolean?`, `labelLetterSpacingEm: Float?`; `PlayerJson.controlShape: String?`. In `SkinParser.build` read them with `?: defaults.…`, validate ranges with `SkinError("invalid value '$value' for '<path>'")`, add `"mixed"` to `CONTROLS` (after `iconOnly` so indices map to the enum) and a `CONTROL_SHAPES = listOf("circle", "theme")` list via `enumIndex`.
- [ ] **Step 4: Re-run the same command.** Expected: PASS.
- [ ] **Step 5: Run the full unit suite `./gradlew :app:testDebugUnitTest`.** Expected: all pass (341 + new). Commit `skins: parse chamfer, heading squeeze, label caps and control shape fields`.

### Task 2: ChamferShape and theme shapes

**Files:**
- Create: `ui/theme/ChamferShape.kt`
- Modify: `ui/theme/AppTheme.kt` (`shapesFor` becomes `internal fun shapesFor(skin: Skin): Shapes`)
- Test: `ui/theme/ChamferShapeTest.kt` (new)

**Interfaces:**
- Consumes: Task 1's `Skin.cornerStyle`, `Skin.chamferDp`, `Skin.cornerRadiusDp`.
- Produces: `data class ChamferShape(val cut: Dp) : Shape` (outline is `Outline.Generic`); `internal fun chamferPoints(size: Size, cut: Float): List<Offset>` (the polygon, clockwise from the top-left corner: `(0,0)`, `(w-c,0)`, `(w,c)`, `(w,h)`, `(c,h)`, `(0,h-c)` with `c = min(cut, min(w,h)/2)`); `internal fun chamferCutsDp(chamferDp: Int): List<Dp>` returning the five cuts in order extraSmall…extraLarge; `internal fun shapesFor(skin: Skin): Shapes`.

- [ ] **Step 1: Write failing tests:** `polygon_cutsTopRightAndBottomLeft` (`chamferPoints(Size(100f, 60f), 10f)` equals the six points above); `cut_isClampedToHalfShorterSide` (`Size(20f, 20f)`, cut 50 → c = 10; `Size(100f, 8f)`, cut 10 → c = 4; the polygon is never self-intersecting: x values within `0..w` and y within `0..h`); `cuts_scaleFromChamferDp` (10 → 3, 5, 8, 10, 14 dp; 20 → 6, 10, 16, 20, 28); `chamferSkin_usesChamferShapes` (`shapesFor(Skin.FALLBACK.copy(cornerStyle = CHAMFER, chamferDp = 10, cornerRadiusDp = 28))` has `ChamferShape(3.dp)` … `ChamferShape(14.dp)`); `roundSkin_isUnchanged` (`shapesFor(Skin.FALLBACK)` equals `Shapes` of `RoundedCornerShape(28·factor)` for the five sizes with factors 0.25, 0.5, 0.75, 1, 1.5, exactly as `AppTheme` builds them today).
- [ ] **Step 2: Run `--tests '*ChamferShapeTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `ChamferShape.createOutline` by building a `Path` from `chamferPoints` (convert `cut` with the `Density`), and `shapesFor(skin)` choosing chamfer or the existing radius factors. Change the `remember` key in `AppTheme` to `skin.cornerStyle, skin.chamferDp, skin.cornerRadiusDp`.
- [ ] **Step 4: Run `--tests '*ChamferShapeTest'`.** Expected: PASS.
- [ ] **Step 5: Run full suite plus `./gradlew :app:verifyRoborazziDebug`.** Expected: pass with no screenshot diff. Commit `theme: add ChamferShape and chamfer shape set`.

### Task 3: Hard-coded shapes follow the theme for chamfer skins

**Files:**
- Modify: `ui/theme/ChamferShape.kt` (add the helper), `ui/components/AlbumArt.kt`, `ui/library/AlbumDetailScreen.kt:142`, `ui/settings/SkinPreviewCard.kt:58`
- Test: `ui/theme/ChamferShapeTest.kt`

**Interfaces:**
- Consumes: `ChamferShape` from Task 2.
- Produces: `fun shapeOr(fallback: Shape, themed: Shape): Shape` returning `themed` when it is a `ChamferShape`, else `fallback`; `AlbumArt(albumId, modifier, shape: Shape? = null)` where null means "8 dp rounded, or the theme's small shape for a chamfer skin".

- [ ] **Step 1: Write failing tests:** `shapeOr_returnsThemedOnlyForChamfer` (chamfer themed → themed; `RoundedCornerShape(5.dp)` themed → the fallback).
- [ ] **Step 2: Run `--tests '*ChamferShapeTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `shapeOr`. In `AlbumArt` resolve `shape ?: shapeOr(RoundedCornerShape(8.dp), MaterialTheme.shapes.small)`; callers that pass an explicit shape (`NowPlayingScreen` artwork) are unchanged. `AlbumDetailScreen` passes `shapeOr(RoundedCornerShape(16.dp), MaterialTheme.shapes.large)`; `SkinPreviewCard` uses `shapeOr(RoundedCornerShape(16.dp), MaterialTheme.shapes.large)`. Artist avatars stay `CircleShape`. Reading the shape from `MaterialTheme` (not `LocalSkin`) keeps composables usable without a skin provider.
- [ ] **Step 4: Run `--tests '*ChamferShapeTest'`.** Expected: PASS.
- [ ] **Step 5: Run full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `ui: let artwork and preview shapes follow a chamfer theme`.

### Task 4: Heading squeeze and label letter spacing

**Files:**
- Modify: `ui/theme/AppTheme.kt` (`Typography.withFontFamilies`)
- Test: `ui/theme/TypographyMappingTest.kt` (existing)

**Interfaces:**
- Consumes: Task 1's `Skin.headingScaleX`, `Skin.labelLetterSpacingEm`.
- Produces: `internal fun Typography.withFontFamilies(fonts: SkinFonts, headingScaleX: Float = 1f, labelLetterSpacingEm: Float = 0f): Typography`.

- [ ] **Step 1: Write failing tests in `TypographyMappingTest`:** `headingScale_reachesOnlyHeadings` (`headingScaleX = 0.8f`: display/headline/title nine styles have `textGeometricTransform?.scaleX == 0.8f`; body and label styles are equal to the base styles); `labelSpacing_reachesOnlyLabels` (`0.14f` → the three label styles have `letterSpacing == 0.14.em`; title and body spacing unchanged); `defaults_leaveTypographyEqual` (`base.withFontFamilies(SkinFonts(null, null, null)) == base`, which also pins that Material's label letter spacing is not overwritten); `scaleOne_leavesGeometricTransformUntouched`.
- [ ] **Step 2: Run `--tests '*TypographyMappingTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** by extending the existing `with(family)` helper: apply `TextGeometricTransform(scaleX = headingScaleX)` to heading styles only when `headingScaleX != 1f`, and `letterSpacing = labelLetterSpacingEm.em` to label styles only when `labelLetterSpacingEm > 0f`. In `AppTheme` pass `skin.headingScaleX` and `skin.labelLetterSpacingEm` and add them to the `remember` keys.
- [ ] **Step 4: Run `--tests '*TypographyMappingTest'`.** Expected: PASS.
- [ ] **Step 5: Run full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `theme: squeeze headings and track labels from the skin`.

### Task 5: Uppercase app-drawn labels

**Files:**
- Create: `ui/theme/SkinLabel.kt`
- Modify: `ui/library/SongsScreen.kt:116`, `ui/AppScaffold.kt:78`, `ui/player/PlayerControls.kt:217`, `ui/player/SeekBars.kt:121-122`, `ui/components/FastScroller.kt:53-60`, `ui/equalizer/EqualizerScreen.kt:216-226`
- Test: `ui/theme/SkinLabelTest.kt` (new)

**Interfaces:**
- Consumes: Task 1's `Skin.labelCaps`, `LocalSkin`.
- Produces: `fun labelText(text: String, caps: Boolean): String` (uppercases with `Locale.ROOT` semantics when `caps`); `@Composable fun skinLabel(text: String): String` reading `LocalSkin.current.labelCaps`.

- [ ] **Step 1: Write failing tests:** `caps_uppercasesLatin` (`labelText("Songs", true) == "SONGS"`); `off_leavesTextAlone`; `caps_leavesJapaneseAlone` (`"流星City"` → `"流星CITY"`, `"プレイリスト"` unchanged); `caps_isLocaleIndependent` (`"title"` stays `"TITLE"` under a Turkish default locale).
- [ ] **Step 2: Run `--tests '*SkinLabelTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `labelText` with `uppercase()` and `skinLabel`. Wrap the displayed string at each listed site with `skinLabel(...)`; leave `contentDescription` and `description` arguments (e.g. the slider's) on the original string. Sites that can render without a skin provider in existing tests must keep working: if any test composes one of these without `AppTheme`, wrap that test's content in `AppTheme(Skin.FALLBACK, AppSettings()) { … }` rather than weakening `LocalSkin`.
- [ ] **Step 4: Run `--tests '*SkinLabelTest'` then the full suite.** Expected: PASS, including the existing UI tests.
- [ ] **Step 5: Run `verifyRoborazziDebug`.** Expected: no diffs. Commit `ui: uppercase app-drawn labels for skins that ask`.

### Task 6: Theme-shaped and mixed controls

**Files:**
- Modify: `ui/player/PlayerControls.kt`, `ui/player/NowPlayingScreen.kt:156`
- Test: `ui/player/ControlKindTest.kt` (new)

**Interfaces:**
- Consumes: Task 1's `ControlsStyle.MIXED`, `ControlShape`, `PlayerStyle.controlShape`.
- Produces: `internal enum class ControlKind { FILLED, TONAL, OUTLINED, ICON }`; `internal fun controlKind(style: ControlsStyle, primary: Boolean): ControlKind`; `internal fun controlShapeFor(controlShape: ControlShape, shapes: Shapes): Shape?` (`CIRCLE` → `null` meaning the Material default, `THEME` → `shapes.large`); `PlayerControls(..., shape: Shape? = null)` and `ControlButton(..., shape: Shape?)`.

- [ ] **Step 1: Write failing tests:** `controlKind_perStyle` — FILLED: primary → FILLED, side → TONAL; OUTLINED: both OUTLINED; ICON_ONLY: both ICON; MIXED: primary → FILLED, side → OUTLINED; `controlShape_circleIsDefault` (null) and `controlShape_themeIsLarge` (`Shapes().large` equality).
- [ ] **Step 2: Run `--tests '*ControlKindTest'`.** Expected: compile failure (red).
- [ ] **Step 3: Implement** `controlKind` and `controlShapeFor`, make `ControlButton` choose the button by `controlKind` and pass `shape` when non-null (each icon-button variant accepts a `shape` argument; omit it when null to keep the Material default). `NowPlayingScreen` passes `shape = controlShapeFor(skin.player.controlShape, MaterialTheme.shapes)`. `OutlinedIconButton` borders use the `outline` role (confirm the default border colour; if it uses a faded variant pass `BorderStroke(1.dp, MaterialTheme.colorScheme.outline)`). Sizes, glow and the 48 dp minimum touch target stay as they are.
- [ ] **Step 4: Run `--tests '*ControlKindTest'`.** Expected: PASS.
- [ ] **Step 5: Run full suite and `verifyRoborazziDebug`.** Expected: pass, no diffs. Commit `player: mixed controls and theme-shaped buttons`.

### Task 7: Docs, Geofront skin and device check

**Files:**
- Modify: `docs/skins/FORMAT.md` (fields, defaults, error table), `docs/design/skin-design-brief.md` (§2 can/cannot table, §5, §6, §7.2, §9 default `skin.json`, §13), `README.md` and `docs/FEATURES.md` (test count), then regenerate `C:\Users\Admin\Android apps\skin-design-brief.html` with the scratchpad `md2html.py`
- Modify (outside the repo): `C:\Users\Admin\Android apps\geofront.mskin` — set `shapes.cornerStyle` `chamfer`, `chamferDp` 10, `typography.headingScaleX` 0.8, `labelCaps` true, `labelLetterSpacingEm` 0.14, `player.controls` `mixed`, `controlShape` `theme`; bump `version` to `1.2`; repack with `skin.json` first and the three fonts unchanged.
- Test: parse the repacked skin with the real parser (a throwaway test or the existing archive reader test pattern) and confirm Valid.

**Interfaces:**
- Consumes: Tasks 1–6.

- [ ] **Step 1: Update the docs** to describe each new field, default, range and error message from Global Constraints; update the test count to the real number from the final run.
- [ ] **Step 2: Rebuild `geofront.mskin`**, check it parses, then run `./gradlew :app:testDebugUnitTest :app:verifyRoborazziDebug`. Expected: all green; record the test count.
- [ ] **Step 3: Build `:app:assembleBenchmarkRelease`, `adb install -r` it, push the skin to `/sdcard/Download/`, import it via Settings → Skins → Import skin → Replace.** Check Songs, Now Playing, mini player and queue in dark and light at font scale 1.0 and 1.3, using the scratchpad `smoke.ps1`; expect cut corners on buttons and cards, squeezed headings, spaced uppercase labels, a filled play button with outlined previous/next, and no crash. Restore font scale 1.0, night mode `auto` and pause playback afterwards.
- [ ] **Step 4: Commit** `docs: describe skin shape and type fields`. Do not push; ask first.
