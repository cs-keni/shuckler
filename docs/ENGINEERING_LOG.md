# Engineering Log

## 2026-05-14 (Claude Code — session 5)

- **Phase 5 (Download queue)**: replaced single `downloadingVideoUrl: String?` with `Set<String>` so multiple cards show loading indicators simultaneously. Added `Semaphore(2)` to `DownloadManager` to cap concurrent downloads at 2. Emit PENDING track + `DownloadProgress` entry immediately on enqueue so the `WaveformDownloadCard` appears with title/art right away; mark DOWNLOADING when semaphore is acquired. Fixed `completeDownload`/`failDownload` to replace (not append) existing pending entries. Fixed failed download card showing "DONE" instead of "FAILED". Renamed section label "Downloading" → "Download Queue". Commit: `cd94a60`.
- **Phase 6 (Stats)**: Added 5 new achievements (Night Owl, Marathon, Dedicated, Explorer, Mood Setter) with unlock logic in `AchievementManager`. Added `DonutChart` composable (Canvas-drawn ring split by top-5 artists, 900ms entry animation). Added listening streak chip (counts consecutive days with plays from `lastPlayedMs`, shows 🔥 flame + "X-day streak"). Commit: `8bb34e1`.
- **Phase 7.8 (Sleep timer chip)**: Upgraded from plain text row to an accent-colored pill chip with `Bedtime` icon, seconds countdown when under 1 minute, and a compact X dismiss button. Commit: `425ee55`.
- **Phase 8 (Downloaded badge)**: Added 16dp green circle + `Check` icon overlaid on `YouTubeResultItem` thumbnail top-right when track is already downloaded. Commit: `425ee55`.
- WSL Gradle still blocked (known). Build in Android Studio.

## 2026-05-14 (Claude Code — session 4)

- **Repo cleanup**: moved all docs/HTML previews/setup guides out of root into `docs/` subdirs. Deleted stale files (`catdoodle.png` duplicate, `log.txt`, `project.txt`). Commit: `00dc9a4`.
- **REDESIGN-v3.md**: full v3 planning doc created; 8 phases covering typography, bloom, Library reorg, downloads, stats, feel/micro-interactions, and playback polish. Commits: `a0b7985`, `bd580f7`.
- **Phase 1 (Quick wins)**: removed Preview button from SearchScreen (both `YouTubeResultItem` and `RecommendedSearchTile` + both call sites). Fixed Now Playing accent bloom corner 24dp→18dp. Fixed Library orange SwipeToDismissBox corner bleed with `clip(RoundedCornerShape(8.dp))`. De-bounced tab slide from `DampingRatioMediumBouncy` spring to `tween(280ms, FastOutSlowInEasing)`. Commit: `aee4d05`.
- **Phase 2 (Typography)**: replaced `DmSerifDisplay` with `PlusJakartaSans` (Normal/Medium/SemiBold/Bold) throughout `Type.kt`. Display=Bold, headline/titleLarge=SemiBold, smaller titles=Medium. Slight negative letterSpacing on display sizes. DM Mono unchanged. Fixed hardcoded `DmSerifDisplay` in `PlayerScreen.kt` lyrics view → `PlusJakartaSans Medium`. Commit: `81d58e1`.
- **Phase 3 (Ambient bloom)**: two-layer `drawBehind` in NavGraph — persistent Amber baseline (9% alpha, 1.1x radius) always present + album accent overlay (15% alpha, 0.9x radius). Same top-center origin for unified light source. App never goes to cold black without music. Commit: `cdb3827`.
- **Phase 4 (Library)**: renamed "Your Library" → "Downloads". Made "Albums" header tappable (ChevronRight icon, sets `BY_ALBUM` filter). Made "Playlists" header tappable (ChevronRight icon, opens `ModalBottomSheet` listing all playlists). `LibraryTrackItem` vertical padding 8dp→12dp. Commit: `c8c5eef`.
- **Phase 7.2 (Swipe up mini player)**: added `pointerInput` with `awaitEachGesture` to pill outer Box in `MiniPlayerBar.kt`. Swipe upward ≥36dp calls `onTap()` opening Now Playing sheet. Non-consuming (`requireUnconsumed = false`), coexists with tap. Haptic and progress bar already implemented in prior sessions.
- WSL Gradle still blocked (known). Build in Android Studio.

## 2026-05-14 (Claude Code — session 2)

