# Handoff

Date: 2026-05-14

## Changed (Claude Code — 2026-05-14, sessions 2 & 3)

### Commits this session

| Hash | Description |
|------|-------------|
| `740648f` | Phase 2–4: album grouped view, list stagger, album bloom, spring collapse |
| `4e90696` | Fix: replace mutableStateSetOf with mutableStateOf(emptySet) for compat |
| `0a7a130` | Phase 4: Now Playing accent bloom, pressScale breadth |

---

### AccentExtensions.kt (new)

- Five accent color helpers: `accentAmbient()`, `accentWash()`, `accentChipBg()`, `accentChipBorder()`, `accentAlbumGroup()`.
- `Modifier.pressScale(targetScale)`: non-consuming spring press scale using `awaitEachGesture` + `awaitFirstDown(requireUnconsumed = false)`. Works alongside `Modifier.clickable`.

### Phase 2 — Library Album View (COMPLETE)

- `LibraryFilter.BY_ALBUM` added. "By Album" chip routes to `AlbumGroupedList`.
- `searchFilteredAlbumGroups`: groups filtered by title/artist/tracks matching search query.
- `AlbumGroupHeader`: 28×28dp thumbnail, DM Serif Display title, DM Mono metadata, spring chevron. Playing album: `accent.copy(alpha = 0.06f)` bg + accent title.
- `AlbumGroupedList`: collapsible albums via `var collapsedAlbums by mutableStateOf(emptySet<String>())`. Toggle uses immutable set `+`/`-` operations. Indented track rows (`showArt = false`, 40dp start padding).
- Sort button hidden in BY_ALBUM mode.
- `LibraryTrackItem`: `showArt: Boolean = true` parameter added.

### Phase 3 — Animation System (COMPLETE)

- **List Stagger**: `itemsIndexed` in flat list and album grouped list. Items 0–4 stagger 30ms each; items ≥5 instant.
- **Album Art Bloom**: infinite `bloomPulse` 0→1 in `LibraryAlbumCard`. Bloom box scale 1.0→1.18, alpha 0.35→0.80, `RoundedCornerShape(18.dp)`, accent fill.
- **Download Spring Collapse**: `lingeringProgress` map in SearchScreen. 1.5s delay → `cardVisible = false` → `shrinkVertically(spring(StiffnessLow, MediumBouncy)) + fadeOut()`.

### Phase 4 — Polish (COMPLETE)

- **Now Playing accent bloom**: added a solid accent-color Box behind the breathing glow ring (same `glowScale`/`glowAlpha`). Uses `albumColor.copy(alpha = glowAlpha * 0.9f)`. Blurred thumbnail glow preserved. Gated on `isPlaying`.
- **Lyrics transitions**: already implemented correctly in `PlayerScreen.kt` (animated alpha + fontSize per line distance). No changes needed.
- **pressScale coverage**: `FilterChip` (0.92f), `LibraryAlbumCard` (0.96f), `PlaylistCard` (0.96f), `AlbumGroupHeader` row (0.98f).

## Checks

- `mutableStateSetOf` replaced with `mutableStateOf(emptySet<String>())` — Compose runtime compat.
- `git diff --check` passes on all modified files.
- Lyrics transitions confirmed already implemented — no change needed.
- WSL Gradle blocked (known). Build in Android Studio.

## Known Risks / Next Steps

- **Device testing**: ambient bloom with dark/light/desaturated palettes; album grouped list collapse with large library; stagger doesn't re-fire on scroll.
- **pressScale + clickable interaction**: uses `requireUnconsumed = false` so both handlers fire, but test on physical device for edge cases (fast taps, long-press context menu).
- **Tablet/foldable**: bloom radius in NavGraph was designed for phone screens; may need adjustment.
- **Phase 5 (if desired)**: shared-element transition pill → Now Playing, crossfade between tracks, sleep timer countdown chip in Now Playing.
