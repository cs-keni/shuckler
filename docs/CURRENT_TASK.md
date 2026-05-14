# Current Task

Date: 2026-05-14

## Status

**Phases 1–4 COMPLETE.** All ambient color + animation system work is done.

Last commit: `0a7a130`

## What Was Implemented (this session)

**Phase 2** — Library Album View  
**Phase 3** — List stagger, album art bloom, download spring collapse  
**Phase 4** — Now Playing accent bloom, lyrics transitions (already done), pressScale breadth

Full details in `HANDOFF.md`.

## What's Left (optional Phase 5 / future)

These are nice-to-have improvements beyond the v2 spec:

1. **Shared-element transition** — pill → Now Playing screen art (requires Compose SharedTransition API or manual hero animation).
2. **Track crossfade UI** — visual fade indicator when crossfade is active.
3. **Sleep timer countdown chip** — visible chip in Now Playing action row when timer is running (state already available via `sleepTimerRemainingMs`).
4. **Queue row press scale** — `pressScale` on reorderable queue rows.
5. **Home shelf press scale** — shelf art tiles in HomeScreen.
6. **Broader device testing** — bloom with dark/light/desaturated palettes, tablet layout.

## Design Source of Truth

- `DESIGN.md` — canonical spec for all decisions
- `color-redesign-preview.html` — interactive before/after for ambient color system
- `design-preview.html` — all screens overview

## Notes for Codex

- `AccentExtensions.kt` is in `com.shuckler.app.ui` — import helpers from there.
- `pressScale()` is `@Composable Modifier` — must be in composable scope.
- `BY_ALBUM` filter state in LibraryScreen resets when navigating away (correct behavior).
- `AlbumGroupedList` uses `var collapsedAlbums by mutableStateOf(emptySet<String>())` — `mutableStateSetOf` is not available in this Compose runtime version.
