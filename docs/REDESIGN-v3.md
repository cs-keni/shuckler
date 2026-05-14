# Redesign v3 — Post-Launch Feedback Pass

Date: 2026-05-14  
Based on: Kenny's first full visual review after Phases 1–4 shipped

---

## Context

Phases 1–4 of the v2 Ambient Color System are complete. The app is building and running. This document captures the next wave of improvements from Kenny's hands-on testing session, scoped into actionable implementation phases with full file-level context for either agent.

---

## Summary of Feedback

| Tab | Issue | Priority |
|-----|-------|----------|
| Global | Font feels too blocky (DM Serif Display) | High |
| Global | Ambient bloom has no baseline — goes black without music | High |
| Global | Tab slide is too bouncy | Medium |
| Home | Bloom origin points don't align (scattered light sources) | Medium |
| Home | Empty black gap at bottom of scroll | Low |
| Home | Glow fades when scrolled past — should persist like fireflies | Medium |
| Search | Preview button looks identical to Play — remove it | High |
| Now Playing | Accent bloom Box corners are sharp | Medium |
| Downloads | Can't play music while downloading | High |
| Downloads | Only one download at a time; should queue multiple | High |
| Library | Orange-tipped corners on track rows (visual bug) | High |
| Library | Feels cramped | Medium |
| Library | Albums / Playlists / "Your Library" should be tappable nav headers | Medium |
| Library | "Your Library" should be renamed "Downloads" | Low |
| Stats | Too dim and low-color | Medium |
| Stats | Wants pie/donut charts | Medium |
| Stats | More achievements + easter eggs | Low |
| Stats | Stats should be tappable/interactive | Low |

---

## Phase 1 — Quick Wins (No Architecture Change)

These are isolated, low-risk changes with no API surface impact.

### 1.1 Remove Preview Button from Search

**Problem:** Preview and Play buttons look identical. User never uses Preview.  
**File:** `app/src/main/java/com/shuckler/app/ui/SearchScreen.kt`

Search results render via a composable (around line 845) that accepts `onPreviewClick`, `onStopPreviewClick`, `isPreviewing`, and `previewDurationMs` params. The `PreviewPlayer` infrastructure in `com.shuckler.app.preview` can stay — just hide the button in the UI.

**Change:** In the search result card composable, remove the Preview/Stop-Preview button row. Remove `onPreviewClick`, `onStopPreviewClick`, `isPreviewing`, `previewDurationMs` from the composable signature if they are only used by the button. The `PreviewPlayer.stop()` call in `onPlayClick` (line ~523) should stay so playback is cancelled when the user hits Play.

**Gotcha:** The composable is called twice in SearchScreen — once for YouTube results and once for Spotify imports (around lines 468 and 562). Update both call sites when removing the params.

### 1.2 Fix Now Playing Accent Bloom Corner Radius

**Problem:** The accent bloom `Box` behind the glow ring uses `RoundedCornerShape(24.dp)` but the album art thumbnail uses `RoundedCornerShape(18.dp)`. The bloom bleeds out with sharper corners, creating an ugly mismatch.  
**File:** `app/src/main/java/com/shuckler/app/ui/PlayerScreen.kt`

**Change:** Set the bloom `Box` to `RoundedCornerShape(18.dp)` to match the art. This change is one line in the `PlayerScreen` bloom block added in commit `0a7a130`.

### 1.3 Fix Library Orange Corner Bug

**Problem:** Track rows in Library have orange-tipped corners — looks like the `SwipeToDismissBox` background (red/orange delete indicator) is bleeding through the card's rounded corners.  
**File:** `app/src/main/java/com/shuckler/app/ui/LibraryScreen.kt`

**Root cause:** `SwipeToDismissBox` renders a full-bleed colored background behind the swiped item. If the track row has `clip(RoundedCornerShape(...))`, the clipping happens on the foreground but not on the dismiss background container, so the background color peeks through the rounded corners.

**Fix options:**
- Wrap the entire `SwipeToDismissBox` (not just the content) in a `Box` with `clip(RoundedCornerShape(12.dp))` so the dismiss background is also clipped.
- Or apply `Modifier.clip(...)` on the `SwipeToDismissBox` itself rather than just on the inner content composable.