- Implemented Phases 2–4 of the Ambient Color + Animation System. Commit: `740648f`.
- **`AccentExtensions.kt`** (new): five accent color helpers (`accentAmbient`, `accentWash`, `accentChipBg`, `accentChipBorder`, `accentAlbumGroup`) + `Modifier.pressScale()` extension. Press scale uses `awaitFirstDown(requireUnconsumed = false)` so it does not consume touch events and coexists cleanly with `Modifier.clickable`.
- **Library Album View**: `BY_ALBUM` added to `LibraryFilter`. "By Album" chip in filter row routes to a new `AlbumGroupedList` composable. Album headers use DM Serif Display titles, 28×28dp thumbnails, spring-animated chevrons for collapse. Playing album header: accent tint at 6% + accent title color. Track rows indented 40dp, no art (implied by album header). Search filters album groups and their tracks. Sort dropdown hidden in BY_ALBUM mode.
- **List Stagger Entrance**: `itemsIndexed` in flat `LazyColumn` and inside `AlbumGroupedList`. Items 0–4 stagger at 30ms intervals; items ≥5 appear instantly. `LaunchedEffect(track.id)` ensures animation only fires for new items, not scroll recompose.
- **Album Art Bloom**: `rememberInfiniteTransition` in `LibraryAlbumCard`. `bloomPulse` 0→1→0 at 2000ms. Bloom box scales 1.0→1.18 and alpha 0.35→0.80 via infinite pulse, using `RoundedCornerShape(18.dp)` fill. Only visible when `isPlaying = true`; transition still runs cheaply when not playing.
- **Download Spring Collapse**: `lingeringProgress` `SnapshotStateMap` in `SearchScreen` keeps cards alive after they leave the `progress` flow. `LaunchedEffect(isActive)` delays 1.5s then sets `cardVisible = false`, triggering `AnimatedVisibility` spring-shrink exit (`StiffnessLow` + `DampingRatioMediumBouncy`).
- `pressScale(0.92f)` applied to `FilterChip`; `pressScale(0.96f)` applied to `LibraryAlbumCard`.
- `showArt: Boolean = true` param added to `LibraryTrackItem`; album grouped rows pass `showArt = false`.
- WSL Gradle still blocked (known). Build in Android Studio.

## 2026-05-14 (Claude Code — session 3)

- Fixed `mutableStateSetOf` → `mutableStateOf(emptySet<String>())` after build failure; compat issue with project's Compose runtime version. Commit: `4e90696`.
- **Now Playing accent bloom**: added accent-colored `Box` behind the breathing glow ring in `PlayerScreen.kt`. Pulses with the same `glowScale`/`glowAlpha` infinite transition values. `albumColor.copy(alpha = glowAlpha * 0.9f)` fill. Rendered before blurred thumbnail glow for layered depth. Commit: `0a7a130`.
- **Lyrics transitions confirmed already implemented**: `PlayerScreen.kt` already had `animateFloatAsState` on both `targetAlpha` and `targetSizeSp` per lyric line, matching `DESIGN.md` spec exactly. No changes needed.
- **pressScale breadth**: applied to `PlaylistCard` (0.96f) and `AlbumGroupHeader` row (0.98f). All interactive surface elements in Library now have spring press feedback.
- Phases 1–4 complete. Design system v2 fully implemented.

## 2026-05-14

- Implemented ambient album color system across all screens. Commit: `033b408`.
- NavGraph now applies a radial gradient bloom (14% opacity) behind all content using `animatedAccent` from Palette API. Scaffold `containerColor` is transparent so bloom bleeds through.
- All screen outer columns/Scaffolds made transparent: HomeScreen, SearchScreen, AnalyticsScreen, LibraryScreen, PlaylistScreen. Removed opaque `background(Base)` blockers.
- Nav bar redesigned: Rounded icon set (Home, Search, LibraryMusic, BarChart), "Stats" label, dot-above-icon selected indicator (spring animated), accent-colored selected state via `NavigationBarItemDefaults.colors()`, no ripple indicator.
- MiniPlayerBar: added whole-pill spring press-scale (0.97f), accent-color border (22% opacity).
- Fixed duplicate `tween` import in NavGraph.kt; added missing `Color` import in PlaylistScreen.kt.
- DESIGN.md Phase 1 (Ambient Color System) is now fully implemented. Phases 2–4 remain for Codex or next Claude session.

## 2026-05-12

