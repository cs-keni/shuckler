# Current Task

Date: 2026-05-14

## Status

Phase 1 ✅ · Phase 2 ✅ · Phase 3 ✅ · Phase 4 partial ⚙️

All core ambient color + animation phases are implemented. Phase 4 polish remains.

## What Was Implemented (commit `740648f`)

**Phase 2 — Library Album View**
- `BY_ALBUM` filter chip in Library → shows `AlbumGroupedList`
- Collapsible album sections with DM Serif Display headers + spring chevron
- Playing album: accent tint + accent title color
- Indented track rows (40dp, no art)
- Per-album stagger entrance animation
- Search filtering across album groups

**Phase 3 — Animation System**
- `AccentExtensions.kt`: 5 accent helpers + `Modifier.pressScale()` (non-consuming)
- List stagger: first 5 items in flat list and album grouped list
- Album art bloom: infinite pulsing glow behind playing album in shelf
- Download spring collapse: 1.5s delay → `shrinkVertically(spring)` + `fadeOut`

**Phase 4 — Polish (partial)**
- `pressScale` on FilterChip and LibraryAlbumCard
- `showArt` param on LibraryTrackItem

## Remaining Work (Phase 4)

1. **Now Playing art bloom** — `PlayerScreen.kt`
   - Find the breathing glow composable/animation
   - Change glow color from static amber to `LocalAccentColor.current` at 35% opacity
   - The rest of the glow mechanism (infinite animateFloat for scale/opacity) stays as-is

2. **Lyrics line transitions** — `PlayerScreen.kt`
   - Active lyric line: `TextSize(15sp)`, `alpha = 1f`
   - Adjacent lines: `13sp`, `alpha = 0.6f`
   - Distant lines: `12sp`, `alpha = 0.35f`
   - Use `animateFloatAsState` for size + alpha, triggered by `activeLineIndex` changes
   - See `DESIGN.md § Lyrics`

3. **pressScale breadth** — `HomeScreen.kt`, `LibraryScreen.kt` (PlaylistCard, track rows)
   - Apply `Modifier.pressScale()` to: shelf art tiles, playlist cards, track row touchable area
   - Non-conflicting with `clickable` (uses `requireUnconsumed = false`)

## Notes for Codex

- `AccentExtensions.kt` is in `com.shuckler.app.ui` package — import from there.
- `Modifier.pressScale()` is `@Composable` so must be called in composable scope.
- `AlbumGroupedList` reads `collapsedAlbums` as `SnapshotStateSet` — recomposition happens automatically when set changes.
- `LibraryAlbumCard` always runs `rememberInfiniteTransition` but bloom Box is only rendered when `isPlaying = true`, so performance is fine.
- WSL Gradle still blocked (known). Build in Android Studio.
