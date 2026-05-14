# Handoff

Date: 2026-05-14

## Changed (Claude Code — 2026-05-14)

- Diagnosed the "too black / not enough color" problem: `LocalAccentColor` from album art Palette API was used only on Now Playing elements. Every other screen (Library, Home, Search, Stats) was monochromatic warm-dark.
- Added `## Ambient Color System (v2 Redesign)` section to `DESIGN.md` — defines `AmbientBackground` composable, six dynamic tint helpers (`accentAmbient`, `accentWash`, `accentChipBg`, `accentChipBorder`, `accentAlbumGroup`), and a full inventory of where each token appears.
- Added `## Library — Album-First View (v2)` section to `DESIGN.md` — album-grouped list with per-album section headers, playing album tint, and collapsed state.
- Added `## Animation System (v2)` section to `DESIGN.md` — six named animations with Compose code: Ambient Transition, List Stagger Entrance, Tab Slide, Press Feedback, Album Art Bloom, Download Spring Collapse.
- Updated Design Principle #06 from "Art as the only accent" to "Art as ambient light" to reflect the expanded color philosophy.
- Created `color-redesign-preview.html` — interactive before/after comparison with live album color simulator (click color dots to see different Palette API extractions).
- Updated `docs/CURRENT_TASK.md` with precise Codex implementation instructions for all four phases.
- Updated global `~/.claude/CLAUDE.md` with: (a) requirement to always read docs at session start; (b) requirement to commit + push with a concise message after every meaningful implementation slice; (c) requirement to log commit hash in `docs/ENGINEERING_LOG.md`.

## Checks

- `git diff --check -- DESIGN.md color-redesign-preview.html docs/CURRENT_TASK.md docs/HANDOFF.md` — passes.
- No Compose code was changed in this session; only design docs and the HTML preview.
- Android Studio build state: last confirmed successful build was 2026-05-13 (user report).

## Known Risks

- `AmbientBackground` must not double-apply `Modifier.background(Base)` if the screen scaffold already sets it. Codex should remove the existing `background(Base)` modifier from screen scaffolds when wrapping with `AmbientBackground`.
- `animateColorAsState` for accent transition requires the `LocalAccentColor` to be updated on the main thread via the existing `PlayerViewModel` Palette extraction flow. Verify the extraction still happens correctly after wrapping screens with `AmbientBackground`.
- Stagger entrance pitfall: `key = { item.id }` is required in `LazyColumn`. Index-based keys cause animation re-fire on every recompose. (See prior pitfall: `staggered-lazyrow-key-stability`)
- Library "By Album" view depends on `DownloadedTrack.albumTitle` being non-null. Tracks downloaded before album metadata was added (pre-2026-05-12) will fall under "Unknown Album". This is acceptable until a metadata enrichment pass is done.

## Remaining Work

- **Phase 1 (Ambient):** Create `AccentExtensions.kt`, `AmbientBackground.kt`. Wrap all screen scaffolds. Update mini player border. Replace static amber chips with dynamic accent chips.
- **Phase 2 (Library Album View):** Add "By Album" filter chip and grouped list composable to `LibraryScreen.kt`.
- **Phase 3 (Animations):** List stagger, tab slide, press feedback, album bloom, download collapse.
- **Phase 4 (Polish):** Extend press scale to all interactive elements, album bloom in Now Playing, lyrics line entrance.
- Device review all screens after Phase 1 lands to confirm ambient color looks right across different album palettes (dark albums, light albums, desaturated albums).
- Consider a future metadata enrichment pass for existing YouTube-only downloads that have null album title.