### 1.4 De-bounce Tab Slide Transition

**Problem:** The tab slide uses `DampingRatioMediumBouncy` which causes noticeable overshoot when switching tabs. Feels playful but unwanted here.  
**File:** `app/src/main/java/com/shuckler/app/navigation/NavGraph.kt`

**Lines to change:** NavGraph.kt uses `DampingRatioMediumBouncy` in multiple `spring()` calls (lines ~245, ~338, ~395–396). Also has `tween(600)` on line ~168 for ambient color cross-fade (keep that one — it controls color, not slide).

**Change:** Replace `DampingRatioMediumBouncy` with `DampingRatioNoBouncy` or `DampingRatioLowBouncy` for the `slideInHorizontally`/`slideOutHorizontally` animation spec. Keep the nav dot spring separate if desired.

```kotlin
// Before
spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)

// After — smooth deceleration, no overshoot
tween(durationMillis = 280, easing = FastOutSlowInEasing)
// or if spring feel is still desired:
spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
```

---

## Phase 2 — Typography Overhaul

### 2.1 Replace DM Serif Display with Plus Jakarta Sans

**Problem:** DM Serif Display is elegant but has serifs and narrow letterforms that read as blocky on dense UI at small sizes. User wants something more modern and bubbly.

**Recommendation: Plus Jakarta Sans** — rounded letterforms, variable weight, designed for screens, slightly playful without being childish. Perfect for a music app. Widely used in modern fintech/consumer apps. Available on Google Fonts.

**Alternatives considered:**
- *Nunito* — very round but can read as childish at display sizes
- *Figtree* — clean and modern, slightly less personality than Plus Jakarta Sans
- *Outfit* — geometric, modern, but thinner at default weight (may wash out on dark bg)

**File:** `app/src/main/java/com/shuckler/app/ui/theme/Type.kt`

**Change:** Add `PlusJakartaSans` FontFamily using the same Google Fonts provider pattern. Replace `DmSerifDisplay` with `PlusJakartaSans` in all display/title/headline `TextStyle` entries. Adjust `fontWeight` — Plus Jakarta Sans has a broader weight range, so use `FontWeight.SemiBold` (600) for headers and `FontWeight.Bold` (700) for display to maintain visual hierarchy that DM Serif Display achieved through its contrasting strokes.

```kotlin
@OptIn(ExperimentalTextApi::class)
val PlusJakartaSans = FontFamily(
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Plus Jakarta Sans"), fontProvider = provider, weight = FontWeight.Bold),
)
```

**Everywhere `DmSerifDisplay` is referenced in Type.kt:**
- `displayLarge` → PlusJakartaSans, SemiBold
- `headlineLarge` → PlusJakartaSans, SemiBold
- `headlineMedium` → PlusJakartaSans, SemiBold
- `headlineSmall` → PlusJakartaSans, Medium
- `titleLarge` → PlusJakartaSans, SemiBold
- `titleMedium` → PlusJakartaSans, Medium
- `titleSmall` → PlusJakartaSans, Medium

**DM Mono stays.** It's used for timestamps, metadata, bitrate, track numbers — monospace is still correct there. The contrast between Plus Jakarta Sans (bubbly, rounded, warm) and DM Mono (crisp, technical, systematic) will be excellent.

**Hardcoded font overrides to check:** Some composables may pass `fontFamily = DmSerifDisplay` directly (e.g., `AlbumGroupHeader`, `EmptyState`, screen section headers). Search for `DmSerifDisplay` across all UI files and replace with `PlusJakartaSans`.

```bash
grep -rn "DmSerifDisplay" app/src/main/java/com/shuckler/app/ui/
```

---

## Phase 3 — Ambient Bloom Improvements

### 3.1 Persistent Warm Baseline Glow (No Music Playing)

**Problem:** When no track is playing, `LocalAccentColor` falls back to a neutral default and the ambient bloom is invisible — the app feels cold and empty on first open.

**Goal:** A warm amber/gold baseline bloom that's always present, getting brighter when music plays and the accent color takes over.