- `DownloadedTrack` now has optional album title/year fields. Existing downloads and normal YouTube downloads may still have null album data and fall back to artwork/title grouping.
- Gradle verification is blocked in the current WSL shell: no Linux Java is installed, and Windows Java/cmd interop fails with `UtilBindVsockAnyPort:307: socket failed 1`.
- Existing worktree was dirty before Codex edits, including navigation, player, analytics, settings, Last.fm work, and docs/prep files.
- `gstack` was requested but is not installed or not on PATH in the Codex WSL environment.
- Android Studio build succeeded on 2026-05-12 per user report; keep Codex-side Gradle limitation separate from project build health.
- Gradle catalog cleanup kept major-line dependency upgrades deferred: OkHttp `5.x` and reorderable `3.x` should be reviewed separately because API behavior may change.
- NewPipe Extractor must use the `v0.25.1` JitPack tag in the version catalog; `0.25.1` fails to resolve.
- Spotify playlist imports now preserve Spotify title/artist plus album title/year while still using the matched YouTube result for the source URL and artwork.
- YouTube download completion now stores the stable video URL in metadata while using the temporary stream URL only for the actual file transfer.
- `DownloadManager.suggestFileName()` requires `java.net.URL`; missing import surfaced in Android Studio run checks after the album metadata changes.
- Brand direction now avoids character-derived assets. User chose to keep Shuckler and use `catdoodle.png`; launcher, header, and onboarding use that drawable; old character image files were removed.
- Browser launch note: `xdg-open` is missing and Windows Explorer interop returned exit code 1, but `cmd.exe /c start` opened `brand-review.html`.

## 2026-05-13

- Checkpoint commit `dbcdc25` was pushed to `origin/main` before continuing new work, per user request.
- Search screen now uses the redesign token system instead of Material default surface colors. Result actions moved below the metadata row to reduce narrow-screen overflow risk.
- Codex WSL now has OpenJDK 17 and `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` persisted in `~/.zshrc`.
- Latest visual cleanup removed `CircleShape` from onboarding pager indicators and migrated hardcoded black/white overlay values in UI composables to existing warm tokens. `git diff --check` passes for the touched files.
- User reported the redesigned UI still feels gappy and too much like stacked boxes. Added a flow-first planning section to `DESIGN.md` and created `flow-redesign-preview.html`; no Compose implementation was changed for this planning pass.
- First flow-first implementation slice targets Home only. Existing behavior is preserved, but visual structure changed substantially.
- Library flow slice separates browsing from storage maintenance. The previous structure hid the track list inside the collapsed storage/download disclosure, which made the Library feel like a utility panel before a collection.
- Library sheet gap was likely caused by `heightIn(min = 500.dp)` applied only in `isSheetMode`; removed that forced minimum and let the bottom sheet partially expand.
- Library mid-page gap was caused by the `No playlists yet` empty state rendering before Albums even when album/track content existed. Removed that block from the main collection flow.
- Material default `FilterChip` styling made Library still look unchanged; replaced local Library chips with tokenized pill chips.
- Gotcha: after replacing the local `FilterChip` API, update all Library call sites. The mood-tag dialog still used Material's `label = { Text(mood) }` shape and caused a compile error at `LibraryScreen.kt:1613`; it now uses `label = mood`.
- Search idle recommendations are now intentionally shelf-like; keep full action cards for concrete search results where play/preview/download state needs room.
- Search felt empty because idle state depended on recommendations/recent searches. Added deterministic starter query chips so Search always has useful content on fresh installs.
- Search recommendation title/blank-gap bug came from rendering the section label whenever recommendation data existed, even if the fetch returned an empty list. Gate the whole section on `recommendedLoading || recommendedResults.isNotEmpty()`.
- Added "Keep exploring" chips under non-empty recommendations to prevent the Search idle surface from ending abruptly below the recommendation shelf.
- Stats still used old Material default chips/cards; first Stats flow pass replaced those with tokenized chips, inline accent panel, and flatter metric surfaces.
- Stats `DESIGN.md` target includes Top artists; added a ranked artist bar section using play count, falling back to track count when plays are zero.
- User reported Android Studio Gradle build succeeded after the latest redesign work.
- Codex-side Gradle now starts with `GRADLE_USER_HOME=.gradle`, but WSL cannot use the Windows Android SDK build tools because they contain `.exe` binaries. `ANDROID_HOME=/mnt/c/Users/nguye/AppData/Local/Android/Sdk ./gradlew :app:compileDebugKotlin` reaches SDK resolution, then fails on missing Linux `aapt` in build-tools `36.0.0`. A separate Linux Android SDK install would be needed for full WSL Gradle checks.
- Now Playing polish moved the queue sheet and playback controls off default Material surfaces/chips and onto Base, album accent, and warm text tokens. `PlayerScreen.kt` no longer has Material `FilterChip` usage.
- Settings polish grouped options with DM section headers and tokenized segmented choices while preserving existing settings behavior.
- Android Studio compile caught that this Material3 version does not expose `FilledIconButtonDefaults`; switched the play button colors to `IconButtonDefaults.filledIconButtonColors`.
- Search idle recommendations previously showed the section title while loading, then removed it if YouTube returned no recommendation results. The fix now keeps a separate visible recommendation shelf and only replaces it with non-empty fetches, so transient empty refreshes cannot make "Recommended for you" blink away.
- Download card polish added `DownloadStatus` labels to `WaveformDownloadCard` and tightened its shape/border to match the active-download exception in `DESIGN.md`; Search now passes the track status into the shared card.
- Library's storage/download disclosure now uses the warm tokenized utility style instead of default Material surface/text colors.

