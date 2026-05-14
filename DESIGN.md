# Shuckler Design System

> A personal music app that feels like it was made by hand — because it was.
> Analog warmth. Digital precision.

---

## Design Principles

Seven principles that every decision traces back to.

| # | Principle | Meaning |
|---|-----------|---------|
| 01 | **Music moves the UI** | Audio amplitude and playback state drive subtle animations everywhere — not decorations bolted on top. |
| 02 | **Rectangles, not circles** | Music has always lived in rectangles: album covers, cassette inserts, jewel cases. Circles crop the artist's composition and add no meaning. |
| 03 | **Warmth over neutral** | `#0C0A07`, not `#000000`. A whisper of warmth makes the app feel like listening under a lamp, not in a server room. |
| 04 | **Gestures are first-class** | Expand, dismiss, skip — via continuous gestures, not button taps. The UI feels physically held. |
| 05 | **The DM type system** | DM Serif Display (emotional) + DM Mono (data). Same type family — intentional, cohesive, no cursive. |
| 06 | **Art as ambient light** | Album Palette API color bleeds through the *entire* app as ambient atmosphere — not just the Now Playing screen. The whole interface is tinted by whatever you're listening to. An advantage no commercial streaming service can copy. |
| 07 | **Your listening, visible** | History, stats, and scrobbling make the app feel like a living record of your taste — not just a player. |

---

## Flow-First Redesign Direction

The current implementation is moving toward the right palette and typography, but the screen composition can still feel like separated boxes with accidental gaps. The next redesign pass should focus on continuous layout flow before adding more components.

### Research Notes

- Apple layout guidance emphasizes full-bleed content, clear visual hierarchy, consistent alignment, and scrollable layouts that extend behind control layers instead of stopping at obvious bars.
- Material layout guidance says cards are useful when a grouping needs a distinct entry point, varied content behavior, or extra separation. Cards should not be the default container for every section.
- Spotify's newer adaptive design language points toward browsing that remains familiar while making the currently playing context feel present rather than detached.

### Composition Rules

1. **One canvas per screen.** Screens use `Base` as a continuous scroll canvas. Avoid stacking multiple full-width boxed sections with their own backgrounds.
2. **Cards are exceptions.** Use cards for search results, active downloads, modals, and playlist/album entry points. Do not wrap simple headings, stats, chips, or short rows in cards.
3. **Art leads the page.** Home and Library should start with album/playlist artwork or a compact now-playing context, not an empty header gap.
4. **No dead vertical zones.** If a section has no content, collapse it or replace it with a compact inline empty state. Do not reserve hero-height space for missing recommendations.
5. **Rows belong to the canvas.** Track rows are flat list items separated by subtle dividers or spacing. The currently playing row gets an accent wash, not a boxed card.
6. **Section rhythm is tight and repeatable.** Use `20dp` after the screen header, `16dp` between section title and content, and `24dp` between major sections. Avoid arbitrary `32–48dp` gaps except before true full-screen empty states.
7. **Horizontal shelves should peek.** Album, playlist, and recommendation shelves intentionally show partial next items to communicate scrollability.
8. **Controls float above content.** The mini player and nav bar overlay the bottom of the canvas. Content gets bottom padding, but the canvas visually continues underneath.

### Home Redesign Target

Home should feel like a personalized music feed, not a dashboard.

**Top region**
- Collapsed brand/header row: "Shuckler" or greeting, settings icon, no large standalone card.
- Compact "Continue listening" strip when playback/history exists:
  - Large rectangular artwork on the left or full-bleed artwork band.
  - Title, artist, and one primary action.
  - Palette tint fades directly into `Base`.
- If nothing is available, show a compact inline empty state and immediately surface Search prompts.

**Main feed**
- "Recently added" shelf: artwork-only first impression, labels underneath, no card backgrounds.
- "Made from your library" or recommendations: one horizontal shelf of art-backed tiles. Tiles may have image overlays, but the section itself stays unboxed.
- "Quick starts": small text chips or rows, not large cards.
- "Your snapshot": compact inline metrics row with no outer card. Use typography and alignment for hierarchy.