**File:** `app/src/main/java/com/shuckler/app/navigation/NavGraph.kt`

**Current behavior:** The ambient radial gradient bloom uses `animatedAccent` (animated `LocalAccentColor`). When no track plays, this color is whatever the `LocalAccentColor` default is (likely `Color.Transparent` or a cold neutral).

**Fix:** Define a `WarmBaseline` color (a muted amber: `Color(0xFFB07A30).copy(alpha = 0.08f)`) and blend it with the accent color:

```kotlin
val hasTrack = currentTrack != null
val bloomColor = if (hasTrack) animatedAccent.copy(alpha = 0.14f) 
                 else Color(0xFFB07A30).copy(alpha = 0.06f)
```

The baseline should be subtle — just enough to warm the background so it never looks like a cold void.

**Alternative approach:** Use `animateColorAsState` to cross-fade between the warm baseline and the album accent as a track starts/stops. This avoids the hard cut and feels more alive:

```kotlin
val targetBloom = if (currentTrack != null) 
    LocalAccentColor.current.copy(alpha = 0.14f)
else 
    Color(0xFFB07A30).copy(alpha = 0.06f)
val bloomColor by animateColorAsState(targetBloom, animationSpec = tween(800))
```

### 3.2 Unified Bloom Origin (Home Tab Alignment)

**Problem:** The ambient bloom on the Home tab appears to have multiple origin points rather than a single coherent light source, making the background feel scattered.

**File:** `app/src/main/java/com/shuckler/app/navigation/NavGraph.kt` + `HomeScreen.kt`

**Diagnosis needed:** Check whether Home has its own `Box` with a radial gradient, or if it inherits only from the NavGraph bloom. If Home has multiple bloom sources, consolidate them to a single origin point — `Offset(0.5f, 0.0f)` (top center) is the natural choice for a music player (album art sits at top, light radiates down).

**Change:** Ensure the NavGraph ambient bloom uses a single `RadialGradiant` with `center = Offset(width * 0.5f, 0f)` and a radius that covers approximately the top 60% of the screen. Remove any additional bloom `Box` composables added inside `HomeScreen` that create competing origins.

### 3.3 Scroll-Persistent Glow on Home (Firefly Effect)

**Problem:** The bloom/glow on the Home tab fades away when the user scrolls past the album art — they want a persistent warm ambient atmosphere regardless of scroll position.

**Current behavior:** The bloom is likely tied to the `LazyColumn` header or to an element that exits the composition during scroll.

**Fix:** The ambient bloom must live *outside* the `LazyColumn` — it should be in the screen's outer `Box` or `Scaffold` background, not inside the scrollable content. If the bloom is currently drawn as a `Box` inside a `Column` that is inside the `LazyColumn`, move it to the `Scaffold` background layer (same `Box` that contains the `LazyColumn`, sibling not child).

The bloom should never scroll — it's a screen-level atmosphere, not a content element.

---

## Phase 4 — Library Reorganization

This is the most structural change in v3. The current Library is a single scrollable surface mixing browse and storage. The new model gives Albums, Playlists, and Downloads their own navigation destinations.

### 4.1 Rename "Your Library" → "Downloads"

Simple rename everywhere "Your Library" appears in `LibraryScreen.kt`.

### 4.2 Albums, Playlists, Downloads as Tappable Nav Headers → Dedicated Pages

**Current structure:**  
Library screen has filter chips (All / By Album / Favorites) and a scrollable list that includes everything. "Your Library" / "Albums" / "Playlists" are section headers within the same scrollable surface.

**New structure:**
```
Library (root)
  ├── Albums [header → AlbumBrowseScreen or modal sheet]
  ├── Playlists [header → PlaylistBrowseScreen or modal sheet]  
  └── Downloads [header → DownloadsScreen or modal sheet]
```

**Implementation options:**

**Option A — Bottom sheets (lighter lift):** Tapping "Albums" opens a `ModalBottomSheet` showing the album-grouped view. Same for Playlists and Downloads. No new nav destinations needed. Library root becomes a quick-access hub.

