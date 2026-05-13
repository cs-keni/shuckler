# Engineering Log

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
- Codex still cannot run Gradle in this WSL shell; use Android Studio sync/build for compile verification.
- Latest visual cleanup removed `CircleShape` from onboarding pager indicators and migrated hardcoded black/white overlay values in UI composables to existing warm tokens. `git diff --check` passes for the touched files; Java/Gradle verification remains blocked in WSL.
- User reported the redesigned UI still feels gappy and too much like stacked boxes. Added a flow-first planning section to `DESIGN.md` and created `flow-redesign-preview.html`; no Compose implementation was changed for this planning pass.
- First flow-first implementation slice targets Home only. Existing behavior is preserved, but visual structure changed substantially. Codex still cannot compile due to missing Java in WSL, so Android Studio verification is required.
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