**Spacing**
- Header to first content: `12–16dp`.
- Section-to-section: `24dp`.
- Shelf item gap: `10–12dp`.
- No blank region larger than `32dp` unless it is the bottom safe area behind the mini player.

### Library Redesign Target

Library should behave more like a dense collection view.

- Keep the Albums shelf, but make it the first visual anchor after the header when albums exist.
- Move filters into a single horizontally scrollable chip row directly under the header.
- Playlist cards should be compact art tiles, not large surface blocks.
- Track list rows stay flat and dense; use `BorderSubtle` only when needed.
- Storage/download maintenance moves behind a "Manage" affordance or collapsed utility section so it does not interrupt browsing.

### Search Redesign Target

Search can keep cards because each result has mixed actions, metadata, and download states. The next pass should still reduce boxed feel:

- Search bar remains a pill.
- Recent and suggestion chips stay unboxed.
- Results cards use one shared plane, consistent `8dp` radius, and minimal border.
- Recommended results should appear as an art shelf before list cards when the user has not searched yet.

### Preview Artifact

The flow-first composition study is at `flow-redesign-preview.html` in the project root. It is a planning artifact, not an implementation source.

---

## Brand

The app should not use character art, mascot art, or any asset derived from existing copyrighted characters.

Current direction:
- **Name:** Shuckler remains the working name until a deliberate rename is chosen.
- **Mark:** User-created `catdoodle.png`, a simple black-line cat drawing. It is deliberately not derived from existing character art.
- **Review artifact:** `brand-review.html` in the project root compares the current recommendation against possible rename directions.

Implementation rules:
- Use `R.drawable.catdoodle` for in-app brand marks.
- Launcher foreground/background are vector drawables in `res/drawable`.
- Do not reintroduce `ic_shuckle`, `shuckle.png`, `shuckle.svg`, or character-derived placeholders.

---

## Color Tokens

All colors are defined as tokens in `Color.kt` and referenced via `MaterialTheme`. No hardcoded color values anywhere in the composables.

```kotlin
// Background / Surface
val Base            = Color(0xFF0C0A07)   // App background — warm near-black
val Surface         = Color(0xFF141210)   // Cards, bottom sheets
val SurfaceElevated = Color(0xFF1C1A17)   // Elevated cards, inputs
val SurfaceHigh     = Color(0xFF252220)   // Highest elevation (pill, context menus)

// Borders
val Border          = Color(0xFF2A2722)   // Standard dividers and card borders
val BorderSubtle    = Color(0xFF1E1C19)   // Track row separators

// Text
val Text1           = Color(0xFFF5F2EC)   // Primary — warm off-white
val Text2           = Color(0xFF8C8880)   // Secondary — muted warm grey
val Text3           = Color(0xFF4A4843)   // Tertiary — very muted, timestamps, labels

// Accent
val Amber           = Color(0xFFE8A850)   // Static accent — replaced at runtime by album Palette
val Green           = Color(0xFF6AB187)   // Success / download complete
val Red             = Color(0xFFC0635A)   // Error / destructive
```

### Dynamic Palette

At runtime, the Palette API extracts a dominant vibrant or muted color from the playing track's album art. This extracted color replaces `Amber` as the UI accent — tinting the progress bar, amplitude bars, playing track row, and surface overlays on the Now Playing screen.

The Palette color is threaded through the app via a `CompositionLocal`:

```kotlin
val LocalAccentColor = compositionLocalOf { Amber }
```

---

## Typography

Two typefaces. One type family. Zero cursive.