**Option B — Nav graph destinations (heavier, more correct):** Add `Route.Albums`, `Route.Playlists`, `Route.Downloads` to the nav graph. Library root shows three prominent tappable cards/headers instead of a flat list. Back arrow returns to Library root.

**Recommendation: Option A (Bottom Sheets)** for this pass — avoids nav graph changes, back stack complexity, and shared-element setup. Can graduate to Option B in Phase 5 if needed.

**Files to change:**
- `LibraryScreen.kt` — replace section headers with tappable rows that open sheets
- New composables: `AlbumBrowseSheet`, `PlaylistBrowseSheet`, `DownloadsSheet` (can be extracted from existing LibraryScreen sections)

### 4.3 Fix Cramped Feeling

Current track rows have insufficient vertical padding. Increase `LibraryTrackItem` vertical content padding from the current value to `14.dp` (top+bottom). Add `spacedBy(2.dp)` between items in the `LazyColumn`. This gives the list room to breathe without wasting space.

---

## Phase 5 — Downloads: Background Queue + Play During Download

### 5.1 Play During Download

**Problem:** Starting playback while a download is in progress likely cancels/conflicts with the download.

**File:** `app/src/main/java/com/shuckler/app/download/DownloadManager.kt`

**Investigation needed:** Check whether `DownloadManager` streams audio via `ExoPlayer` or writes to disk independently. If it streams via a shared player instance, that's the conflict. If it writes to disk via `OkHttp`/`OkHttpDownloader`, playback should be independent.

**Fix approach:** Ensure the download HTTP client (`OkHttpDownloader`) is separate from the playback `ExoPlayer` instance. Downloads write to disk; the player reads from disk (for completed tracks) or streams from YouTube (for on-demand plays). These should not share state.

If there's a lock or single-player assumption, the download should run in a `Service` with its own `OkHttpClient` coroutine scope, completely independent of playback.

### 5.2 Background Download Queue (Multiple Concurrent Downloads)

**Problem:** Only one download runs at a time; subsequent downloads block.

**File:** `app/src/main/java/com/shuckler/app/download/DownloadManager.kt`

**Current pattern:** Likely a single `Flow`-based download that processes one item. 

**Change:** Replace the single-item flow with a `Channel<DownloadRequest>` that feeds a pool of coroutine workers (2 concurrent downloads is a good max — more risks throttling from YouTube). Use `produce` or a `MutableStateFlow<List<DownloadProgress>>` keyed by video ID.

```kotlin
// Conceptual change
private val downloadQueue = Channel<DownloadRequest>(capacity = Channel.UNLIMITED)
private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())

// Launch N worker coroutines in DownloadManager init
repeat(2) {
    scope.launch {
        for (request in downloadQueue) {
            processDownload(request)
        }
    }
}
```

**SearchScreen.kt** already uses a `SnapshotStateMap` (`lingeringProgress`) that supports multiple simultaneous cards — it just needs the underlying data to emit multiple concurrent entries.

---

## Phase 6 — Stats: Color, Charts, Achievements

### 6.1 More Color and Saturation

Stats currently renders with low-alpha accent colors that wash out on the dark background. Increase accent usage:
- Section headers should use `LocalAccentColor.current` at 100% for the icon/indicator dot
- Metric callout numbers (total plays, total time) should render in the accent color, not Text1
- The inline accent panel should have a stronger `accentWash()` background (bump alpha from 0.07f to 0.14f)

**File:** `app/src/main/java/com/shuckler/app/ui/AnalyticsScreen.kt`

### 6.2 Pie / Donut Charts

**Goal:** Replace the ranked artist bar section with a donut chart showing listening distribution by artist (top 5 + "Other").

**Implementation:** Android has no built-in chart composable. Options:
- **Vico** (`com.patrykandpatrick.vico`) — Compose-native, well-maintained, donut/pie support. Add to `libs.versions.toml`.
- **Custom `Canvas` donut** — draw arcs manually with `drawArc()`. More control, zero dependency.

**Recommendation:** Custom `Canvas` donut for the v3 pass — Vico adds ~600KB and dependency risk for what is essentially a ring of colored arcs. The donut only needs: arc per artist, accent color for #1, muted warm tones for the rest, center label showing the top artist name.

