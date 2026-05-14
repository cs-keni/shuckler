# Current Task

Date: 2026-05-14

## Status

**REDESIGN-v3 in progress.** Phases 1–4 + Phase 7.2 implemented this session.

Last commit: see ENGINEERING_LOG.md session 4

## What Was Implemented (this session)

**Phase 2** — Library Album View  
**Phase 3** — List stagger, album art bloom, download spring collapse  
**Phase 4** — Now Playing accent bloom, lyrics transitions (already done), pressScale breadth

Full details in `HANDOFF.md`.

## REDESIGN-v3 Status

| Phase | Status |
|-------|--------|
| 1 — Quick wins (Preview button, bloom corners, tab slide, Library corners) | ✅ Done |
| 2 — Typography (Plus Jakarta Sans replaces DM Serif Display) | ✅ Done |
| 3 — Ambient bloom (warm baseline, unified origin) | ✅ Done |
| 4 — Library reorg (Downloads rename, tappable headers, spacing) | ✅ Done |
| 5 — Downloads background queue | ✅ Done |
| 6 — Stats (color, donut chart, achievements, streak) | ✅ Done |
| 7 — Feel & micro-interactions | ✅ Done (7.2 swipe-up, 7.5 skeleton, 7.6 track menu, 7.8 sleep chip) |
| 8 — Playback & queue UX | ✅ Done (stream loading state, downloaded badge) |

## What's Left (REDESIGN-v3 Phase 5+)

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