| Role | Typeface | Size | Usage |
|------|----------|------|-------|
| Display | DM Serif Display | 28sp | Now Playing song title, onboarding |
| Screen header | DM Serif Display | 22sp | Library, Search, Downloads, Queue, Stats screen titles |
| Section header | DM Serif Display | 16sp | "Albums", "Recent", "Trending", "Up Next" |
| Empty state | DM Serif Display | 16–18sp | Empty state messages ("Your library is quiet.") |
| Lyrics active | DM Serif Display | 15sp | The currently playing lyric line |
| Timestamp | DM Mono | 13sp | Playback position / duration |
| Metadata | DM Mono | 9–11sp | Artist, bitrate, format, status labels |
| UI chrome | System sans | 11–13sp | General UI: list items, buttons, nav labels |

### Loading fonts in Compose

```kotlin
val dmSerifDisplay = FontFamily(
    Font(GoogleFonts.Font("DM Serif Display"), weight = FontWeight.Normal)
)
val dmMono = FontFamily(
    Font(GoogleFonts.Font("DM Mono"), weight = FontWeight.Normal),
    Font(GoogleFonts.Font("DM Mono"), weight = FontWeight.Medium)
)
```

---

## Components

### Floating Pill Mini Player

Replaces the full-width bottom bar. A floating pill that sits between the content and the nav bar, with a blurred background and a 1dp progress line along its bottom edge.

**Anatomy:**
- `34×34dp` rounded rectangle album art (`8dp` corner radius)
- Song title in DM Serif Display, single line, ellipsized
- Live amplitude bars (8 bars, 2dp wide, driven by FFT data)
- 30dp circular play/pause button
- 1dp progress line along the bottom edge of the pill

**Behavior:**
- Swipe up → shared-element transition to full Now Playing screen
- Swipe down → dismiss (stops playback, animated collapse)
- Pill is only visible when `hasActivePlayback` is true

### Album Art

All album art is displayed as a rounded rectangle (`RoundedCornerShape(16.dp)` for full-size, `5–8dp` for list items). No `CircleShape` anywhere.

**Sizes:**
- Now Playing: `196×196dp`, `18dp` radius, with breathing glow behind
- Shelf / album grid: `75–80dp` square, `8dp` radius
- Track rows: `36dp` square, `5dp` radius
- Pill: `34dp` square, `7dp` radius

### Breathing Glow Ring

Behind the album art on the Now Playing screen — a blurred copy of the album art at larger size, animated with a subtle scale + opacity oscillation driven by playback amplitude.

```kotlin
// Conceptual implementation
val breatheScale by animateFloatAsState(
    targetValue = if (isPlaying) 1.06f else 0.9f,
    animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    )
)
```

### Shimmer Skeleton

Used during any loading state to mirror the exact layout of the content that will appear. Implemented as a `Brush.linearGradient` that sweeps across the placeholder shape.

```kotlin
val shimmerBrush = Brush.linearGradient(
    colors = listOf(SurfaceElevated, SurfaceHigh, SurfaceElevated),
    start = Offset(shimmerOffset, 0f),
    end = Offset(shimmerOffset + 600f, 0f)
)
```

Skeletons mirror the shelf grid, track rows, search results — every loading surface has a dedicated skeleton composable.

### Waveform Download Card

Replaces the full-width horizontal progress bar. Each active download renders as a card with:
- Album art + title + artist in the header
- A waveform visualization below: bars of randomized heights that fill left-to-right as bytes arrive
- The frontier bar (rightmost filled bar) glows with `box-shadow` / `BlendMode`
- On completion, the card spring-collapses and the track transitions into the library

### Empty States

Every empty state uses DM Serif Display for the message and DM Mono for the supporting line.

```
Your library is quiet.          ← DM Serif Display, 18sp
Search for something            ← DM Mono, 10sp, Text3
and download it to start.
```

```
Nothing playing right now.
Tap a song in your library.
```

```
No results for "that song".
Try a different search.
```

---

## Screens

### Library

- **Header:** DM Serif Display "Library" + DM Mono song count
- **Shelf:** Horizontal scroll of album art rectangles, grouped by "Albums" section
- **Tracklist:** Full track rows below shelf; currently playing row uses DM Serif Display in amber
- **Floating pill:** Always visible when playback is active, floating above the nav bar

### Now Playing

