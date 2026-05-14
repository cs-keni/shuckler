# Handoff

Date: 2026-05-14

## Changed (Claude Code — 2026-05-14, session 2)

Commit: `740648f` — "Implement Phase 2-4: album grouped view, list stagger, album bloom, spring collapse"

### New file: `AccentExtensions.kt`

- Five accent color helpers: `accentAmbient()`, `accentWash()`, `accentChipBg()`, `accentChipBorder()`, `accentAlbumGroup()` — all composable functions returning `LocalAccentColor.current.copy(alpha = ...)`.
- `Modifier.pressScale(targetScale)` — non-consuming spring press scale using `awaitEachGesture` + `awaitFirstDown(requireUnconsumed = false)`. Compatible with `Modifier.clickable` (does not steal touch events).

### Phase 2 — Library Album View (COMPLETE)

- **`LibraryFilter` enum**: added `BY_ALBUM` between `ALL` and `FAVORITES`.
- **"By Album" chip**: rendered in the filter chip row. Selecting it hides the sort button and shows `AlbumGroupedList` instead of the flat list.
- **`searchFilteredAlbumGroups`**: computed from `albumGroups` + `searchQuery`. A group is included if its title/artist matches, or if any of its tracks match; only matching tracks are shown for partial-match groups.
- **`AlbumGroupHeader`**: 28×28dp thumbnail (4dp radius) + DM Serif Display album title (via `typography.headlineSmall`) + DM Mono metadata row (artist · year · N songs) + spring-animated expand/collapse chevron. Playing album: `accent.copy(alpha = 0.06f)` background + accent title text.
- **`AlbumGroupedList`**: `LazyColumn` with `forEach { item(...) + itemsIndexed(...) + item(divider) }` pattern. Collapse state tracked with `mutableStateSetOf`. Playing album detection via `currentPlayingTrackId`. Empty state handled internally.
- Track rows inside groups: `LibraryTrackItem(showArt = false, modifier = Modifier.padding(start = 40.dp))`. Play action builds queue from the whole album.

### Phase 3 — Animations (COMPLETE)

- **List Stagger**: `itemsIndexed` in flat LazyColumn (and inside `AlbumGroupedList`). First 4 items stagger 0/30/60/90/120ms. Items ≥5 appear immediately. `LaunchedEffect(track.id)` triggers the delay so new tracks animate in, cached tracks do not.
- **Album Art Bloom**: `rememberInfiniteTransition` in `LibraryAlbumCard`. `bloomPulse` 0→1→0 every 2000ms. Bloom box: `fillMaxSize`, `scale(1.0 + pulse*0.18)`, `alpha(0.35 + pulse*0.45)`, `RoundedCornerShape(18.dp)`, `accentColor` fill. Rendered only when `isPlaying = true`. Non-playing albums still compute the transition cheaply (both values equal 0).
- **Download Spring Collapse**: `lingeringProgress` map in `SearchScreen` keeps cards alive after they leave the `progress` flow. `LaunchedEffect(isActive)` schedules 1.5s delay → `cardVisible = false` → `AnimatedVisibility` exits with `shrinkVertically(spring(StiffnessLow, MediumBouncy)) + fadeOut()`.

### Phase 4 — Polish (partial)

- `pressScale(0.92f)` on `FilterChip` box.
- `pressScale(0.96f)` on `LibraryAlbumCard` column.
- `showArt: Boolean = true` param added to `LibraryTrackItem`; art block gated on this flag.

## Checks

- No Gradle build available in WSL (known limitation).
- `git diff --check` passes on all three modified/new files.

## Remaining Work

### Phase 4 — Polish (remaining)

- Apply `pressScale` to more interactive elements: `PlaylistCard`, `LibraryTrackItem` row (the whole row, not just the swipe-dismiss background), shelf items in `HomeScreen`.
- **Album bloom on Now Playing screen**: extend the existing breathing glow in `PlayerScreen.kt` to use `LocalAccentColor.current` at 35% opacity instead of static amber. The current glow already uses `animateFloatAsState` with infinite repeat; just change the color.
- **Lyrics line transitions**: `PlayerScreen.kt` lyrics view — each lyric line should animate to `TextSize(15sp)` + `alpha(1f)` when active, with adjacent lines at 13sp/0.6f and distant at 12sp/0.35f.

### Device testing needed

- Test ambient bloom with dark, light, and desaturated album palettes.
- Verify album grouped list collapse/expand with large libraries (50+ albums).
- Verify stagger doesn't fire repeatedly on scroll (confirm `key = { track.id }` is stable).
- Verify spring collapse timing feels right in the download flow.