```kotlin
Canvas(modifier = Modifier.size(160.dp)) {
    val total = segments.sumOf { it.value }.toFloat()
    var startAngle = -90f
    segments.forEach { seg ->
        val sweep = (seg.value / total) * 360f
        drawArc(color = seg.color, startAngle = startAngle, sweepAngle = sweep, 
                useCenter = false, style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round))
        startAngle += sweep
    }
}
```

### 6.3 Interactive / Tappable Stats

**Goal:** Tapping a stat row or donut segment drills into detail.

**Light implementation:** Tapping an artist row navigates to `ArtistDetailScreen`. Tapping the play-count number opens a `ModalBottomSheet` showing the per-day breakdown as a bar chart (again, custom `Canvas`).

Use `Modifier.clickable` on existing stat rows and artist rows; pass `onNavigateToArtist` down from the screen to the composable.

### 6.4 More Achievements + Easter Eggs

**File:** `app/src/main/java/com/shuckler/app/achievement/AchievementManager.kt`

Add achievement triggers:
- `NIGHT_OWL` — listened past midnight 3+ times
- `MARATHON` — listened >4 hours in one day  
- `GENRE_HOPPER` — played 5+ different genres in one session
- `FIRST_DOWNLOAD` — first track saved offline
- `LIBRARY_HUNDRED` — 100 tracks in library
- `SPEED_RUNNER` — skipped 10 songs in 5 minutes (easter egg: "You okay?" toast)

Easter egg: If the user plays the same song 10 times in a row, show a fun `Snackbar` or animated overlay — "ok we get it, you love this song 😭" (with the crying emoji — this one warrants the exception).

### 6.5 Listening Streak

Track consecutive days where the user listened for at least 15 minutes. Display as a flame/streak chip on the Stats screen header (e.g., "🔥 7-day streak"). Reset if a day is skipped.

Store `lastListenDateEpochDay` and `currentStreakDays` in DataStore/SharedPreferences alongside existing stats. Increment on session end if the current epoch day differs from last.

This alone dramatically increases retention — streaks are one of the highest-leverage engagement mechanics.

### 6.6 Shareable Stats Card

Generate a screenshot-ready summary card in Stats: album art mosaic of top 5 tracks, total play time, top artist, streak, and the app name — all on a branded dark background with accent color. Render via `AndroidView` wrapping a `ComposeView` at fixed 1080×1080 resolution, then use `Bitmap.createBitmap` + `Canvas.drawBitmap` to export. Share via `ShareCompat`.

---

## Phase 7 — Feel & Micro-interactions

This phase makes the app feel *premium*. None of these are features — they're sensory feedback loops that make every interaction satisfying. High impact, medium complexity.

### 7.1 Haptic Feedback

**File:** `app/src/main/java/com/shuckler/app/ui/PlayerScreen.kt` + `LibraryScreen.kt` + `SearchScreen.kt`

Add `HapticFeedbackType` calls at key moments:
- **Play/Pause toggle** — `haptic.performHapticFeedback(HapticFeedbackType.LongPress)` (satisfying "click" sensation)
- **Download complete** — `HapticFeedbackType.LongPress` on the card spring-collapse
- **Achievement unlock** — double pulse (two `HapticFeedbackType.LongPress` 100ms apart)
- **Track favorited** — `HapticFeedbackType.LongPress`
- **Swipe-to-delete confirm** — `HapticFeedbackType.LongPress`

In Compose: `val haptic = LocalHapticFeedback.current` — no permissions needed, uses existing system APIs.

### 7.2 Swipe Up from Mini Player → Now Playing

**Files:** `MiniPlayerBar.kt`, `NavGraph.kt`

The mini player pill should respond to an upward drag gesture that opens the Now Playing screen. This is a fundamental music app UX pattern (Spotify, Apple Music both do it).

**Implementation:** Add `Modifier.pointerInput(Unit)` detecting a vertical swipe (velocity > 500 px/s upward) on the `MiniPlayerBar` composable. On trigger, navigate to the Now Playing route.