- **Background:** Extracted Palette color as a soft gradient overlay on `#0C0A07`
- **Album art:** `196×196dp` rectangle, `18dp` radius, centered with breathing glow ring behind
- **Title:** DM Serif Display, 22sp — the most typographically prominent element
- **Artist/album:** DM Mono, 11sp, 45% opacity
- **Progress bar:** 2dp height, white fill, 8dp dot scrubber
- **Controls:** Previous / Play-Pause (58dp circle) / Next
- **Action chips:** LYRICS · ♡ · ⇄ · QUEUE

### Search

- **Header:** DM Serif Display "Search"
- **Search bar:** Rounded pill, DM Mono placeholder
- **Section headers:** DM Serif Display "Recent", "Trending"
- **Recent chips:** DM Mono tags in `SurfaceElevated` pill containers
- **Results:** Card list with rectangle art and DM Mono metadata

### Downloads

- **Header:** DM Serif Display "Downloads"
- **Active download card:** Waveform frontier visualization; DM Mono percentage
- **Queued card:** 55% opacity, "QUEUED" label in DM Mono
- **Completed card:** 40% opacity, "DONE" in green DM Mono

### Artist Page

- **Hero:** Blurred, darkened version of any album's art as a full-bleed header
- **Overlay gradient:** Fades to `Base` at the bottom so content reads cleanly
- **Artist name:** DM Serif Display, 22sp, bottom-aligned in hero
- **Sections:** "Songs" + "Albums" with the shelf layout below the tracklist

### Album Detail

- **Hero:** Blurred album palette + overlay, album cover art (`60×60dp`) + title + metadata bottom-aligned
- **Album title:** DM Serif Display, 17sp
- **Metadata:** DM Mono: artist · year · song count · total runtime
- **Actions:** Play All (amber filled) + Shuffle (outlined) buttons
- **Tracklist:** Track number (DM Mono) + art + name + duration, currently playing row highlighted

### Queue

- **Header:** DM Serif Display "Queue" + "CLEAR" in DM Mono amber
- **Now Playing card:** Elevated card pinned at top showing current track with live amplitude bars
- **Up Next list:** Track rows with drag handles on the right for reordering
- **Reorder:** Long press + drag with spring-physics animation and haptic on lift

### Listening Stats

- **Header:** DM Serif Display "Stats" (lives on Profile tab)
- **Big numbers:** DM Serif Display for the numeric value, DM Mono for labels
- **Weekly chart:** 7 bars, today highlighted in full amber, past days at 50% opacity
- **Top artists:** Ranked list with proportional amber fill bars
- **Data source:** Local Room database (no server required)

### Settings

Grouped into sections with DM Serif Display section titles:

**Connections**
- Last.fm Scrobbling — toggle + OAuth flow
- Spotify Metadata — toggle (uses existing `SpotifyAuthManager`)

**Playback**
- Audio Quality — LOW / MEDIUM / HIGH
- Crossfade — 0–10s slider
- Sleep Timer — toggle + duration picker

**Appearance**
- Dynamic Album Colors — toggle Palette API tinting
- Large Text — accessibility scaling

### Lyrics

Full-screen overlay accessible from the LYRICS chip on Now Playing:
- Mini album art + song title at top
- Lyric lines centered, scrolling
- **Active line:** DM Serif Display, 15sp, `Text1`
- **Adjacent lines:** 13sp, `Text2`, opacity transition
- **Distant lines:** 12sp, `Text3`
- Line transitions use `animateFloatAsState` for size and opacity
- Progress bar + minimal controls at the bottom

---

## Interaction Design

### Gesture Map

| Gesture | Where | Action |
|---------|-------|--------|
| Swipe up | Pill | Expand to Now Playing (shared-element transition) |
| Swipe down | Now Playing | Collapse to pill (art animates back to pill size) |
| Swipe down | Pill | Dismiss playback |
| Swipe left/right | Album art (Now Playing) | Next / previous track (card tilts, snaps away) |
| Long press | Track row | Context menu (queue, download, playlist, share) |
| Drag | Progress bar | Scrub — thumb springs to finger position |
| Long press + drag | Queue row | Reorder with spring physics |