## 2026-05-14

- User asked to keep implementing `DESIGN.md` before doing a full visual review. Gap scan showed the core flow-first screens are close, but detail/utility surfaces still need consistency passes.
- Playlist Detail was still using Material cards/default colors for track rows and dialogs. It now uses Base, Surface, SurfaceElevated, Text1/Text2/Text3, Red, and LocalAccentColor directly, with flat track rows and rectangular artwork.
- `git diff --check -- app/src/main/java/com/shuckler/app/ui/PlaylistScreen.kt` passes. Codex-side Gradle remains blocked before Kotlin compilation by the Windows Android SDK build-tools lacking Linux `aapt`.
- Artist Detail, Album Detail, Onboarding, and Create were tokenized after Playlist Detail. These screens were structurally close to `DESIGN.md` but still had default Material surface/text/accent usage in secondary states.
- Re-attempting Codex-side Gradle after the detail/onboarding slice still fails before Kotlin compilation on the known Windows SDK `aapt` mismatch.
- Import dialog, Crop Cover, and Equalizer were tokenized next. `rg` now finds no targeted default Material surface/primary color usages in those files.
- A broad remaining-default scan still reports some expected function-name hits (`StatCard`, `ArtistAlbumCard`, local `FilterChip`, etc.) plus scattered real leftovers in Home, Library utility dialogs, EmptyState, MiniPlayer placeholder, and NavGraph.
- Final scattered sweep replaced those real leftovers. Broad `rg` now finds no targeted default Material surface/primary color usages in UI/navigation; only two direct `Card(` calls remain as intentional design exceptions in Search result cards and Home recommendation image tiles.

## 2026-05-14 (Claude Code — design planning session)

- User reported the app feels "too black" and empty despite the full token sweep. Diagnosed the root cause: `LocalAccentColor` was used only in Now Playing; every other screen was monochromatic warm-dark.
- Ran `/design-consultation` to plan a redesign. Result: the Ambient Color System (v2) and Animation System (v2) documented in `DESIGN.md`.
- Key concept: "Ambient Album Atmosphere" — the Palette API accent color bleeds through the whole app as a radial gradient bloom at 12–18% opacity, as if the album cover is casting light on the entire interface.
- New `AccentExtensions.kt` helpers documented (not yet implemented): `accentAmbient()`, `accentWash()`, `accentChipBg()`, `accentChipBorder()`, `accentAlbumGroup()`.
- New `AmbientBackground.kt` composable documented (not yet implemented): wraps each screen scaffold, applies animated ambient bloom that cross-fades over 600ms when the playing track changes.
- Six named animations documented in `DESIGN.md`: Ambient Transition, List Stagger Entrance, Tab Slide, Tab Press Feedback, Album Art Bloom, Download Spring Collapse.
- Library Album-First view documented: "By Album" filter chip, album-grouped section headers with thumbnail, playing album header tint, collapse chevron.
- Created `color-redesign-preview.html` — interactive before/after with live album color simulator.
- Updated global `~/.claude/CLAUDE.md`: added requirement to commit+push after every implementation slice, and to always read Codex docs at session start.
- No Compose source files were changed in this session — design planning only. Implementation is Codex's next task (see `CURRENT_TASK.md`).
