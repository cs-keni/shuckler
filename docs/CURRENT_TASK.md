# Current Task

Date: 2026-05-14

## Status

Phase 1 (Ambient Color System) COMPLETE — commit `033b408`. Phases 2–4 ready for Codex.

## Active Slice

**Ambient Color + Animation System (v2)** — Phase 1 implemented by Claude Code. Phases 2–4 remain.

### Context

Claude Code implemented Phase 1 on 2026-05-14:
- NavGraph: radial gradient bloom from album accent, transparent Scaffold.
- HomeScreen, SearchScreen, AnalyticsScreen: opaque backgrounds removed so bloom shows through.
- LibraryScreen, PlaylistScreen: Scaffold made transparent.
- MiniPlayerBar: whole-pill spring press scale + accent border.
- Nav bar: Rounded icons, "Stats" label, dot indicator above selected tab, accent colors.
- See `DESIGN.md § Ambient Color System (v2 Redesign)` and `color-redesign-preview.html` for visual spec.

### What Codex Must Implement Next

**Phase 1 — Ambient Background — ✅ DONE (Claude Code, 2026-05-14, commit 033b408)**

1. Create `app/src/main/java/com/shuckler/app/ui/AccentExtensions.kt`:
   ```kotlin
   package com.shuckler.app.ui
   import androidx.compose.runtime.Composable
   import androidx.compose.ui.graphics.Color
   import com.shuckler.app.ui.theme.LocalAccentColor

   @Composable fun accentAmbient()    = LocalAccentColor.current.copy(alpha = 0.15f)
   @Composable fun accentWash()       = LocalAccentColor.current.copy(alpha = 0.07f)
   @Composable fun accentChipBg()     = LocalAccentColor.current.copy(alpha = 0.12f)
   @Composable fun accentChipBorder() = LocalAccentColor.current.copy(alpha = 0.40f)
   @Composable fun accentAlbumGroup() = LocalAccentColor.current.copy(alpha = 0.05f)
   ```

2. Create `app/src/main/java/com/shuckler/app/ui/AmbientBackground.kt`:
   ```kotlin
   @Composable
   fun AmbientBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
       val accent = LocalAccentColor.current
       val animatedAccent by animateColorAsState(accent, tween(600, easing = FastOutSlowInEasing), label = "ambient")
       Box(
           modifier
               .fillMaxSize()
               .background(Base)
               .drawBehind {
                   drawRect(
                       brush = Brush.radialGradient(
                           colors = listOf(animatedAccent.copy(alpha = 0.15f), Color.Transparent),
                           center = Offset(size.width / 2f, 0f),
                           radius = size.width * 0.75f
                       )
                   )
               },
           content = content
       )
   }
   ```

3. Wrap each top-level screen scaffold's background with `AmbientBackground` instead of plain `Background(Base)`. Affected screens: `HomeScreen.kt`, `LibraryScreen.kt`, `SearchScreen.kt`, `AnalyticsScreen.kt`, `PlayerScreen.kt`, `PlaylistScreen.kt`, `ArtistDetailScreen.kt`, `AlbumDetailScreen.kt`.

4. In `MiniPlayerBar.kt`: add `border = BorderStroke(1.dp, LocalAccentColor.current.copy(alpha = 0.20f))` to the pill container.

5. Replace static `Amber` chip styling (selected state) in `LibraryScreen.kt`, `SearchScreen.kt`, `AnalyticsScreen.kt` with `accentChipBg()` background + `accentChipBorder()` border. (All other chip internals unchanged.)

6. In all playing-track rows across all screens: use `accentWash()` as the row's background modifier instead of whatever static tint is there now. The title text of the playing track should use `LocalAccentColor.current` at full alpha.

**Phase 2 — Library Album View**

Add a "By Album" filter chip to `LibraryScreen.kt`. When active, group the track list by `DownloadedTrack.albumTitle` (fall back to "Unknown Album" when null). Render each album group as:
- Album section header: 28×28dp thumbnail + DM Serif Display album name + DM Mono metadata row + collapse chevron.
- Playing album header: `accentAlbumGroup()` background + amber title text + amp bars in thumbnail.
- Track rows under that album: indent 40dp from left. Remove track row art (it's implied by the album header above).
- Divider between albums: 1px `BorderSubtle`.

See `DESIGN.md § Library — Album-First View (v2)` for full spec.

**Phase 3 — Animation System**

Implement all six animations from `DESIGN.md § Animation System (v2)`:
1. Ambient Transition — already handled by Phase 1's `animateColorAsState`.
2. List Stagger Entrance — `TrackRow` with `LaunchedEffect` + `AnimatedVisibility`. Use `key = { item.id }` not index.
3. Tab Slide — `AnimatedContent` in `NavGraph.kt` tab host.
4. Press Feedback — `Modifier.pressScale()` extension, apply to all tappable surfaces.
5. Album Art Bloom — pulsing `animateFloatAsState` behind playing album in shelf composables.
6. Download Spring Collapse — `AnimatedVisibility` with `shrinkVertically(spring(...))` exit in `WaveformDownloadCard.kt`.

**Phase 4 — Polish**

- Apply `Modifier.pressScale()` to all interactive elements not already covered.
- Add album bloom to Now Playing art (extend existing breathing glow to use accent color bloom at 35% instead of static amber).
- Lyrics line transitions: entrance animation for each lyric line as it becomes active.

## Next Best Work

Codex: Start with Phase 1. It touches many files but requires no structural changes — only new composables and wrapper changes. Build and test after Phase 1 before moving to Phase 2. The `color-redesign-preview.html` file is the visual reference.
