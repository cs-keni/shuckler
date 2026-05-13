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
