# Handoff

Date: 2026-05-13

## Changed

- Added `ArtistDetailScreen` with blurred artwork hero, artist title, Songs list, Albums shelf, Play All, and Shuffle.
- Wired Library track artist text to open the artist page.
- Artist page playback uses the existing `QueueItem` / `PlayerViewModel.playTrackWithQueue` path.
- Added shared AI task docs so future Codex/Claude sessions have a current handoff anchor.
- Moved remaining hardcoded Gradle library coordinates into `gradle/libs.versions.toml`.
- Bumped `desugar_jdk_libs` from `2.1.4` to `2.1.5`; left OkHttp `4.12.0`, Coil `2.5.0`, and reorderable `2.4.0` stable for now.
- Rephrased the `gitignored` comment in `app/build.gradle.kts` to avoid the IDE typo warning.
- Fixed NewPipe Extractor catalog version back to the JitPack tag form `v0.25.1`.
- Added optional `albumTitle` and `albumYear` to `DownloadedTrack`, metadata persistence, retry/chapter propagation, and Spotify import propagation.
- Added `AlbumDetailScreen` with blurred artwork hero, album metadata, Play All, Shuffle, and numbered track rows.
- Added a Library Albums shelf and changed Artist page album cards to open Album Detail instead of playing immediately.
- Preserved stable YouTube video URLs as `DownloadedTrack.sourceUrl` for YouTube downloads instead of storing temporary audio stream URLs.
- Fixed `DownloadManager.kt` run-blocker by restoring `java.net.URL` import and correcting `loadMetadata()` indentation.
- Added user-created `catdoodle.png` as the app mark and replaced launcher/header/onboarding usage of the old character image.
- User chose Direction A from `brand-review.html`: keep the Shuckler name, use the cat doodle icon direction.
- Removed old character image files: `app/src/main/res/drawable/ic_shuckle.png`, `shuckle.png`, and `shuckle.svg`.
- Added `brand-review.html` with brand/name directions for user review.
- Added brand guidance to `DESIGN.md`.
- Updated Home screen visual language: warm Base background, library snapshot stats, rounded hero/empty hero, and quieter section headers.
- Updated Search screen visual language: warm Base background, tokenized search field, recent/suggestion chips, section labels, redesigned result cards, rectangular art, and a separate horizontally scrollable action row to reduce narrow-screen overflow.
- Continued design-system cleanup by removing remaining `CircleShape` usage from onboarding pager dots and replacing hardcoded black/white overlay colors in Home recommendations, Library grid favorite overlay, Player visualizer default, crop cover scrim, and Equalizer curve dots with warm design tokens.
- Added a new `DESIGN.md` section, "Flow-First Redesign Direction", based on user feedback that Home still feels gappy and too box/card-heavy.
- Added `flow-redesign-preview.html` as a planning-only preview artifact and opened it in the browser.
- User approved the flow-first direction for implementation.
- Implemented the first Home flow slice:
  - Replaced the old `ScreenHeader` + greeting + boxed snapshot + separate hero stack with a compact Shuckler topbar, full-bleed continue-listening/search band, inline metrics, chip actions, and artwork-led shelves.
  - Converted Home recent-search cards to tokenized chips.
  - Converted Home playlist/track shortcut cards to flat artwork tiles with labels.
  - Preserved existing playback, preview, download, recommendation, and playlist navigation behavior.
- Continued the flow-first implementation into Library:
  - Decoupled the track list from the collapsed storage/download maintenance section so browsing is visible by default.
  - Moved storage/download maintenance behind a lower "Manage storage & downloads" disclosure.
  - Flattened album and playlist shelves from card surfaces into artwork tiles with labels.
  - Flattened track rows with a subtle now-playing accent wash instead of card containers.
  - Set the Library scaffold background to `Base`.
- Fixed the reported Library bottom-sheet gap by removing the forced `heightIn(min = 500.dp)` track-area minimum in sheet mode and allowing the Library bottom sheet to partially expand.
- Fixed the reported Library mid-page gap between smart playlists and Albums by removing the large `No playlists yet` empty state from the middle of the collection flow.
- Converted Library filter/mood/smart playlist chips away from Material default chips to flat tokenized pills, and made chip rows horizontally scrollable.
- Fixed Android Studio compile error in `LibraryScreen.kt` around line 1613 by updating the mood-tag dialog's `FilterChip` call to the local tokenized chip signature (`label = mood`) instead of Material's composable `label = { Text(...) }` API.
- Softened the shared `ScreenHeader` by removing the boxed background behind the cat mark.
- Continued Search flow work:
  - Idle "Recommended for you" now renders as a horizontal artwork shelf instead of a vertical stack of full result cards.
  - Frequent "Try these" suggestions now render as horizontal token chips instead of stacked cards.
  - Empty Search copy is now inline/left-aligned instead of centered like a standalone empty panel.
  - Search now has a deterministic discovery starter surface so fresh/idle states do not feel empty: typography-led topbar, supporting copy, and starter query chips.
  - Fixed recommendation-section flash/blank space by rendering "Recommended for you" only while loading or when non-empty recommendation results exist.
  - Added a compact "Keep exploring" chip row under non-empty recommendations to avoid dead space below the recommendation shelf.
  - Search result cards remain for actual search results because they carry multiple actions and progress state.