### Spring Specifications

All interactive elements use spring physics, not duration-based easing.

```kotlin
// Button press scale
val pressedScale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

### Haptics

| Event | Pattern |
|-------|---------|
| Pill expand | `HapticFeedbackType.LongPress` |
| Track skip | `HapticFeedbackType.TextHandleMove` |
| Queue row lift | `HapticFeedbackType.LongPress` |
| Queue row drop | `HapticFeedbackType.LongPress` |
| Progress scrub end | `HapticFeedbackType.TextHandleMove` |
| Sleep timer end | `HapticFeedbackType.LongPress` |

---

## New Features

### Last.fm Scrobbling

A track is scrobbled when it has been played for at least 50% of its duration (Last.fm standard). OAuth login via WebView in Settings. Token stored in `EncryptedSharedPreferences`.

**API calls:**
- `POST auth.getMobileSession` — initial auth
- `POST track.scrobble` — after each qualifying play event
- `POST track.updateNowPlaying` — on track start (optional, shows "now playing" on Last.fm profile)

Failure to scrobble is silent — no error UI, no retry beyond 3 attempts. Scrobbling is a bonus, not a critical path.

### Spotify Metadata Enrichment

Uses the existing `SpotifyAuthManager` OAuth token. After auth, a background `WorkManager` job walks through the library and enriches songs with:
- Artist profile image URL (cached locally)
- Artist biography text
- "Appears on" album list

Data is stored in a new `ArtistMetadata` Room entity. Artist pages display this data when available, fall back to album art if not.

### Listening History + Stats

A new `PlayEvent` Room entity records every qualifying listen:

```kotlin
@Entity
data class PlayEvent(
    val songId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val playedMs: Long,
    val startedAt: Long   // epoch ms
)
```

A play event is committed when the user skips, pauses, or the track ends — whichever comes first. The Stats screen queries this table for weekly aggregation and top artist ranking.

### Smart Auto-Playlists

Generated from `PlayEvent` data via Room queries. Auto-playlists are virtual — no separate playlist entity needed, just a named query:

| Playlist | Query |
|----------|-------|
| Most Played | Top 25 songs by `COUNT(*)` over all time |
| Recently Added | Songs sorted by `downloadedAt DESC`, limit 30 |
| Long Sessions | Songs with `AVG(playedMs) / durationMs > 0.8`, limit 25 |
| Hidden Gems | Downloaded songs with `COUNT(*) < 3`, sorted by duration |

### Line-Synced Lyrics

Lyrics sourced from a provider (LRCLIB is free and open). Each line has a timestamp offset in milliseconds. The lyrics view subscribes to `PlayerViewModel.positionMs` and highlights the current line.

```kotlin
data class LyricLine(val timestampMs: Long, val text: String)
```

Line transitions: the active line animates to `TextSize(15sp)` and `alpha(1f)`; adjacent lines animate to `13sp / 0.6f`; distant lines to `12sp / 0.35f`.

### Crossfade

Implemented in `MusicPlayerService`. At `durationMs - crossfadeDurationMs`, begin fading out the current player while starting the next track at `alpha(0f)` and fading it in. Duration configurable in Settings (0 = off, max 10s).

### Sleep Timer

A countdown timer in `MusicPlayerService`. When it expires, volume fades to 0 over 10 seconds before stopping playback. The UI shows a countdown chip in the Now Playing action row when active.

---

## Ambient Color System (v2 Redesign)

> Preview artifact: `color-redesign-preview.html` in the project root. Includes an interactive color simulator.

**The problem (solved in v2):** The `LocalAccentColor` extracted from album art was siloed to a handful of Now Playing elements. The rest of the app — Library, Home, Search, Stats — was monochromatic warm-dark with no sense that music was playing at all.

**The fix:** The album accent is now treated as *ambient light*. It bleeds through the entire app as a subtle color cast, exactly the way a light source affects everything in a room — not just the object it's pointed at. The intensity is low (12–18% opacity). The effect is cumulative: accent text + accent wash on the playing row + ambient bloom on the background = a screen that clearly belongs to the music.

### Ambient Layer — Implementation

Every screen scaffold wraps its content with an `AmbientBackground` composable:

```kotlin
@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    val accent = LocalAccentColor.current
    val animatedAccent by animateColorAsState(
        targetValue = accent,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )
    Box(
        Modifier
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
            }
    ) { content() }
}
```

When the track changes, `LocalAccentColor` updates and the 600ms `animateColorAsState` cross-fades the bloom smoothly. The whole room changes color.

### Dynamic Tint Helpers

Add to `AccentExtensions.kt` (new file):

```kotlin
@Composable fun accentAmbient()     = LocalAccentColor.current.copy(alpha = 0.15f)
@Composable fun accentWash()        = LocalAccentColor.current.copy(alpha = 0.07f)
@Composable fun accentChipBg()      = LocalAccentColor.current.copy(alpha = 0.12f)
@Composable fun accentChipBorder()  = LocalAccentColor.current.copy(alpha = 0.40f)
@Composable fun accentAlbumGroup()  = LocalAccentColor.current.copy(alpha = 0.05f)
```

### Where the accent now appears (full inventory)

| Surface | Token / usage | Opacity |
|---------|--------------|---------|
| All screen backgrounds | `AmbientBackground` bloom | 15% |
| Playing track row (all screens) | `accentWash()` as row background | 7% |
| Playing track title text | `LocalAccentColor.current` | 100% |
| Selected filter chip bg | `accentChipBg()` | 12% |
| Selected filter chip border | `accentChipBorder()` | 40% |
| Playing album (shelf) — glow ring | `accentWash()` + blur | 35% |
| Playing album (shelf) — amp bars | `LocalAccentColor.current` | 100% |
| Playing album section header | `accentAlbumGroup()` background | 5% |
| Mini player border | `LocalAccentColor.current` | 20% |
| Mini player progress line | `LocalAccentColor.current` | 90% |
| Secondary labels (count, sort) | `LocalAccentColor.current` | 70% |
| Now Playing (existing) | All existing usages | unchanged |

---

## Library — Album-First View (v2)

The Library has a new "By Album" filter mode that groups tracks under their album. This is the recommended default for libraries with more than 5 albums.

### Album-Grouped List Behavior

- **Section headers:** Rectangular album thumbnail (28×28dp, 4dp radius) + album title in DM Serif Display + DM Mono metadata (artist · year · track count). A collapse chevron on the right — tap to collapse all tracks under that album.
- **Playing album header:** Rendered with `accentAlbumGroup()` background, amber-tinted title text, amplitude bars in the thumbnail instead of artwork.
- **Track rows under each album:** Indented 40dp from left edge to visually belong to the album block. No track numbers — index is implied by position.
- **Album dividers:** A 1px `BorderSubtle` line between albums. No full-width card surfaces.
- **Collapsed state:** Shows only the header row. Album tap expands with a stagger entrance.

### Library Filter Chip Order

```
[All] [By Album] [Playlists] [Downloaded] [Moods]
```

"All" shows the flat track list (existing behavior). "By Album" shows the grouped view. The chips use `accentChipBg()` / `accentChipBorder()` when selected.

---

## Animation System (v2)

Six named animations. Each has a trigger, a duration, and Compose implementation guidance. Do not add animations outside this list without a documented trigger rationale.

### 1. Ambient Transition
- **Trigger:** Playing track changes (accent color updates)
- **Duration:** 600ms, `FastOutSlowInEasing`
- **What:** The ambient bloom in `AmbientBackground` cross-fades from old accent to new accent via `animateColorAsState`. Interruptible — if the user skips repeatedly, the color chases the latest accent without queuing.

### 2. List Stagger Entrance
- **Trigger:** Screen loads, filter chip changes, album collapses/expands
- **Duration:** 280ms per item, 30ms stagger, `FastOutSlowInEasing`
- **What:** First 5 visible items fade in and slide up 8dp. Items at index ≥ 5 appear immediately to keep scroll performance clean.
- **Critical pitfall:** Use `key = { item.id }` in `LazyColumn`, not index-based keys. Index keys re-trigger the animation on every recompose. (See prior pitfall learning: `staggered-lazyrow-key-stability`)

```kotlin
@Composable
fun TrackRow(track: Track, index: Int, modifier: Modifier = Modifier) {
    val delay = (minOf(index, 4) * 30).toLong()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(track.id) { delay(delay); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { 24 },
        modifier = modifier
    ) { /* row content */ }
}
```

### 3. Tab Slide Transition
- **Trigger:** Bottom navigation tab selection changes
- **Duration:** 280ms, `FastOutSlowInEasing`
- **What:** Content slides horizontally in the direction of the tab (left tap → right-to-left slide, right tap → left-to-right slide). Creates spatial memory — users learn the tab order through direction.

```kotlin
AnimatedContent(
    targetState = selectedTab,
    transitionSpec = {
        val dir = if (targetState.index > initialState.index) 1 else -1
        (slideInHorizontally(tween(280)) { it * dir } + fadeIn(tween(180))) togetherWith
        (slideOutHorizontally(tween(280)) { -it * dir } + fadeOut(tween(120)))
    },
    label = "tab_transition"
) { tab -> TabContent(tab) }
```

### 4. Press Feedback (Scale Pulse)
- **Trigger:** Any tappable element — track rows, chips, shelf items, album cards, buttons, nav items
- **Duration:** Spring physics (no fixed duration)
- **What:** Scale to 0.94f on press, spring back with `DampingRatioMediumBouncy` on release.

```kotlin
@Composable
fun Modifier.pressScale(): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press_scale"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { pressed = true; tryAwaitRelease(); pressed = false }
            )
        }
}
```

### 5. Album Art Bloom (Shelf)
- **Trigger:** Currently-playing album is visible in a horizontal shelf
- **Duration:** 2000ms, infinite, `RepeatMode.Reverse`
- **What:** A blurred radial gradient of the album accent pulses behind the playing album's artwork in any shelf. Scale 0.95→1.05, opacity 0.4→0.9. No bloom on non-playing albums.

```kotlin
val bloomScale by animateFloatAsState(
    targetValue = if (isPlaying) 1.05f else 0.95f,
    animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "album_bloom"
)
// Render as a Box with Modifier.graphicsLayer { scaleX = ...; scaleY = ... }
// behind the artwork Box, filled with the accent color at 35% opacity + blur
```

### 6. Download Spring Collapse
- **Trigger:** Download status transitions to `DONE`
- **Duration:** Spring physics, `StiffnessLow`, `DampingRatioMediumBouncy`
- **What:** 1.5s after completion, the waveform card spring-collapses vertically and disappears. The newly added track simultaneously enters the Library list with the Stagger Entrance animation.

```kotlin
AnimatedVisibility(
    visible = downloadStatus != DownloadStatus.DONE || showForDelay,
    exit = shrinkVertically(
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    ) + fadeOut(tween(200)),
    label = "download_collapse"
) { WaveformDownloadCard(track = track) }
```

### Animation rules

- All spring animations use `Spring.DampingRatioMediumBouncy` + `Spring.StiffnessMedium` unless specified otherwise.
- All duration-based animations use `FastOutSlowInEasing` unless specified otherwise.
- Never animate layout parameters (width, height, padding) with `tween` — always use springs or `AnimatedVisibility`/`animateContentSize`.
- No animation should block user input. Every animation is interruptible.

---

## Design Preview

The full interactive design preview is at `design-preview.html` in the project root. Open in any browser — no server required.

The **color redesign** preview is at `color-redesign-preview.html`. Includes a live album-color simulator and before/after comparison of every ambient color change. Open this one first to understand the v2 direction.

It includes all screens, live animations (waveform frontier, amplitude bars, breathing glow, shimmer), before/after comparisons, and the full feature overview.
