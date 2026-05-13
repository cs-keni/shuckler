# Handoff

Date: 2026-05-12

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

## Known Risks

- Kotlin/Compose compile succeeded in Android Studio, but Codex still cannot run Gradle from this WSL shell.
- Album metadata now exists on `DownloadedTrack`, but only new Spotify playlist imports currently populate it.
- Existing downloads will have null album metadata; they fall back to artwork/title grouping until reimported or enriched.
- Older planning docs still mention the old character icon in historical phase notes; app resources and active design docs now point to the original mark.
- Repo already had many modified files before this slice; avoid broad formatting or cleanup until ownership is clear.

## Remaining Work

- Run Android Studio sync/build after the latest album, brand, and Home changes.
- Review `catdoodle.png` in launcher/header/onboarding after Android Studio sync/build.
- Device-test tapping artist names from Library and Smart Playlists, then opening Album Detail from Artist and Library album shelves.
- Consider a future metadata-enrichment pass for existing YouTube-only downloads that do not have album title/year.