- Continued the flow-first pass into Stats:
  - Replaced Material default time-range chips with tokenized pill chips.
  - Replaced the personality card surface with a quieter inline accent panel.
  - Flattened the big stat numbers and achievement badge surfaces.
  - Tokenized playlist stat fallback surfaces.

## Checks

- Attempted `./gradlew :app:compileDebugKotlin`.
- Blocked: WSL shell has no Linux Java install.
- Attempted Windows Gradle/JDK paths through WSL.
- Blocked: WSL interop failed with `UtilBindVsockAnyPort:307: socket failed 1`.
- Codex re-checked on 2026-05-12:
  - `gstack` is not available on PATH in this shell.
  - `java -version` still fails because Linux Java is not installed.
  - Android Studio JBR `java.exe` is present on Windows, but WSL interop still fails with `UtilBindVsockAnyPort:307: socket failed 1`.
  - Source review confirms `ArtistDetailScreen` is wired from Library rows and Smart Playlist rows.
- User reported Android Studio build succeeded on 2026-05-12.
- After this handoff update, another Android Studio sync/build/run is needed because album metadata, Album Detail, brand/icon, Home redesign, and the `DownloadManager.kt` fix were added after the last successful build.
- `git diff --check` passes for the files touched in the album-detail slice; full-worktree `git diff --check` still reports pre-existing trailing whitespace in `AndroidManifest.xml` Last.fm intent-filter lines.
- `xdg-open` is unavailable and Windows Explorer interop failed, but `cmd.exe /c start` succeeded for opening `brand-review.html`.
- `git diff --check -- app/src/main/java/com/shuckler/app/ui/SearchScreen.kt` passes after the Search visual pass.
- `git diff --check` passes for the latest visual cleanup files: `HomeScreen.kt`, `LibraryScreen.kt`, `OnboardingScreen.kt`, `PlayerScreen.kt`, `CropCoverDialog.kt`, and `EqualizerScreen.kt`.
- `git diff --check -- DESIGN.md flow-redesign-preview.html` passes.
- `java -version` still fails in this WSL shell because `java` is not installed/on PATH.
- Attempted `./gradlew :app:compileDebugKotlin` after the Home flow implementation; blocked because `JAVA_HOME` is not set and no `java` command is available in PATH.
- `git diff --check -- app/src/main/java/com/shuckler/app/ui/LibraryScreen.kt` passes after the Library flow slice.
- `git diff --check` passes for the Library sheet fix, shared header update, and Search recommendation shelf changes.
- `./gradlew :app:compileDebugKotlin` still blocked in WSL: `JAVA_HOME` is not set and no `java` command is available.
- After the `LibraryScreen.kt` line 1613 fix, Codex-side Gradle is still blocked by missing Java, so Android Studio should rerun `:app:compileDebugKotlin`.
- `git diff --check -- app/src/main/java/com/shuckler/app/ui/SearchScreen.kt app/src/main/java/com/shuckler/app/ui/LibraryScreen.kt` passes after the Search discovery pass.
- `git diff --check -- app/src/main/java/com/shuckler/app/ui/SearchScreen.kt app/src/main/java/com/shuckler/app/ui/AnalyticsScreen.kt` passes after the recommendation gating and Stats flow pass.

## Known Risks

- Kotlin/Compose compile succeeded in Android Studio, but Codex still cannot run Gradle from this WSL shell.
- Album metadata now exists on `DownloadedTrack`, but only new Spotify playlist imports currently populate it.
- Existing downloads will have null album metadata; they fall back to artwork/title grouping until reimported or enriched.
- Older planning docs still mention the old character icon in historical phase notes; app resources and active design docs now point to the original mark.
- Repo already had many modified files before this slice; avoid broad formatting or cleanup until ownership is clear.

## Remaining Work

- Run Android Studio sync/build after the latest album, brand, Home, and Search changes.
- Run Android Studio sync/build after the latest visual-token cleanup.
- Android Studio sync/build the Home flow implementation.
- Android Studio sync/build the Home and Library flow implementation.
- Device-review Home with empty library, some downloads but no plays, and active listening history.
- Device-review Library in sheet and full-tab modes, including empty library, albums shelf, playlists shelf, list/grid track view, swipe delete, and Manage storage disclosure.
- Device-review Search with no library, with saved tracks but no query, with recommendation data, and after a normal search.
- Device-review Stats/Analytics for empty library, active listening history, achievements, and playlist stat shelf.
- Continue with Analytics/Stats and Now Playing polish using the same flow-first rules.
- Review `catdoodle.png` in launcher/header/onboarding after Android Studio sync/build.
- Device-test tapping artist names from Library and Smart Playlists, then opening Album Detail from Artist and Library album shelves.
- Device-test Search preview/play/download actions on a narrow phone viewport; the result action row scrolls horizontally if localized/long labels do not fit.
- Consider a future metadata-enrichment pass for existing YouTube-only downloads that do not have album title/year.