```kotlin
.pointerInput(Unit) {
    detectVerticalDragGestures(
        onDragEnd = { /* navigate if velocity was upward */ }
    ) { _, dragAmount ->
        if (dragAmount < -80f) { onNavigateToPlayer() }
    }
}
```

### 7.3 Swipe Down to Dismiss Now Playing → Mini Player

**File:** `PlayerScreen.kt`

Complement to 7.2 — swipe down on the Now Playing screen collapses it back to the mini player. Use `detectVerticalDragGestures` on the root `Column`. If drag exceeds 120dp downward, call `navController.navigateUp()`. Add a `graphicsLayer { translationY = dragOffset }` visual preview during the drag so it feels physically attached.

### 7.4 Mini Player Progress Bar

**File:** `MiniPlayerBar.kt`

Add a thin (2dp) accent-colored line at the bottom edge of the mini player pill, showing track progress (0% → 100%). Use `LinearProgressIndicator` with `progress = positionMs / durationMs` and `color = LocalAccentColor.current`. This is the most elegant way to show playback position without adding any cognitive load.

```kotlin
LinearProgressIndicator(
    progress = { (positionMs / durationMs.toFloat()).coerceIn(0f, 1f) },
    modifier = Modifier
        .fillMaxWidth()
        .height(2.dp)
        .align(Alignment.BottomCenter)
        .clip(CircleShape),
    color = LocalAccentColor.current,
    trackColor = Color.Transparent
)
```

### 7.5 Skeleton Loading States for Search

**File:** `SearchScreen.kt`

Replace the loading spinner on search results with shimmer skeleton cards — same height/layout as result cards, animated shimmer gradient sweeping left-to-right. The existing `Shimmer.kt` composable is already in the project; wire it up to the search loading state.

This makes the app feel significantly faster — perceived performance improves even when actual latency is unchanged.

### 7.6 Long-Press Context Menu on Tracks

**Files:** `LibraryScreen.kt`, `SearchScreen.kt`, `PlaylistScreen.kt`

Long-pressing any track should open a `ModalBottomSheet` with quick actions:
- Play next (insert after current in queue)
- Add to queue (append)
- Add to playlist (submenu or dialog)
- Share (YouTube link)
- Delete (Library only)

This removes clutter from the visible UI while giving power users direct access to the actions they need. Use `Modifier.combinedClickable(onLongClick = { showMenu = true })`.

### 7.7 Undo Snackbar for Track Deletion

**File:** `LibraryScreen.kt`

After swipe-to-delete in Library, immediately remove the track from the list but show a `Snackbar` with "Undo" for 4 seconds. If Undo is tapped, re-insert the track at its original position. If the Snackbar dismisses naturally, commit the deletion.

Pattern: keep a `deletedTrack: DownloadedTrack?` state variable. On swipe, set `deletedTrack = track` and remove from list. On `LaunchedEffect(deletedTrack)`, delay 4s then actually delete from disk/DB if still non-null.

### 7.8 Sleep Timer Countdown Chip in Now Playing

**File:** `PlayerScreen.kt`

Already on the Phase 5 wishlist in `CURRENT_TASK.md`. When a sleep timer is active, show a small chip in the Now Playing action row: `⏱ 23:14` counting down. The `sleepTimerRemainingMs` state is already available. Tapping the chip cancels the timer.

---

## Phase 8 — Playback & Queue UX

### 8.1 Loading State When Fetching Stream

**File:** `PlayerScreen.kt` + `MiniPlayerBar.kt`

When the user taps Play on a streamed (not downloaded) track, there's a delay while fetching the stream URL from YouTube. Currently this is silent — the UI looks like nothing happened. Show a subtle loading indicator:
- In MiniPlayerBar: replace the play/pause icon with a `CircularProgressIndicator` while `isLoading = true`
- In PlayerScreen: same on the main play button

This prevents users from tapping Play multiple times thinking it didn't register.

### 8.2 Offline / Downloaded Badge on Tracks

**File:** `LibraryScreen.kt`, `SearchScreen.kt`

In Search results, if a track is already in the Library (matched by video ID or title+artist), show a small "Downloaded" chip or a download-complete icon instead of the download button. This is partially implemented per `QOL.md` — verify coverage is complete.

In Library, distinguish streamed-without-download tracks (if any) from fully downloaded ones with a small icon.

### 8.3 Gapless Playback Verification

**File:** `app/src/main/java/com/shuckler/app/player/`

ExoPlayer supports gapless playback when tracks are pre-loaded into the `MediaItem` queue. Verify that the next track's `MediaItem` is queued before the current track ends (at least 15 seconds before). If not, pre-fetch the stream URL and add to the `ExoPlayer` queue. Silent win — no UI change needed.

---

## Implementation Order

For maximum momentum with minimum risk, implement in this order:

```
Phase 1 — Quick wins (each is isolated, ~30min each)
  1.1  Remove Preview button (SearchScreen)
  1.2  Fix Now Playing bloom corners (PlayerScreen)
  1.3  Fix Library orange corners (LibraryScreen SwipeToDismissBox)
  1.4  De-bounce tab slide (NavGraph)

Phase 2 — Typography (affects all screens visually but is one file change + font name grep)
  2.1  Add PlusJakartaSans to Type.kt, replace DmSerifDisplay everywhere

Phase 3 — Ambient bloom polish (NavGraph + HomeScreen, medium complexity)
  3.1  Warm baseline glow (no music state)
  3.2  Unified origin alignment
  3.3  Scroll-persistent glow

Phase 4 — Library reorganization (medium complexity, bottom-sheet approach)
  4.1  Rename "Your Library" → "Downloads"
  4.2  Albums/Playlists/Downloads as sheet-launching headers
  4.3  Increase LibraryTrackItem spacing

Phase 5 — Downloads background queue (higher complexity, touches DownloadManager)
  5.1  Verify play-during-download works (may be free)
  5.2  Multi-download queue (coroutine channel approach)

Phase 6 — Stats (additive, low risk)
  6.1  Boost accent color saturation
  6.2  Donut chart (Canvas, no dep)
  6.3  Tappable stats (clickable + nav)
  6.4  New achievements
  6.5  Listening streak (DataStore)
  6.6  Shareable stats card

Phase 7 — Feel & Micro-interactions (high impact, mostly additive)
  7.1  Haptic feedback (play/pause, download complete, achievement)
  7.2  Swipe up mini player → Now Playing
  7.3  Swipe down Now Playing → mini player
  7.4  Mini player progress bar
  7.5  Skeleton loading in Search
  7.6  Long-press context menu on tracks
  7.7  Undo snackbar for track deletion
  7.8  Sleep timer countdown chip in Now Playing

Phase 8 — Playback & queue UX
  8.1  Loading state when fetching stream URL
  8.2  Downloaded badge on Search results
  8.3  Gapless playback verification
```

---

## Gotchas / Notes

- **mutableStateSetOf** is not available in this Compose runtime — continue using `mutableStateOf(emptySet<String>())` with `+`/`-` set operations. Confirmed fix in commit `4e90696`.
- **WSL Gradle blocked** — build in Android Studio. Kotlin type errors will only surface there.
- **Font loading is async** — Plus Jakarta Sans via Google Fonts provider will fall back to system-default on first load. This is expected; the font caches after first run. No fallback font needs to be embedded unless offline font support is required.
- **OkHttpDownloader** — check whether it shares a connection pool with the ExoPlayer streaming client before assuming downloads and playback are independent.
- **pressScale + clickable** — the `awaitFirstDown(requireUnconsumed = false)` pattern is critical. Don't revert to `detectTapGestures(onPress)` which consumes events and breaks clickable.
- **Haptic feedback** — `LocalHapticFeedback.current` is available in any composable, no permissions required. Prefer `HapticFeedbackType.LongPress` over `TextHandleMove` — more reliable across manufacturers.
- **Swipe gestures on mini player** — test on physical device. The pill lives inside a `NavigationBar` container; touch events may need `Modifier.zIndex(1f)` + `consumeWindowInsets` to propagate correctly.
- **QOL.md** — `docs/QOL.md` has a broader brainstorm backlog. Items in this doc represent the prioritized slice; everything else lives there for future passes.
