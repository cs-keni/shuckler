# Shuckler - Development Phases

This document breaks down the Shuckler Android music app development into incremental, testable phases. Each phase should be completed and tested before moving to the next.

---

## Phase 1: Project Setup & Basic UI Foundation
**Goal:** Create the Android project structure and basic UI screens

### Tasks:
1. Create Android Studio project
   - App name: Shuckler
   - Package: com.shuckler.app (or your preferred package)
   - Min SDK: Android 10 (API 29)
   - Target SDK: Latest (API 36 - Android 16 "Baklava") - since targeting S22 Ultra/S25
   - Language: Kotlin

2. Configure build.gradle
   - Add necessary dependencies (Media3, Coroutines, etc.)
   - Set up project structure

3. Create basic UI layouts
   - `activity_main.xml` - Main screen with tabs/fragments
   - `fragment_search.xml` - Search input and results list
   - `fragment_library.xml` - List of cached/downloaded tracks
   - `fragment_player.xml` - Player controls (play, pause, next, prev, loop)

4. Create MainActivity.kt
   - Set up basic navigation between fragments
   - Handle fragment lifecycle

5. Add app icon
   - Place SVG icon in `res/drawable/`
   - Configure in AndroidManifest.xml

### Testing:
- [x] App launches without crashes
- [x] Can navigate between Search, Library, and Player screens
- [x] UI elements are visible and properly laid out

### Deliverables:
- Working Android project
- Basic UI screens (no functionality yet)
- Project structure ready for next phase

---

## Phase 2: Basic Audio Playback (Local File)
**Goal:** Play a local MP3 file using MediaPlayer/Media3

### Tasks:
1. Add a test MP3 file to `res/raw/` or assets folder ✅
2. Create basic `MusicPlayer` class
   - Use ExoPlayer (Media3) or MediaPlayer
   - Implement play/pause functionality
   - Handle audio focus
3. Integrate player into PlayerFragment
   - Connect play/pause buttons
   - Display current track info (placeholder)
4. Request necessary permissions
   - Update AndroidManifest.xml with permissions
   - Handle runtime permission requests

### Testing:
- [x] Can play a local MP3 file
- [x] Play/pause buttons work
- [x] Audio plays through device speakers/headphones
- [x] App handles audio focus (pauses when phone call comes in)

### Deliverables:
- Working audio playback from local file
- Basic player controls functional

---

## Phase 3: Foreground Service for Background Playback
**Goal:** Music continues playing when screen is off

### Tasks:
1. Create `MusicPlayerService.kt`
   - Extend Service or use MediaSessionService
   - Implement foreground service with notification
   - Move playback logic from MusicPlayer to service
2. Create persistent notification
   - Basic notification with play/pause action
   - Show current track name
3. Update MainActivity
   - Bind to service
   - Start service when playback begins
4. Handle service lifecycle
   - Start/stop service appropriately
   - Handle app being killed

### Testing:
- [x] Music continues playing when screen turns off
- [x] Music continues when app is minimized
- [x] Notification appears and shows playback controls
- [x] Service survives app being swiped away (optional, depends on device)

### Deliverables:
- Background audio playback working
- Foreground service with notification

---

## Phase 4: MediaSession & System Controls
**Goal:** Headphone buttons, lock screen, and notification controls work

### Tasks:
1. Implement MediaSession ✅
   - Create MediaSession in MusicPlayerService
   - Set metadata (title, artist, album art)
   - Handle media button events
2. Update notification ✅
   - Add previous/next buttons
   - Add seek bar (optional for Phase 4)
   - Style notification as media notification
3. Implement MediaSession callbacks ✅
   - onPlay, onPause, onSkipToNext, onSkipToPrevious
   - onSeekTo (optional)
4. Test with headphones
   - Verify play/pause button works
   - Verify next/previous buttons work (if supported)

### Testing:
- [ ] Lock screen shows media controls
- [ ] Headphone play/pause button works
- [ ] Notification controls (play, pause, next, prev) work
- [ ] MediaSession properly reports playback state

### Deliverables:
- Full system media control integration
- Headphone button support
- Lock screen controls

---

## Phase 5: Download Functionality (Basic)
**Goal:** Download audio from a URL (start with direct MP3 link, then YouTube)

### Tasks:
1. Create `DownloadManager.kt` ✅
   - Handle file downloads
   - Save to app-specific storage (`getExternalFilesDir()` or `getFilesDir()`)
   - Show download progress
2. Create download data model ✅
   - Track metadata (title, artist, file path, duration)
   - Track download status
3. Implement storage management ✅
   - Create app-specific audio directory
   - Handle file naming conflicts
   - Store metadata (use SharedPreferences or simple JSON for now)
4. Add download UI ✅
   - Download button in search results
   - Progress indicator
   - Download status in library

### Testing:
- [x] Can download a direct MP3 URL
- [x] File saves to app storage
- [x] Download progress is visible
- [x] Downloaded file can be played

### How to test Phase 5 (direct MP3 URLs)
- **Not a YouTube search:** Phase 5 is "Download from URL" — you paste a **direct link** to an MP3 file. YouTube search/download is Phase 6.
- **What to enter:** A URL that points directly to an audio file (e.g. ends in `.mp3` or returns audio when opened in a browser). Examples of how to get one:
  - Search the web for "free sample mp3 download" or "test mp3 url" and use a link that goes straight to the file (e.g. from a CDN or audio host).
  - Example test URLs (may change; use any public direct MP3 link):  
    - `https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3` (sample music)  
    - Any `.mp3` link from a site like Internet Archive, Freesound, or a podcast feed.
- **Title / Artist:** Optional; if you leave them blank, the app uses "Track (id)" and "Unknown".

### Deliverables:
- Working download system for direct audio URLs
- Files stored in app-specific directory
- Basic download UI

---

## Phase 6: YouTube Integration & Search
**Goal:** Search YouTube and download audio using yt-dlp or similar

### Tasks:
1. Research YouTube audio extraction ✅
   - Option A: Use yt-dlp via ProcessBuilder (requires root or external tool)
   - Option B: Use YouTube API + audio extraction library
   - Option C: Use a third-party library (NewPipe extractor, etc.)
   - **Implemented:** NewValve (OkHttp + NewPipe Extractor)
2. Implement YouTube search ✅
   - Search YouTube for queries
   - Display results (title, thumbnail, duration)
3. Implement YouTube download ✅
   - Extract audio URL or download directly
   - Convert to MP3 (if needed)
   - Save with proper metadata
4. Integrate with UI ✅
   - SearchFragment shows YouTube results
   - Download button triggers download
   - Show download progress

### Testing:
- [x] Can search YouTube
- [x] Search results display correctly
- [x] Can download audio from YouTube
- [x] Downloaded audio plays correctly
- [ ] Works with long compilations (1-2 hours)

### Deliverables:
- YouTube search functionality
- YouTube audio download
- Integration with existing download system

---

## Phase 7: Library & Cache Management
**Goal:** Display downloaded tracks, manage storage

### Tasks:
1. Create LibraryFragment UI ✅
   - List of downloaded tracks
   - Display track info (title, duration, file size)
   - Play button for each track
2. Implement track database/storage ✅
   - Store track metadata (use Room database or simple JSON)
   - Track file paths, download dates, play counts
3. Implement library playback ✅
   - Select track from library
   - Play selected track
   - Queue management (optional: play next)
4. Add storage management UI ✅
   - Show total storage used
   - Delete individual tracks
   - Clear all cache option
   - Storage awareness: show per-track file size in Library; show available device space; before starting a download, when size is known, check available space and warn or fail with a clear message if insufficient (so users don’t fill storage with e.g. an 8-hour song)
5. Improve download progress display (Search screen) ✅
   - Show download speed (e.g. MB/s or Mbps) next to progress for active downloads
   - Active downloads section is visible at top of Search so progress is visible without switching tabs

### Testing:
- [x] Library shows all downloaded tracks
- [x] Can play tracks from library
- [x] Can delete tracks
- [x] Storage usage is accurate

### Deliverables:
- Functional library screen
- Track management (view, play, delete)
- Storage tracking

---

## Phase 8: Favorites (optional auto-delete)
**Goal:** Let users mark favorites and optionally auto-delete tracks after playback.

**Default behavior:** Downloaded tracks **stay in the library** after you play them. Users delete manually (Library delete button) when they don’t want a track. No automatic removal.

**Optional:** A setting can enable “auto-delete after playback (except favorites)” for users who want to free space automatically—e.g. listen once, then remove unless favorited.

### Tasks:
1. Add favorites functionality ✅
   - Favorite button on tracks (Library and optionally Player)
   - Store favorite status in metadata/database
   - Filter or sort library by favorites (e.g. “Favorites” filter)
2. Optional auto-delete (off by default) ✅
   - Setting: “Delete track after playback unless it’s a favorite” (default: off)
   - When enabled: track playback completion → delete track if not favorite
   - Don’t delete currently playing track; don’t delete if user manually stopped
3. Update UI ✅
   - Favorite indicator (e.g. heart icon) in library
   - Settings screen or dialog: toggle for auto-delete, optional delay

### Testing:
- [x] Can mark/unmark favorites
- [x] Favorites persist across app restarts
- [x] With auto-delete off (default): tracks remain after playback
- [x] With auto-delete on: non-favorite tracks are removed after playback; favorites are never auto-deleted
- [x] Currently playing track is not deleted

### Deliverables:
- Favorites system
- Optional auto-delete (off by default)
- Settings for auto-delete

---

## Phase 9: Polish & Optimization
**Goal:** Improve UX, optimize for long compilations, add remaining features

### Tasks:
1. Implement loop functionality ✅
   - Single track loop
   - Queue/playlist loop
   - UI toggle for loop mode
2. Optimize for long compilations
   - Efficient storage (check compression options)
   - Progress tracking for long downloads
   - Seek functionality in player ✅
3. Improve UI/UX
   - **App icon:** Use project assets `shuckle.png` / `shuckle.svg` as the app launcher icon ✅
   - Better loading states
   - Error handling and user feedback
   - Dark mode support (optional)
   - Responsive layouts
   - **Preview before download:** In Search (YouTube results), let the user play a short preview (e.g. 30–60 seconds) of a track without downloading. If they like it, they can tap Download to get the full file. (Stream preview from YouTube audio URL, then stop after N seconds; no file saved until Download is pressed.)
4. Add seek bar ✅
   - Show playback progress
   - Allow seeking to position
5. Audio quality options
   - Configurable bitrate (192-320 kbps)
   - Quality selector in download options
   - Investigate/fix any playback glitches (ticking, stutter) if they persist
6. Bug fixes and testing
   - Test on Samsung S22 Ultra
   - Test with headphones
   - Test screen-off playback
   - Test notification controls
   - Fix any crashes or issues

### Testing:
- [ ] All features work smoothly
- [ ] App handles errors gracefully
- [ ] Long compilations download and play correctly
- [ ] No memory leaks or performance issues
- [ ] Works well on target device (S22 Ultra)

### Deliverables:
- Fully functional, polished app
- All core features implemented
- Ready for personal use

---

## Phase 10+ Roadmap (Brainstorm Summary)

**Next/Previous:** They don’t work yet because there is no play queue; that’s addressed in Phase 10.

**Static when playing:** Often an **emulator** issue (buffer/sample-rate quirks). Test on a **real device** first. If it still happens, try updating Media3/ExoPlayer and/or adjusting buffers; see Phase 12. **Startup static:** Some users observe brief static when the app first starts playing; the audio often clears up after the app (or audio pipeline) has been running for a short while. This can be emulator-related or buffer warmup; note in Phase 12 if investigating.

**Lossless compression:** Android supports **FLAC** (lossless). **Opus** is lossy but very efficient. Phase 14 covers quality/lossless options and storage.

**Parity ideas (YouTube Music / Spotify style):** Queue + Next/Previous (Phase 10), smooth animations (Phase 11), crossfade (Phase 12), artwork/thumbnails (Phase 13), quality/bitrate options (Phase 14), then optional: preview before download, playback speed, equalizer, sleep timer, widget, offline search, custom playlists (Phase 15). Metadata/art is attainable for YouTube-sourced tracks via thumbnail URLs; full video metadata is optional.

---

## Phase 10: Queue, Next/Previous & Basic Playlist
**Goal:** Make Next/Previous buttons functional with a play queue.

### Why it was deferred:
- Next/Previous require a queue (list of tracks to play). Without a queue there is no "next" or "previous" track, so the buttons were no-ops until this phase.

### Tasks:
1. Define a play queue (in-memory or persisted) ✅
   - Queue: ordered list of track URIs + metadata (title, artist, optional trackId for library items)
   - Current index into the queue
2. Integrate queue with MusicPlayerService ✅
   - On track end (STATE_ENDED): if repeat mode off, advance to next queue item; if at end, stop
   - Next: skip to next item in queue (or stop if last)
   - Previous: go to previous item or restart current (e.g. if under 3s in, go to previous; else restart current)
3. Populate queue from Library ✅
   - "Play" from Library: set queue to full library (or filtered list), set current index to selected track, play
   - Optional: "Play next" / "Add to queue" from Library or Search (add single track to queue)
4. Update Player UI ✅
   - Next/Previous buttons call service methods that use the queue
   - Optional: show "Track X of Y" or queue length

### Testing:
- [x] Next advances to next track in queue
- [x] Previous goes to previous or restarts current
- [x] Playing from Library sets queue and plays selected track; Next/Previous work
- [x] When queue ends (no repeat), playback stops

### Deliverables:
- Functional Next/Previous
- Play queue populated from Library (and optionally from Search)

---

## Phase 11: Animations & UX Polish
**Goal:** Smooth, satisfying animations and transitions (Spotify/YouTube Music–style feel), plus app theme support.

### Tasks:
1. **App theme (light / dark / follow system)** ✅
   - Add a theme setting: **Light**, **Dark**, or **Follow system** (use system light/dark setting).
   - Persist the choice (e.g. SharedPreferences or DataStore).
   - Apply theme at app startup and when the setting changes (Compose: use `MaterialTheme.colorScheme` from a theme that respects the setting; wrap app in a theme that reads the preference and uses `darkTheme =` / `ColorScheme` accordingly).
   - Expose the setting in the existing Settings dialog (Player tab) or a dedicated "Appearance" section (e.g. dropdown or list choice).
2. **Fix app icon framing** ✅
   - The launcher icon currently zooms into the body of the Shuckle image; it should show the **whole picture** (full shuckle visible).
   - Adjust the launcher foreground (e.g. `ic_launcher_foreground.xml` and/or `ic_shuckle` usage) so the drawable is scaled or positioned to **fit the full image** in the adaptive icon bounds (e.g. use `android:gravity="center"` with a scaled bitmap that fits, or an inset so the full asset is visible rather than cropped/zoomed). Use `shuckle.png` or `shuckle.svg` (convert SVG to vector drawable if needed) so the entire character is visible.
3. Screen and transition animations
   - Animated transitions between tabs (e.g. fade/slide) — *requires NavHost animation API (e.g. navigation-compose animation artifact or 2.8+); current NavHost does not expose enter/exit transition lambdas with matching receiver type.*
   - Fragment/screen enter/exit transitions
4. List and item animations ✅
   - Animate list items on appear (e.g. staggered fade-in or slide)
   - Smooth scroll behavior; consider item animations on scroll — *Library uses `Modifier.animateItemPlacement()` for list item placement animation; favorite heart has scale animation.*
5. Loading and feedback
   - Skeleton loaders or shimmer for Search results and Library — *Search already shows CircularProgressIndicator when loading; full skeleton/shimmer optional.*
   - Button/layout state changes with subtle scale or opacity animation
   - Pull-to-refresh on Search or Library (if applicable)
6. Micro-interactions ✅
   - Play button press feedback (e.g. ripple, scale) — *Material buttons have built-in ripple.*
   - Favorite heart animation (e.g. brief scale or fill animation) — *Scale animation on favorite icon when toggled.*
   - Seek bar thumb feedback

### Testing:
- [x] Theme setting switches between Light, Dark, and Follow system correctly
- [x] Theme choice persists after app restart
- [x] App icon shows full Shuckle image (not zoomed into body)
- [x] Transitions feel smooth and consistent (sliding tab transitions implemented)
- [x] No jank or dropped frames on target device

### Deliverables:
- Light / Dark / Follow system theme with persisted setting
- App icon with full Shuckle visible (correct framing)
- Cohesive animation set for main flows
- Improved perceived quality and "polish"

---

## Phase 12: Crossfade & Audio Quality
**Goal:** Crossfade between tracks (Spotify-like) and reduce/eliminate static or glitches.

### Tasks:
1. Crossfade between tracks ✅
   - Volume-based crossfade: fade out current track, then switch to next and fade in (single ExoPlayer).
   - Configurable duration in Settings: **slider 0–10 s** (Spotify-style; stored in SharedPreferences via DownloadManager). "Off" at 0.
   - On queue advance (track end or skip next): if crossfade > 0, run fade-out steps then play next at 0 volume and fade in.
2. Static / crackling (research and mitigation) ✅
   - **Emulator note:** Static and glitches are common on the Android emulator (buffer size, sample rate quirks). Always verify playback on a **real device** before assuming an app bug.
   - **Startup static:** Audio may be staticky when playback first starts; it often improves after the app has been running (pipeline warmup or emulator behavior). If it persists on a real device, investigate buffer sizes or Media3 version.
   - If static persists on real device: ensure Media3/ExoPlayer is up to date (SilenceSkippingAudioProcessor bugs have been fixed in newer versions). Optional: try increasing buffer sizes or disabling silence-skipping if using custom pipeline.
   - **Audio troubleshooting** (developer note): See Phase 10+ Roadmap and this section; emulator vs real device is the first check; then Media3/ExoPlayer version and buffer settings.
3. Optional: audio focus and ducking
   - Ensure other apps (e.g. navigation) can take focus and Shuckler ducks correctly

### Testing:
- [x] Crossfade works when moving to next track (no hard cut)
- [x] Playback on real device: confirm whether static is emulator-only or needs code fix

### Deliverables:
- Crossfade slider (0–10 s) in Settings
- Documented approach for static (emulator vs device, Media3 version)

---

## Phase 13: Artwork & Metadata
**Goal:** Show track art (e.g. thumbnails) in Library, Player, and notification where feasible.

### Tasks:
1. Artwork source ✅
   - **YouTube:** Thumbnail URL from search results (NewPipe) is passed when starting download and stored in DownloadedTrack metadata.
   - Direct MP3 URLs: no thumbnail (optional field remains null).
2. Display artwork ✅
   - **Search:** Small thumbnail per YouTube result (or placeholder icon).
   - **Library list:** Small thumbnail per track (Coil AsyncImage) or placeholder icon.
   - **Player screen:** Larger art (200dp) with placeholder when missing.
   - **Notification:** Large icon set when artwork is available (bitmap loaded in background from thumbnail URL, scaled to 256px).
3. Storage ✅
   - Thumbnail URL stored in track metadata (downloads.json). No local image cache; images loaded from URL via Coil (UI) or URL connection (notification). Cache eviction: track delete removes metadata; no separate art files.

### Notes:
- Full "video" or rich metadata (e.g. channel art, description) is optional and not required for parity; focus on thumbnail/art for now.

### Testing:
- [x] YouTube-downloaded tracks show thumbnail in Library and Player when available
- [x] Notification shows artwork when supported

### Deliverables:
- Thumbnail/art in Search, Library, Player, and notification where we have a source (e.g. YouTube)

---

## Phase 14: Storage, Quality & Lossless Options
**Goal:** Offer quality options and explore lossless/compression for storage.

### Tasks:
1. Research and document ✅
   - **M4A (AAC):** Well supported on Android and in ExoPlayer. Often better compression than MP3 at similar quality; YouTube commonly serves M4A. We prefer M4A when bitrate is comparable (see quality selector).
   - **Lossless:** Android supports **FLAC** (decode from 3.1+, encode from 4.1+). Good for archival; no quality loss. NewPipe/YouTube rarely offer FLAC; we don’t re-encode.
   - **Opus:** Lossy but efficient; Android 5+ decode, 10+ encode. YouTube may offer WebM/Opus; we support playback (ExoPlayer); quality selector can pick lower-bitrate streams (data saver).
   - **MP3:** Widely compatible; we still save as MP3 when content-type is mpeg; otherwise we use stream format (m4a, webm, etc.).
2. Quality selector (download / settings) ✅
   - **Settings → Download quality (YouTube):** Best | High | Data saver. Stored in SharedPreferences.
   - **Best:** Highest bitrate stream; prefer M4A when tied. **High:** Second-highest bitrate; prefer M4A. **Data saver:** Lowest bitrate; prefer M4A. Implemented in YouTubeRepository (selectStreamByQuality) using NewPipe’s audio streams (bitrate + MediaFormat.M4A).
3. Storage optimization
   - **Storage used** is already shown in Library (Used / Free). Optional: breakdown by format or re-encode later.
4. Compression notes ✅
   - **File extension:** suggestFileName now uses content-type to set extension: mpeg→mp3, mp4/m4a/aac→m4a, ogg→ogg, webm→webm, wav→wav. So M4A and WebM downloads keep correct extension and play in ExoPlayer.

### Testing:
- [ ] Quality option affects downloaded file format/bitrate when supported
- [ ] App works with M4A/WebM (ExoPlayer supports both)

### Deliverables:
- Download quality options (Best / High / Data saver) in Settings; M4A preferred when available
- Correct file extensions for m4a/webm; documentation above

---

## Phase 15: Optional Features — Overview
**Goal:** Expand the app with optional features; each feature below is a dedicated phase (16–27) with tasks and deliverables.

**Recommended implementation order:** 16 → 17 → 18 → 19 → 20 → 21 → 22 → 23 → 24 → 25; 26 and 27 are optional/stretch.

| Phase | Feature | Notes |
|-------|---------|--------|
| 16 | Play next / Add to queue | Small; unblocks better queue UX. |
| 17 | Offline search | Search/filter Library by title/artist. |
| 18 | Playback speed | ExoPlayer setPlaybackSpeed; simple UI. |
| 19 | Sleep timer | Stop after N min; optional fade-out. |
| 20 | Custom playlists | Name, description, cover image; add/remove tracks; play as queue (Spotify-like). |
| 21 | Modern UI refresh | Spotify/YouTube Music/Apple Music–style look; animations where safe. |
| 22 | Home screen widget | Now playing + play/pause (and optionally next/previous). |
| 23 | Preview before download | Play 30–60 s from stream URL in Search without saving. |
| 24 | Recommendation system | Simple rules: recent searches, “more from favorites.” |
| 25 | Equalizer | Android AudioEffect (BassBoost/Equalizer). |
| 26 | Split long compilations | Optional; by chapters if extractor provides them. |
| 27 | Lyrics | Stretch; only if a source (e.g. API) is available. |

---

## Phase 16: Play Next / Add to Queue
**Goal:** From Search or Library, add a single track to the queue (“Play next” or “Add to end”) or insert after the current track.

### Tasks:
1. **Service/queue API**
   - In MusicPlayerService (or connection interface): add `addToQueueNext(item: QueueItem)` (insert at currentQueueIndex + 1) and `addToQueueEnd(item: QueueItem)` (append to queue). Ensure queue JSON/list is updated and notification/UI reflect new queue length.
2. **Library**
   - On each Library track row (or long-press menu): actions “Play next” and “Add to queue.” “Play next” inserts a QueueItem built from that DownloadedTrack at position currentQueueIndex + 1. “Add to queue” appends. If nothing is playing, “Play next” can start playback with that single track (or insert and play).
3. **Search**
   - For YouTube results we don’t have a downloaded file yet; “Add to queue” only makes sense for already-downloaded content unless we support streaming in queue (out of scope). So Phase 16 focuses on **Library only**: “Play next” and “Add to queue” for downloaded tracks. If we later add preview/streaming, we can extend to Search.
4. **UI**
   - Expose “Play next” / “Add to queue” via long-press context menu or icon buttons on Library track items. Optional: show a snackbar “Added to queue” / “Playing next.”

### Testing:
- [x] Play next inserts track after current; add to queue appends; queue length updates.
- [x] When nothing is playing, "Play next" starts playback with that track.
- [x] Queue view on Player shows list; tap row jumps to track. Crossfade audible when track nears end. (Was: When nothing is playing, “Play next” starts playback with that track (or insert + play from current).

### Deliverables:
- Add to queue (next / end) from Library; queue view on Player (bottom sheet); crossfade before track end.

---

## Phase 17: Offline Search
**Goal:** Search or filter within the downloaded library by title and/or artist, without network.

### Tasks:
1. **Data source**
   - Use existing `DownloadManager.downloads` (completed tracks). No new persistence; filter in memory.
2. **Search UI**
   - In Library screen: add a search bar or filter field (e.g. at top). As user types, filter the displayed list by matching query against `track.title` and `track.artist` (case-insensitive, substring). Clear button to reset.
3. **Behavior**
   - When filter is empty, show full list (respecting existing “All” / “Favorites” filter). When filter is non-empty, show only matching tracks. Play, favorite, delete, “Play next,” “Add to queue” work on the filtered list (use the same track reference/id).
4. **Performance**
   - Filter on main thread is fine for hundreds of tracks; if list is very large, consider debouncing the filter text (e.g. 150–300 ms) to avoid recomposition on every keystroke.

### Testing:
- [x] Typing in search filters Library by title and artist.
- [x] Clearing search restores full list. Favorites filter still applies on top of search.
- [x] Play / Play next / Add to queue work on filtered results.

### Deliverables:
- Search/filter bar in Library; offline search by title and artist.

---

## Phase 18: Playback Speed
**Goal:** Let the user change playback speed (e.g. 0.5x, 1x, 1.25x, 1.5x, 2x). ExoPlayer supports this natively.

### Tasks:
1. **Service**
   - [x] ExoPlayer: `player.setPlaybackSpeed(speed)`. StateFlow + setPlaybackSpeed in MusicPlayerService. Persist via DownloadManager (SharedPreferences). Applied in play() and onCreate.
2. **UI**
   - [x] Player screen: horizontal FilterChips for 0.5, 0.75, 1, 1.25, 1.5, 1.75, 2x. Display as “1x”, “1.25x”, etc.
3. **Behavior**
   - [x] On change, call service setter; playback continues at new speed. Persists across track change and app restart.

### Testing:
- [x] Changing speed updates playback immediately; persists across track change if persisted.
- [x] No crash or stuck state when toggling speed.

### Deliverables:
- Playback speed control in Player (or Settings); ExoPlayer setPlaybackSpeed integrated.

---

## Phase 19: Sleep Timer
**Goal:** Stop playback after a user-chosen duration (e.g. 15, 30, 45, 60 minutes); optionally fade out in the last minute.

### Tasks:
1. **Model**
   - Store “sleep timer end time” (e.g. System.currentTimeMillis() + durationMs) or “remaining ms” in the service or a small helper. Cancel when playback stops (user pause) or when timer fires.
2. **Service**
   - MusicPlayerService: start a timer (Handler.postDelayed or coroutine with delay) when user sets sleep timer. When timer fires: if fade desired, run volume fade-out over last N seconds then pause; else just pause. Clear timer on pause/stop if desired (or let it run; document behavior).
3. **UI**
   - In Player or Settings: “Sleep timer” control. Options: Off, 15 min, 30 min, 45 min, 60 min, “When track ends.” Show countdown or “Stops in 23 min” when active. Optional: “Fade out last 1 min” checkbox.
4. **Edge cases**
   - App killed: timer is lost (acceptable). Optional: persist end time and restore in service onCreate (more complex).

### Testing:
- [x] Timer stops playback after selected duration; fade (if implemented) works.
- [x] Turning timer off cancels scheduled stop.

### Deliverables:
- Sleep timer with duration options and optional fade-out; playback stops when timer ends.

---

## Phase 20: Custom Playlists (Spotify-like)
**Goal:** User-created playlists with name, optional description, and optional cover image; add/remove library tracks; reorder; play playlist as queue.

### Tasks:
1. **Data model**
   - **Playlist:** id (UUID), name (String), description (String?), coverImagePath (String? — path to a local image file in app storage, or null). **PlaylistEntry:** playlistId, trackId (references DownloadedTrack id), position (Int). Store playlists and entries in a persistent store (e.g. Room database, or JSON file in filesDir). Ensure when a track is deleted from library, remove it from all playlists (or leave orphaned entry and filter at read time).
2. **Cover image**
   - Allow user to set a cover image when creating/editing playlist: pick from gallery (Intent.ACTION_GET_CONTENT or picker) or take photo (camera). Save to app-specific storage (e.g. filesDir/playlist_covers/<playlistId>.jpg). Resize/compress to avoid huge files (e.g. max 512px). If no image, show a placeholder (e.g. first track’s thumbnail, or a default icon/gradient).
3. **CRUD**
   - Create playlist (dialog: name, description, optional cover). Edit playlist (same fields; reorder tracks if desired). Delete playlist (confirm). List playlists (e.g. in a new “Playlists” tab or section in Library).
4. **Tracks**
   - “Add to playlist” from Library: show dialog with list of playlists (or “New playlist”); add selected track to chosen playlist. From playlist detail: remove track; optional drag-to-reorder. Playlist detail screen: list of tracks (title, artist, thumbnail); play button sets queue to playlist and starts playback; “Play next” / “Add to queue” can reuse Phase 16.
5. **Navigation**
   - Add a way to reach playlists (tab, drawer, or “Playlists” in Library). Playlist detail: full-screen or bottom-sheet with list and metadata (name, description, cover at top).
6. **Animations**
   - Keep existing list animations (animateItem); optional: smooth transition when opening playlist detail. Avoid large refactors; stick to existing nav pattern (tabs: Search, Library, Player; playlists can live under Library or as a fourth tab).

### Testing:
- [x] Create playlist with name, description, and cover; appears in list.
- [x] Add tracks from Library to playlist; remove and reorder; play playlist as queue.
- [x] Deleting a library track removes it from playlists (or UI hides it); deleting playlist removes data and cover file.

### Deliverables:
- Custom playlists with name, description, optional cover image; add/remove/reorder tracks; play as queue; persistent storage.

---

## Phase 21: Modern UI Refresh — Polish & Mini-Player
**Goal:** Make the app feel modern, cohesive, and satisfying to use — inspired by the small details in Spotify, YouTube Music, and Apple Music (without copying them exactly). Focus on the expandable player pattern and polished interactions.

### Tasks:
1. **Mini-player (collapsible now-playing bar)**
   - **Always visible when playing:** On Search and Library tabs, show a compact bar at the bottom with album art, title, artist, play/pause, and skip buttons. User can see what's playing and control playback without leaving the tab.
   - **Tap to expand:** Tapping the mini-bar opens the full Player screen (full artwork, seek bar, all controls, queue, settings).
   - **Swipe or tap down to collapse:** From the full Player, swipe down or tap a collapse control to return to the mini-bar. Smooth animation between collapsed and expanded states.
   - **Implementation options:** Use a ModalBottomSheet that expands to full screen, or a shared layout where the Player content slides up from the bottom; mini-bar is the collapsed state of the same component.
2. **Design direction**
   - Choose a consistent direction inspired by (not copying) Spotify, YouTube Music, or Apple Music: e.g. rounded cards, bold headings, cohesive color palette, generous spacing. Apply consistently: ColorScheme, corner radius, typography scale, padding/margins.
3. **Global styling**
   - Theme: adjust ColorScheme (primary, surface, background), Shape (componentsDefaultCornerSize, cards), Typography (headlineMedium, titleLarge, etc.). Keep existing theme mode (light/dark/system); ensure both look good.
4. **Screens**
   - **Search:** search bar prominence; result cards with consistent elevation and corner radius; thumbnail size/spacing. **Library:** section headers (e.g. "Playlists," "Your Library"); list item height and padding; Favorites chip styling; clear visual hierarchy.
   - **Player (full):** large artwork; clear title/artist hierarchy; control buttons size and spacing; seek bar style; collapse affordance.
5. **Navigation**
   - Tab bar (Search, Library, Player): restyle with icons and labels; clear selected state (e.g. primary color). Player tab can open the full player directly or focus the mini-bar; ensure tab and mini-bar tap both reach full player.
6. **Polish & micro-interactions**
   - Keep existing: tab transition (slide), list item placement (animateItem), favorite scale. Add where appropriate: subtle transitions when expanding/collapsing the player; button press feedback; list item fade-in. Avoid risky refactors of NavHost or entire screen rewrites.
7. **Assets**
   - No new app icon required; optional: adjust default placeholder for “no artwork” (e.g. gradient or icon). Ensure playlist placeholder (Phase 20) fits the new style.

### Testing:
- [x] Mini-player appears on Search/Library when something is playing; play/pause and skip work from collapsed state.
- [x] Tapping mini-player expands to full Player; swiping/tapping down collapses to mini-bar.
- [x] Light and dark themes look consistent; no contrast or readability regressions.
- [x] All existing flows (search, download, library, play, queue, settings) still work; no layout breaks.

### Deliverables:
- Mini-player bar on Search/Library when playing; expandable to full Player with smooth transition.
- Cohesive modern UI (colors, shapes, typography, spacing).
- Polished interactions that make the app feel easy and satisfying to use.

### Phase 21 (Spotify-Style Refinements — implemented)
- **Navigation:** Home, Search, Library, Create (4 tabs). Remove Player tab; full Player only when tapping mini-player.
- **Bottom bar:** Transparent so content shows underneath; icons for each tab.
- **Mini-player:** Show for any playing song (queue or single track). Tap expands; swipe/collapse dismisses. Semi-transparent bar.
- **Search:** YouTube only; remove direct MP3 URL download section.
- **Theme:** Black + bright yellow (Spotify-style but yellow accent). Single dark theme; remove light/dark/system toggle.
- **Home tab:** Welcome / Listen now; quick access to Search, Library.
- **Create tab:** Create playlist flow.

---

## Phase 21d: UI Remodel — Design Notes (Planned)

Design notes for the next UI iteration; implement when ready.

### Consistent Header (Home, Search, Library)
- **Icon top-left:** User/settings icon (circle), same across all three tabs. Default avatar: Shuckle (same as app icon). Tapping opens Settings.
- **Title:** Search tab shows "Search" to the right of icon; Library shows "Library"; Home shows "Home" (or equivalent).

### Home Tab
1. **User icon (top-left):** Circular; default = Shuckle. Taps open Settings.
2. **Playlist shortcuts:** Mini versions (button-like) of user's playlists; max 8 most recent. Tap navigates to playlist.
3. **When no playlists yet:** Show recommendations instead. Recommendations based on library + liked songs (see below). Display with thumbnail art.
4. **Color:** More bright yellow; make it almost neon yellow. Add UI details (accents, borders, highlights) using that color.
5. **Recommendations vs Search:** Since Search will also have recommendations, consider: Home focuses on playlist shortcuts + quick access; Search has search bar + recommendations. Or: Home has playlist shortcuts + something else (e.g. "Recently played") when no playlists; Search owns the recommendation surface.

### Search Tab
1. **Search bar:** Primary place to search for music.
2. **Recommendations:** Show recommendations (e.g. based on recent searches, liked artists, or trending). Thumbnail art.
3. **Header:** Icon + "Search" title.

### Library Tab
1. **Playlists included:** No large "Create playlist" block.
2. **"+" button (top-right):** Create playlist; small icon like Spotify.
3. **Search bar:** To the LEFT of the "+" icon; lets user search/filter playlists when they have many.
4. **Filters:** Playlists | Albums | Artists (liked) | Recents | Recently added | Alphabetical | Creator.
5. **View options:** Grid view, detailed/list view, etc.

### Replace Create Tab → Analytics Tab
1. **Remove Create tab** from bottom nav.
2. **Analytics tab instead:** Listening stats — graphs, charts, statistics. "What you've listened to."
3. **Create playlist:** Triggered from Library's "+" button (modal or slide-up). Options: create playlist; collaborative (skip for now — few users).

### Liked Songs
- **Favorites = Liked:** Use existing favorites (heart icon) as "liked" — no separate concept.
- Used for: recommendations, "artists you like," filtering, Search recommendations.

### Albums & Artists
- **Albums:** Shuckler downloads from YouTube; "album" may be derived from metadata or simplified (e.g. tracks by artist).
- **Artists:** Filter/section "Artists you like" — derive from liked songs.

### Resolved Clarifications
1. **Home when no playlists:** Show "Recently played" / "Quick picks" first.
2. **Search recommendations:** Main drivers: liked genres, trending, or "Try these" for exploring new genres. Recent searches factor in if the user has searched the same thing more than 3 times.
3. **Albums:** Full album support with metadata when possible.
4. **Neon yellow:** #E8FF00.
5. **Favorites = Liked:** Treat favorites as liked for recommendations and filtering.

---

## Phase 21e: UI Refinements (Planned)

Follow-up refinements from Phase 21d implementation.

### Home Tab
- **Greeting:** Already implemented — changes by time of day: "Good morning" (0–11), "Good afternoon" (12–17), "Good evening" (18–23). Uses `Calendar.HOUR_OF_DAY`.
- **Recommendations:** Will fill space when recommendation system is added.

### Library Tab
1. **Downloads section (expandable):**
   - By default: hide the downloads list, storage used, and storage free.
   - **Collapsed:** chevron down (tap to expand). **Expanded:** chevron up (tap to collapse).
   - Animated expand/collapse.
2. **Truncate long titles:** Two-line truncation with trailing `"..."` (maxLines = 2).
3. **Playlist cards:**
   - **Name below art:** Playlist name appears **below** the album/cover art, not overlaid.
   - **Cover crop:** User sees a rounded-square selection they can drag and resize (always square). Slightly rounded corners on crop region. App does not choose dimensions by default.
4. **Header layout:**
   - Search = icon button that opens a search field (not a persistent field).
   - Top row: `[Shuckle icon] [Library] ... [Search icon] [+ icon]`
   - Search icon directly to the **left** of the "+" icon; "+" in top-right on same line as "Library".
5. **Filter/sort for playlists:** Alphabetical, Recently played, etc.

### Analytics Tab
1. **Content:** Tracks, plays, favorites, most played (tracks), most played playlists (title + cover art).
2. **Time range:** 24 hours, 7 days, 30 days, all time.
3. **Graphs and bar charts:** Visuals for stats; users prefer visuals over text-only.
4. **Sort:** Most played, etc.

---

## Phase 22: Home Screen Widget
**Goal:** A home screen widget showing now-playing info and play/pause (and optionally next/previous).

### Tasks:
1. **Widget** ✅
   - Use App Widget (Android XML layout + AppWidgetProvider) or Jetpack Glance (Compose for widgets, if min SDK allows). Widget layout: small/medium size: album art (or placeholder), title, artist, play/pause button. Optional: next/previous buttons. Tapping widget opens the app (or opens Player tab).
2. **Updates** ✅
   - When playback state or track changes, update widget (e.g. MusicPlayerService calls AppWidgetManager.updateAppWidget). Use RemoteViews (or Glance) to set title, artist, and play/pause drawable (play vs pause icon).
3. **Actions** ✅
   - Play/pause (and next/previous if present) send broadcast or start service with action (e.g. ACTION_PLAY, ACTION_PAUSE). Service handles as in existing onStartCommand. Optional: tap artwork to open app.
4. **Artwork** ✅
   - If feasible, set widget’s album art from current track thumbnail (load bitmap and set on ImageView in RemoteViews). Fallback: app icon or placeholder.

### Testing:
- [x] Widget shows current track and play/pause state; tapping play/pause toggles playback.
- [x] Widget updates when track changes or app is in background.

### Deliverables:
- Working home screen widget (play/pause, optional next/previous); updates with now-playing info.

---

## Phase 23: Preview Before Download
**Goal:** In Search (YouTube), play a short preview (e.g. 30–60 seconds) from the stream URL without saving; “Download” still saves the full file.

### Tasks:
1. **Preview playback** ✅
   - When user taps “Preview” (or similar) on a search result: obtain stream URL via existing getAudioStreamUrl. Play in ExoPlayer (either a temporary/secondary player in the app, or reuse MusicPlayerService with a “preview mode” that stops after N seconds and doesn’t add to queue). Stop after 30–60 s (or when user taps stop). Do not save to disk.
2. **UI**
   - Add “Preview” button next to “Download” on YouTube search results. While preview is playing: show “Preview playing…” and a stop button; optional progress for the 30–60 s window.
3. **Service vs in-app player** ✅
   - Option A: Use MusicPlayerService with a flag “previewOnly” and a timer; when timer fires or track would exceed 60 s, stop and clear. Option B: Use a separate ExoPlayer instance in the ViewModel/Composable scope for preview only (simpler but doesn’t use notification; user can’t leave app and keep preview). Choose based on desired UX.
4. **Edge cases** ✅
   - If user starts a full download while preview is playing, stop preview and start download. If user leaves Search during preview, decide: stop preview or let it play (simpler to stop).

### Testing:
- [x] Preview plays for up to 30–60 s then stops; Download still saves full file.
- [x] No leftover preview state that breaks normal playback.

### Deliverables:
- Preview button in Search; short preview playback from stream URL without saving.

---

## Phase 24: Recommendation System ✅
**Goal:** Surface “For you”–style content using simple rules: recent searches and favorites (no ML).

### Tasks:
1. **Data** ✅
   - Persist last N search queries (e.g. 10–20) in SharedPreferences or a small JSON file. We already have favorites (DownloadedTrack.isFavorite) and play counts; no new backend.
2. **“For you” / Home section**
   - If we have a “Home” or “Listen now” surface (could be part of Phase 21 or a new tab): section “Recent searches” (tappable to run search again or show recent results if cached); section “From your favorites” (tracks that are favorite, or most-played). Alternatively: add “Recommended” or “Quick picks” at top of Library (e.g. “Recently played,” “Favorites,” “Recent searches” as horizontal chips or rows).
3. **Logic**
   - “Recently played”: last N tracks played (need to persist “last played” order or timestamps; e.g. add lastPlayedMs to metadata or a separate list). “Favorites”: filter library by isFavorite. “Recent searches”: list of saved query strings; tap opens Search with that query. Keep it simple; no collaborative filtering or external API.
4. **UI** ✅
   - Small, scoped UI: e.g. a “Home” tab with 2–3 sections, or a collapsible “For you” block at top of Library. Don’t overwhelm; 1–2 rows of “Quick access” is enough.

### Testing:
- [x] Recent searches appear and tapping one re-runs search (or fills search box).
- [x] Favorites / recently played section shows correct tracks.

### Deliverables:
- Simple recommendations: recent searches and “from favorites” (and optionally recently played) surfaced in a dedicated section or Home.

---

## Phase 25: Equalizer ✅
**Goal:** Let the user adjust bass/treble or a simple multi-band EQ using Android’s AudioEffect APIs.

### Tasks:
1. **API**
   - Use Android’s Equalizer (and optionally BassBoost) from android.media.audiofx. Attach to the same audio session as ExoPlayer. ExoPlayer/Media3: obtain audio session ID from the player and attach Equalizer to it. Enable/disable and set levels from UI.
2. **UI** ✅
   - Settings or Player: “Equalizer” entry; open a simple screen with presets (Normal, Rock, Pop, etc.) and/or sliders for a few bands (e.g. 5-band). Persist user’s preset or custom levels in SharedPreferences.
3. **Compatibility** ✅
   - Not all devices support Equalizer; check Equalizer.isAvailable() and hide or disable UI if not available. Handle cleanup when playback stops (release effect).

### Testing:
- [x] When available, equalizer affects playback; settings persist.
- [x] No crash when Equalizer is not available.

### Deliverables:
- Equalizer (and optional BassBoost) in Settings/Player; persisted; graceful fallback when unsupported.

---

## Phase 26: Split Long Compilations (Optional)
**Goal:** For very long YouTube videos, optionally split by chapters (or fixed time intervals) into separate tracks.

### Tasks:
1. **Chapters**
   - Check if NewPipe StreamExtractor (or stream info) provides chapter list (start time, title). If yes: after full download, optionally offer “Split by chapters” and create multiple entries (e.g. one per chapter) that reference the same file with start/end offsets, or split the file (complex). Simpler approach: “virtual” tracks — one file, multiple “tracks” with startMs/endMs; playback uses ExoPlayer.seekTo(startMs) and a listener to stop at endMs and advance to next. That avoids re-encoding or file splitting.
2. **UI**
   - Only show “Split by chapters” (or “Add as multiple tracks”) when chapters are available and video is long (e.g. > 10 min). In Library, show these as separate rows (same file path, different start/end); play as queue.
3. **Scope**
   - If extractor doesn’t expose chapters, skip or document “future: when extractor supports chapters.” Time-based split (e.g. every 5 min) is possible but less useful; document as optional.

### Testing:
- [ ] When chapters exist, user can add video as multiple chapter-tracks; playback respects start/end.
- [ ] Library and queue show chapter-tracks correctly.

### Deliverables:
- Optional split by chapters when available; virtual tracks with start/end in one file, or document limitation.

---

## Phase 27: Lyrics (Stretch) ✅
**Goal:** Show lyrics in the Player screen if a source is available.

### Tasks:
1. **Source** ✅
   - Using LRCLIB (https://lrclib.net) — free, no API key. Search API by track_name + artist_name returns synced (LRC) or plain lyrics.
2. **UI** ✅
   - Synced lyrics with highlighted current line; plain text with scroll when unsynced.
3. **Storage** ✅
   - Lyrics cached by (artist, title) hash in app filesDir/lyrics_cache as JSON.

### Testing:
- [x] When lyrics are available, they display and (if synced) highlight current line.
- [x] No crash when lyrics are missing or API fails.

### Deliverables:
- Lyrics in Player via LRCLIB API; local cache; expandable section with synced/plain display.

---

## Phase 28: Import Playlists from Spotify (Planned)
**Goal:** Import existing playlists from Spotify (and optionally other music apps) into Shuckler.

### Tasks:
1. **Spotify API integration**
   - Use Spotify Web API (OAuth 2.0) to authenticate and fetch user's playlists.
   - For each playlist: get track list (title, artist). Spotify doesn't provide downloadable audio; user will need to search YouTube for each track and download. Consider UX: batch "import" that creates a Shuckler playlist and queues YouTube searches for each track.
2. **Other services** (optional)
   - YouTube Music, Apple Music, etc. — similar pattern if APIs allow.
3. **UI**
   - "Import from Spotify" (or similar) in Library or Settings. OAuth flow; then show list of user's Spotify playlists. User selects which to import. For each track: search YouTube (optional: auto-match by title+artist), add to download queue or "to-download" list.

### Notes:
- Spotify API requires app registration at Spotify Developer Dashboard; redirect URI, client ID/secret.
- Spotify ToS: ensure compliance with API usage (non-commercial personal use typically allowed).
- **Import does not speed up downloads:** We still fetch audio from YouTube for each track. Import saves you from manually searching; download speed is unchanged.

---

## Phase 29: Bug — Download Restarts / Fails with "Unexpected End of Stream" ✅
**Goal:** Fix download failures including "unexpected end of stream" (occurs even when staying on Search screen).

### Problem:
- When downloading a song, it may fail with "unexpected end of stream" — sometimes at 26%, after stalling for minutes.
- Root causes: (1) **YouTube stream URLs expire** (2) **No fresh URL on retry** (3) **Network/throttling**

### Tasks:
1. **Investigate** ✅
   - Main failure cause: YouTube stream URLs expire or server closes connection.
2. **Fresh URL on retry** ✅
   - Fetches fresh stream URL on each retry. 5 attempts (up from 3), 2 sec delay between retries.
3. **Resumable downloads** ✅
   - HTTP Range header support: when download fails mid-stream, keeps partial file, fetches fresh URL, resumes from last byte via `Range: bytes=N-`. YouTube's googlevideo.com supports 206 Partial Content. Progress is preserved across retries.

---

## Phase 30: Player Background Gradient from Album Art ✅
**Goal:** Replace the black Player background with a dynamic gradient derived from the current track's album cover.

### Tasks:
1. **Extract dominant color** ✅
   - Using AndroidX Palette API on the track's thumbnail. Fallback to ShucklerBlack when no art.
2. **Gradient** ✅
   - Top of Player: darkened dominant color (lerp 40% toward black for readability). Bottom: fade to ShucklerBlack (#121212). Vertical gradient.
3. **Per-track** ✅
   - When track changes, LaunchedEffect(thumbnailUrl) recomputes color from new artwork.
4. **UI** ✅
   - Gradient applied as Box background behind entire Player content.

---

## Phase 22a: Home Widget UI Improvements (Planned)
**Goal:** Polish the home screen widget appearance.

### Notes:
- Widget works but "could definitely use some work on the UI."
- Consider: improved layout, better typography, rounded corners, consistent styling with app theme (yellow accent, dark background). Optional: different widget sizes (e.g. 2x2 compact vs 4x2 full).

---

## Phase 31: Stream Without Download (Spotify-Style) ✅
**Goal:** Play full tracks from YouTube without downloading. Listen instantly; optionally download for offline later.

### Tasks:
1. **Play button in Search** ✅
   - Add "Play" button next to Preview and Download on YouTube search results.
   - On tap: fetch stream URL via `YouTubeRepository.getAudioStreamUrl`, then play via `MusicPlayerService` (ACTION_PLAY_URI) with the stream URL.
   - No 60-second limit; full track streams until end.
2. **Play in Home recommendations** ✅
   - Same "Play" (stream) button on recommended YouTube cards.
3. **Downloaded indicator** ✅
   - When a search result is already downloaded (sourceUrl matches), show "Downloaded" and disable the Download button.
4. **UI** ✅
   - Play, Preview, and Download as distinct actions. Loading state ("…") while fetching stream URL.

### Notes:
- Stream URLs expire; we fetch fresh URL on each play. No queue support for stream-only items yet (would require storing videoUrl and resolving when playing).
- Download remains for offline listening; streaming uses data.

### Testing:
- [x] Play streams full track without saving
- [x] Download still works for offline
- [x] Already-downloaded tracks show "Downloaded"

### Deliverables:
- Play (stream) from Search and Home; Download for offline; skeleton loaders; Library sort + delete with undo.

---

## Phase 31a: QOL — Library Sort, Delete Undo, Skeleton Loaders ✅
**Goal:** Quality-of-life improvements from QOL.md.

### Tasks:
1. **Library sort options** ✅
   - Sort tracks by: Date added, Title A–Z, Artist, Duration, Play count.
   - Dropdown in "Your Library" section.
2. **Delete with Undo** ✅
   - When user deletes a track: remove from list, show Snackbar "Removed" with "Undo".
   - If Undo within 5 seconds: restore. Otherwise delete file.
3. **Skeleton loaders** ✅
   - Search: placeholder cards (gray boxes) instead of spinner while searching.

### Deliverables:
- Sort dropdown for Library tracks; delete + Undo snackbar; skeleton loaders in Search.

---

## Phase 32: Continue Listening (Resume Position)
**Goal:** Resume long tracks (podcasts, compilations) from where the user left off.

### Tasks:
1. **Persist playback position**
   - When playback stops (pause, app background, track change): save `(trackId, positionMs)` to metadata or database.
   - Update position periodically during playback (e.g. every 5–10 seconds) to avoid losing progress on crash.
2. **Resume on play**
   - When user plays a track: if we have a saved position and it's reasonable (e.g. track not deleted, position < duration), seek to that position before playing.
   - Optional: only resume if position is > 10 seconds (avoid resuming from near-start).
3. **UI hint**
   - Optional: show "Resume from 12:34" or similar when track has saved position; or just resume silently.

### Testing:
- [x] Pausing and resuming restores position
- [x] App kill and reopen: position restored when playing same track
- [x] Deleting track clears saved position (track removed from metadata)

### Deliverables:
- Playback position persisted per track; resume from last position when playing. ✅

---

## Phase 33: Haptic Feedback & Double-tap to Seek
**Goal:** Add tactile feedback and quick seek gestures for a more responsive feel.

### Tasks:
1. **Haptic feedback**
   - Light vibration (`HapticFeedbackConstants.CONFIRM` or `performHapticFeedback`) on: play/pause, favorite toggle, skip next/previous, and key button presses.
   - Optional: different haptic for "favorite" (slightly longer) vs "play" (short).
   - Respect system "Touch vibration" setting if possible.
2. **Double-tap to seek**
   - On seek bar (or area around it): double-tap left = seek backward 10 seconds; double-tap right = seek forward 10 seconds.
   - Use `Modifier.pointerInput` with `detectTapGestures(onDoubleTap = …)` or similar; determine left/right by tap position relative to seek bar center.
   - Clamp to 0 and duration; provide haptic on successful seek.

### Testing:
- [x] Haptic fires on play/pause, favorite, skip
- [x] Double-tap left seeks back 10 s; double-tap right seeks forward 10 s
- [x] No crash when double-tapping at track start/end

### Deliverables:
- Haptic feedback on key actions; double-tap left/right to seek ±10 seconds. ✅

---

## Phase 34: Recently Played Section
**Goal:** Dedicated "Recently played" section for quick access to what you just listened to.

### Tasks:
1. **Data**
   - Persist "recently played" list: ordered by last play time (most recent first). Store track IDs; cap at e.g. 50 items. Update on each play.
   - Use existing metadata (lastPlayedMs) or a separate list in SharedPreferences/JSON.
2. **UI**
   - Add "Recently played" section on Home or Library (or both). Horizontal scroll or vertical list; show thumbnail, title, artist. Tap to play (set queue from this list, play selected).
   - Hide section when empty.
3. **Behavior**
   - "Play" from recently played: set queue to recently played list, current index = selected item, play. Optional: "Play next" / "Add to queue" from items.

### Testing:
- [x] Playing a track adds it to recently played (or updates order)
- [x] Tapping item plays it; queue reflects recently played order
- [x] Section hidden when no plays yet

### Deliverables:
- Recently played section with quick play; persists across app restarts. ✅

---

## Phase 35: "Surprise me" & "Throwback"
**Goal:** Fun discovery buttons: random track, or a track you haven't heard in a long time.

### Tasks:
1. **"Surprise me"**
   - Button on Home or Library. On tap: pick a random track from library (or optionally bias toward rarely played). Set queue to that single track (or "shuffle library, start with this") and play.
   - Optional: brief "roulette" animation (e.g. cycling through track names) before revealing and playing.
2. **"Throwback"**
   - Button on Home or Library. On tap: filter tracks not played in last N days (e.g. 30). Pick one at random. Play it. If none match, fall back to "Surprise me" or show "No throwbacks yet."
   - Requires `lastPlayedMs` (or equivalent) in track metadata.
3. **UI**
   - Place buttons prominently on Home (e.g. under greeting) or as chips. Clear labels: "Surprise me" and "Throwback."

### Testing:
- [x] "Surprise me" plays a random library track
- [x] "Throwback" plays a track not played in 30+ days when available
- [x] No crash when library is empty

### Deliverables:
- "Surprise me" and "Throwback" buttons; random/rarely-played discovery. ✅

---

## Phase 36: Up Next Preview & Queue Reorder
**Goal:** Show next few tracks in mini-player; allow drag-to-reorder queue.

### Tasks:
1. **Up next preview**
   - In mini-player (or expandable section): show next 2–3 tracks (thumbnail, title, artist). Tap a row to jump to that track in queue.
   - Use existing queue from MusicPlayerService; expose via StateFlow or callback.
2. **Queue reorder**
   - In full Player queue view (bottom sheet): allow drag-and-drop to reorder. Use `Modifier.draggable` / `Modifier.dragAndDropSource` or a list with reorder support (e.g. `ReorderableLazyList` from accompanist or manual implementation).
   - Update queue in service when order changes; persist if queue is persisted.
3. **UI**
   - Ensure up-next is visible without expanding full player (e.g. 1–2 rows in collapsed state). Full queue view shows full list with drag handles.

### Testing:
- [x] Up next shows correct next tracks; tap jumps to track
- [x] Move up/down reorder updates queue; playback follows new order
- [x] Queue reorder works in queue view

### Deliverables:
- Up next preview (2–3 tracks) in mini-player; move up/down reorder in queue view. ✅

---

## Phase 37: Gapless Playback & Preload Next
**Goal:** Seamless transitions between tracks and faster start for next track.

### Tasks:
1. **Gapless playback**
   - ExoPlayer supports gapless via `setAudioAttributes` and proper `MediaItem` preparation. Ensure we're not introducing gaps (e.g. avoid unnecessary stop/start). Verify `MediaItem` for next track is prepared before current ends.
   - Crossfade (Phase 12) may overlap; ensure gapless + crossfade work together (crossfade fades out/in; gapless avoids hard gaps when crossfade is 0).
2. **Preload next track**
   - When current track is playing: preload/buffer the next track in queue. ExoPlayer's `ExoPlayer.prepare()` for next MediaItem, or use `MediaSource` preloading. Goal: next track starts instantly (or near-instant) when current ends.
   - Consider memory: don't preload too far ahead; next 1 track is usually enough.
3. **Edge cases**
   - Queue has 1 item: no preload. User skips rapidly: cancel preload if needed. Stream URLs: preload may require fetching URL for next; handle async.

### Testing:
- [x] No audible gap between tracks when crossfade is 0
- [x] Next track starts quickly when current ends (preload effective)
- [x] No memory issues with rapid skips

### Deliverables:
- Gapless playback; preload next track in queue for instant start. ✅

---

## Phase 38: Achievement Badges
**Goal:** Gamification — unlock badges for milestones; show in Analytics or profile.

### Tasks:
1. **Badge definitions**
   - Define badges: e.g. "First download," "First playlist," "10 favorites," "100 plays," "7-day streak," "Library of 50," "Night owl" (listen after midnight), etc. Each has id, name, description, icon/emoji, unlock condition.
2. **Tracking**
   - Check conditions on relevant events (download, play, favorite, playlist create, etc.). Persist unlocked badges (e.g. `Set<badgeId>` in SharedPreferences).
   - Listening streak: compute from play history (need daily play timestamps).
3. **UI**
   - Analytics tab (or Settings): "Achievements" section. Grid of badges; locked = grayed; unlocked = full color with date earned. Optional: toast/snackbar when badge unlocked ("You earned: First playlist!").
4. **Celebration**
   - When badge unlocks: brief celebration (e.g. confetti, scale animation, or simple dialog). Don't be intrusive; can be dismissible.

### Testing:
- [x] Badges unlock when conditions met
- [x] Unlocked badges persist and display correctly
- [x] No false unlocks

### Deliverables:
- Achievement badge system; unlock on milestones; display in Analytics. ✅

---

## Phase 39: Swipe to Delete & Remember Scroll Position
**Goal:** Swipe-to-delete in Library/Playlists; restore scroll when returning to tab.

### Tasks:
1. **Swipe to delete**
   - In Library and Playlist track lists: swipe left (or right) to reveal delete action. On confirm: delete with undo snackbar (reuse Phase 31a pattern). Use `SwipeToDismiss` or `Modifier.swipeable` / custom implementation.
   - Match existing delete + undo behavior (5 s to undo).
2. **Remember scroll position**
   - When user leaves a tab (Library, Search): save scroll position (e.g. first visible index or offset). When returning: restore scroll so user sees same place.
   - Use `LazyListState` and `rememberSaveable` with `ScrollPosition` saver, or `LaunchedEffect` to restore. Handle list changes (e.g. item deleted) gracefully.
3. **Scope**
   - Apply to Library list and Search results. Playlist detail if applicable. Optional: Home if it has scrollable content.

### Testing:
- [x] Swipe reveals delete; undo restores
- [x] Scroll position restored when switching tabs and back
- [x] Deleted item doesn't break scroll restoration

### Deliverables:
- Swipe to delete in Library/Playlists with undo; scroll position remembered per tab. ✅

---

## Phase 40: Download All from Playlist
**Goal:** Batch download all tracks in a playlist with one tap.

### Tasks:
1. **Service**
   - Add "Download all" action: for each track in playlist, if not already downloaded, queue download. Reuse existing DownloadManager; tracks may be YouTube (search by title+artist) or already have sourceUrl. Handle mixed case: some downloaded, some not.
2. **UI**
   - In playlist detail: "Download all" button. Show progress (e.g. "3/12 downloaded") or a progress bar. Disable or show "Downloading…" while in progress.
   - For YouTube-sourced playlists: need to resolve each track to YouTube URL (search or stored sourceUrl). If track has sourceUrl, use it; else search YouTube and pick best match.
3. **Edge cases**
   - Some tracks may fail (e.g. not found on YouTube). Show partial success: "10 of 12 downloaded; 2 failed." Optional: retry failed.
   - Wi‑Fi only setting (if implemented): respect it; show message if on cellular.

### Testing:
- [x] Download all queues all non-downloaded tracks
- [x] Progress visible; completed count correct
- [x] Already-downloaded tracks skipped
- [x] Partial failure reported clearly

### Deliverables:
- "Download all" from playlist; batch download with progress; skip already-downloaded. ✅

---

## Phase 41: Empty State Illustrations
**Goal:** Friendly, on-brand empty states when Library, Search, or Playlists have no content.

### Tasks:
1. **Assets**
   - Create or source simple illustrations for: empty Library, empty Search results, no playlists, no favorites. Can use Shuckle-themed placeholders (e.g. Shuckle with headphones, Shuckle searching). SVG or PNG; place in `res/drawable`.
2. **UI**
   - When Library is empty: show illustration + "Your library is empty" + "Search and download to get started" + button to open Search.
   - When Search returns no results: "No results for [query]" + illustration + "Try a different search."
   - When no playlists: "No playlists yet" + "Create one to organize your music" + create button.
   - When no favorites: "No favorites yet" + "Tap the heart on any track to add it."
3. **Consistency**
   - Use consistent styling (colors, typography) with app theme. Keep copy short and friendly.

### Testing:
- [x] Empty states show when appropriate
- [x] CTA buttons navigate correctly
- [x] No layout issues on different screen sizes

### Deliverables:
- Empty state illustrations and copy for Library, Search, Playlists, Favorites. ✅

---

## Phase 42: Default Tab & Wi‑Fi Only Downloads
**Goal:** Let users choose which tab opens on launch; optionally restrict downloads to Wi‑Fi.

### Tasks:
1. **Default tab**
   - Setting: "Open on launch" — Home, Search, Library, or Analytics. Persist in SharedPreferences. On app start (or when MainActivity creates): navigate to selected tab instead of default (e.g. Home).
   - If Analytics tab doesn't exist yet, hide that option or add when ready.
2. **Wi‑Fi only downloads**
   - Setting: "Download only on Wi‑Fi" (default: off). When enabled: before starting a download, check `ConnectivityManager.getActiveNetwork()`. If not Wi‑Fi (e.g. cellular), show snackbar "Downloads are Wi‑Fi only" and don't start. Optionally: queue for later when Wi‑Fi connected (more complex).
   - For streaming (Play): optionally separate setting "Stream on cellular" (default: allow). Simpler: just downloads for now.
3. **UI**
   - Add to Settings: dropdown for default tab; toggle for Wi‑Fi only downloads. Clear labels.

### Testing:
- [x] App opens to selected default tab
- [x] Wi‑Fi only: download blocked on cellular; works on Wi‑Fi
- [x] Settings persist across restarts

### Deliverables:
- Default tab on launch setting; Wi‑Fi only downloads option. ✅

---

## Phase 43: "Don't Play This Again" (Exclude from Shuffle)
**Goal:** Let users temporarily exclude tracks from shuffle (e.g. "don't play for 30 days").

### Tasks:
1. **Data model**
   - Add `excludedFromShuffleUntilMs` (or similar) to track metadata. When set: track is excluded from shuffle until that timestamp. Null = not excluded.
2. **Shuffle logic**
   - When building shuffle queue (e.g. "Shuffle library"): filter out tracks where `excludedFromShuffleUntilMs != null && currentTime < excludedFromShuffleUntilMs`.
   - Manual queue and "Play" from Library: still include all tracks (exclusion only affects shuffle).
3. **UI**
   - Long-press or context menu on track: "Don't play for 30 days" (or "Exclude from shuffle"). Sets exclusion. Optional: "Don't play for 7 days," "Don't play for 90 days," "Remove exclusion."
   - In Library: optional indicator (e.g. small icon) on excluded tracks. In track detail or metadata: show "Excluded until [date]."
4. **Clear exclusion**
   - User can remove exclusion via same menu. Exclusion auto-expires when timestamp passes (no cron needed; check at shuffle time).

### Testing:
- [x] Excluded tracks don't appear in shuffle
- [x] Excluded tracks still play when selected directly
- [x] Exclusion expires after set duration
- [x] Manual "Remove exclusion" works

### Deliverables:
- "Don't play this again" / exclude-from-shuffle with configurable duration; filter applied when shuffling. ✅

---

## Phase 44: App Shortcuts (Quick Launch) ✅
**Goal:** Long-press app icon shows quick actions: Resume playback, Shuffle library, Recently played.

### Tasks:
1. **Define shortcuts** ✅
   - Use `ShortcutManager` (Android 7.1+): "Resume playback" (resume last track), "Shuffle library" (shuffle all, play), "Recently played" (open app to recently played / play first). Optional: "Search" (open Search tab).
2. **Implementation** ✅
   - Static shortcuts in `res/xml/shortcuts.xml`; meta-data in AndroidManifest. Each shortcut has intent with extra `shortcut_action` (resume, shuffle, recently_played).
3. **Handle intents** ✅
   - MainActivity `onCreate` / `onNewIntent`: `AppShortcutHandler.handleShortcutIntent` reads action, starts MusicPlayerService with ACTION_PLAY_WITH_QUEUE (resume = most recent track; shuffle = shuffled library; recently_played = recently played list or shuffle fallback).
4. **Icons** ✅
   - Resume: `@android:drawable/ic_media_play`; Shuffle/Recently played: custom vector drawables.

### Testing:
- [ ] Long-press app icon shows shortcuts
- [ ] Each shortcut performs correct action
- [ ] Works when app is cold start vs already running

### Deliverables:
- App shortcuts: Resume, Shuffle library, Recently played. ✅

---

## Phase 45: First Launch / Onboarding ✅
**Goal:** Friendly first-time experience: brief welcome and tips so new users know what to do.

### Tasks:
1. **First launch detection** ✅
   - `OnboardingPreferences` checks SharedPreferences `has_completed_onboarding`. If false, show onboarding; set true when done.
2. **Onboarding flow** ✅
   - 4 screens: (1) Welcome with Shuckle icon; (2) Search & download; (3) Library & playlists; (4) "You're all set!" with Get started.
   - Compose `HorizontalPager`; dots indicator; Skip (top-right) and Next/Get started buttons.
3. **Content** ✅
   - Short copy, Shuckle icon on first screen. No account required.
4. **Skip** ✅
   - "Skip" always available; completes onboarding immediately.
5. **Show tutorial in Settings** ✅
   - "Show tutorial" button in Settings; walks through the same onboarding flow anytime.

### Testing:
- [ ] Onboarding shows on first install
- [ ] Skip and Get started both complete onboarding
- [ ] Onboarding does not show again after completion

### Deliverables:
- 4-screen onboarding for first launch; skip option; sets flag so it doesn't show again. ✅

---

## Phase 46: Share Track & Playlist ✅
**Goal:** Share a track (YouTube link or track info) or export a playlist as text/file.

### Tasks:
1. **Share track** ✅
   - From track context menu: "Share" action. If we have `sourceUrl` (YouTube): share that URL. Else: share "Title — Artist" as text. Uses `ShareUtil.shareText` with `Intent.ACTION_SEND`.
2. **Share playlist** ✅
   - From playlist detail header: Share icon. Export as plain text: playlist name + list of "Title — Artist" (one per line). Share via `Intent.ACTION_SEND` (text/plain).
3. **UI** ✅
   - "Share" in long-press context menu on LibraryTrackItem; Share icon in PlaylistDetailScreen header.

### Testing:
- [ ] Share track sends correct URL or text
- [ ] Share playlist sends readable list
- [ ] Share intent opens system share sheet

### Deliverables:
- Share track (URL or text); share playlist (text list); both via system share sheet. ✅

---

## Phase 47: Error Handling & Retry UX ✅
**Goal:** Friendly error states and retry when streams fail or downloads error out.

### Tasks:
1. **Stream failures** ✅
   - Play (stream) fails: snackbar "Couldn't play — check connection" with "Retry" action; retry fetches fresh URL and plays. SearchScreen and HomeScreen.
2. **Download failures** ✅
   - Download fails: "Download failed: $msg" with inline "Retry" button; calls `retryDownload(id)`. `lastFailedDownloadId` in DownloadManager.
3. **Search failures** ✅
   - Search throws on error; SearchScreen catches, shows "Search failed" with "Retry" button. Retry re-runs search.
4. **Empty / error states**
   - No network hint: optional. Downloaded badge: already shown.
5. **Consistency** ✅
   - Snackbar with Retry for stream; inline Retry for download and search.

### Testing:
- [ ] Stream failure shows retry; retry works
- [ ] Download failure shows retry; retry works
- [ ] Search failure shows retry
- [ ] Offline state is clear

### Deliverables:
- Retry UX for stream, download, and search failures; clear error messages; offline hint.

---

## Phase 48: Now Playing Indicator & Search Suggestions ✅
**Goal:** Clear visual indicator for current track in lists; suggest recent searches as you type.

### Tasks:
1. **Now playing indicator** ✅
   - Library and Playlist: yellow accent bar (4×40dp) on left of row when track.id == currentPlayingTrackId. Queue in PlayerScreen already had "Now playing" highlight.
2. **Search suggestions** ✅
   - When user types 2+ chars: show suggestions filtered by prefix from getRecentSearches. Max 8. Tap runs search. "Clear history" when recent searches exist.
3. **UI** ✅
   - Accent bar on left; suggestions as TextButtons below search field.

### Testing:
- [ ] Now playing indicator appears on correct track in all relevant lists
- [ ] Indicator updates when track changes
- [ ] Search suggestions show recent searches; tap runs search
- [ ] Clear history removes suggestions

### Deliverables:
- Now playing indicator in Library, Playlist, Queue; search suggestions from recent searches.

---

## Phase 32+: Roadmap — Analytics, Fun & Polish

Brainstorm of features to make Shuckler more satisfying, fun, and smooth to use. Implement in any order; each can become its own phase when ready.

---

### Analytics & Stats (beyond Phase 21e)

| Feature | Description |
|---------|-------------|
| **Listening time by day of week** | Bar chart (Mon–Sun) showing which days you listen most |
| **Time-of-day heatmap** | Morning / afternoon / evening listening patterns |
| **Listening streak** | "You've listened for X days in a row" |
| **Listening personality** | Labels like "Night owl," "Weekend warrior," "Morning listener" based on patterns |
| **Total listening time milestones** | "You've listened for 100 hours!" celebrations |
| **Achievement badges** | "First playlist," "10 favorites," "100 plays," "7-day streak," etc. |
| **Shareable stats cards** | "My top 5 this month" or "Listening time" as shareable image (Instagram-style) |
| **Library completion rate** | % of downloaded tracks you've actually played |
| **Skip vs finish rate** | How often you finish tracks vs skip |
| **"Rediscover" stat** | Tracks you haven't played in 30+ days |
| **Year-in-review / Wrapped-style** | Annual recap: top tracks, artists, playlists, total time, "first song of the year" |
| **Monthly recap** | Same idea, monthly |
| **"First song of the day" history** | What was your first track each day |
| **This week vs last week** | Comparison of listening time |
| **Most played time of day** | When you listen most (e.g. 9pm) |

---

### Fun & Satisfying Features

| Feature | Description |
|---------|-------------|
| **"Surprise me"** | Random track from library (or rarely played) |
| **"Throwback"** | Something you haven't played in months |
| **"On this day"** | What you listened to on this date last year (if history exists) |
| **"Shuffle roulette"** | Random track with a brief animation before it plays |
| **Listening goals** | "Listen 30 minutes today" with progress bar |
| **Achievement unlocks** | Badges for milestones (first download, first playlist, etc.) |
| **Mood/vibe tags** | Tag tracks or playlists (chill, workout, focus) and filter by mood |
| **Track notes** | Optional personal notes per track |
| **Audio visualizer** | Waveform or spectrum visualizer in Player screen |
| **Animated progress ring** | Circular progress around album art (in addition to seek bar) |
| **"Daily mix"** | Auto-generated mix based on listening habits (Spotify-style) |
| **Focus mode** | Timer + playlist for focus sessions (Pomodoro-style) |

---

### Ease of Use & Smoothness

| Feature | Description |
|---------|-------------|
| **"Continue listening"** | Resume long tracks from last position (especially for podcasts/long compilations) |
| **"Play this again"** | Add entire recent listening session to queue |
| **"Recently played"** | Dedicated section with quick play |
| **"Download all" from playlist** | Batch download all tracks in a playlist |
| **"Add all to playlist"** | Add all search results to a playlist in one tap |
| **"Up next" preview** | Show next 2–3 tracks in mini-player |
| **Preload next track** | Buffer next track in queue while current plays |
| **Gapless playback** | Seamless transition between tracks (ExoPlayer supports) |
| **Haptic feedback** | Light vibration on play/pause, favorite toggle, key actions |
| **Double-tap to seek** | Double-tap left/right of seek bar to skip ±10 seconds |
| **Queue reorder** | Drag to reorder queue items |
| **Search in queue** | Filter current queue by title/artist |
| **Quick playlist create** | "Add to new playlist" creates playlist and adds track in one tap |
| **Auto-playlist cover** | Collage from first 4 track thumbnails for playlist cover |
| **"Don't play this again"** | Temporarily exclude from shuffle (e.g. 30 days) |
| **Auto-play when queue ends** | Option: "Shuffle library" or "Play similar" when queue finishes |
| **Quick filters in Library** | "Downloaded only," "Recently added," "Never played" |
| **"Clean up" suggestion** | Suggest tracks to delete (never played, very old, etc.) |
| **Remember scroll position** | Restore scroll in Library/Search when returning to tab |
| **Swipe to delete** | Swipe Library/Playlist items to delete (with undo snackbar) |

---

### Notifications & Widgets

| Feature | Description |
|---------|-------------|
| **Widget: seekable progress** | Progress bar on widget (tap to seek) |
| **Notification: lyrics preview** | Show current lyric line in notification when space allows |
| **Notification: progress bar** | Seekable progress in media notification |
| **Widget: compact vs full** | Different sizes (2x2, 4x2) with more/less info |

---

### Settings & Preferences

| Feature | Description |
|---------|-------------|
| **Default tab** | Choose which tab opens on app launch (Home, Search, Library, Analytics) |
| **Stream vs download default** | Prefer "Play" (stream) or "Download" as primary action |
| **Wi‑Fi only downloads** | Option to only download when on Wi‑Fi |
| **Cache stream buffer** | How much to buffer when streaming (e.g. 30 s, 60 s) |
| **Data usage hint** | When streaming: "Streaming uses data" tooltip or setting |

---

### Suggested Implementation Order

**High impact, lower effort:**
1. Listening streak + day-of-week chart
2. "Surprise me" / "Throwback"
3. "Continue listening" (resume position)
4. Haptic feedback
5. Double-tap to seek

**Medium effort, high satisfaction:**
6. Achievement badges
7. Shareable stats cards
8. "Up next" preview in mini-player
9. Queue reorder (drag)
10. Gapless playback + preload next

**Larger features:**
11. Year-in-review / Wrapped-style
12. Mood/vibe tags
13. Focus mode (Pomodoro + playlist)
14. "Daily mix" auto-playlist

---

## Technical Notes: Preview vs Download, Spotify Import

### Why is Preview instant but Download slow?

- **Preview (instant):** Uses ExoPlayer to *stream* the audio. ExoPlayer buffers a few seconds, then plays. It fetches more data as you listen. No full file is saved — we just read chunks on demand from the stream URL.
- **Download (slow):** Saves the *entire* file to disk. For a 3‑minute song at ~192 kbps, that’s ~4 MB. The app reads every byte from YouTube’s CDN and writes it to storage. Speed depends on your connection and YouTube’s response (they may throttle non-browser downloads).
- **YouTube Music / Spotify (instant):** They stream from their own servers. When you press play, they send the first chunk right away. They don’t “download” the full file first — they stream chunks like our preview.

### Will Spotify import make downloads faster?

**No.** Spotify import gives you a *list* of tracks (title + artist). To get audio, we still search YouTube and download from YouTube for each track. The source and speed stay the same. Spotify import mainly saves you from manually searching for each song.

### Why can downloads fail with "unexpected end of stream"?

YouTube stream URLs are temporary. If the connection stalls, YouTube throttles, or the URL expires mid-download, the server may close the connection. We then see "unexpected end of stream." The fix: fetch a **fresh stream URL** on each retry instead of reusing the same one.

---

## Development Notes

### Key Android Concepts to Understand:
- **Foreground Service:** Required for background audio playback. Must show persistent notification.
- **MediaSession:** System-level API for media controls. Integrates with lock screen, headphones, Android Auto, etc.
- **Scoped Storage:** Android 10+ restricts external storage access. Use app-specific directories.
- **Audio Focus:** Handle interruptions (calls, other apps) gracefully.
- **Lifecycle:** Understand Activity/Fragment/Service lifecycles for proper state management.

### Recommended Libraries:
- **ExoPlayer (Media3):** Modern, powerful media player. Better than MediaPlayer for most use cases.
- **Coroutines:** For async operations (downloads, database access).
- **Room Database:** For storing track metadata (optional, can use simpler solutions initially).

### Testing Strategy:
- Test each phase on a real device (S22 Ultra) when possible
- Test with headphones to verify media button support
- Test screen-off playback early (Phase 3)
- Test long compilation downloads (Phase 6+)
- Keep emulator for quick iteration, but verify on real device

---

## Getting Started

Begin with **Phase 1** and work through each phase sequentially. After completing each phase:
1. Test thoroughly
2. Fix any bugs
3. Commit code (if using version control)
4. Move to next phase

**Phases 16–27** (optional features): implement in the recommended order given in the Phase 15 overview, or in any order that fits your priorities. Dependencies: Phase 20 (playlists) reuses Phase 16 queue actions; Phase 21 (UI) may reference Library/Playlists layout from Phase 20.

**Phases 32–43** (feel-good features): Continue listening, haptics, double-tap seek, recently played, Surprise me/Throwback, up next & queue reorder, gapless & preload, achievements, swipe-to-delete & scroll memory, download all from playlist, empty states, default tab & Wi‑Fi only, exclude from shuffle.

**Phases 44–48** (polish & first-run): App shortcuts, onboarding, share track/playlist, error handling & retry, now playing indicator & search suggestions.

**Phase 32+** (roadmap): Additional analytics, fun features, and polish ideas. Each item can become its own phase when ready; see suggested implementation order in that section.

---

## Phase 49: Listening Personality, Mood Tags, Smart Playlists, Clean Up, Animations & Accessibility ✅

**Goal:** Add listening personality labels, mood tags for tracks, smart playlists, clean-up suggestions, animated progress ring, and accessibility options (reduced motion, high contrast, TalkBack).

### Tasks:
1. **Listening personality** ✅
   - `ListeningPersonalityManager` records play session timestamps; computes label (Night owl, Morning listener, Weekend warrior, etc.) from time-of-day and weekday/weekend patterns.
   - Displayed in Analytics screen as a card with emoji, label, and description.
2. **Mood tags** ✅
   - Add `moodTags: Set<String>` to `DownloadedTrack`; persist in metadata.
   - Mood tag dialog: preset moods (chill, workout, focus, etc.) + custom tags.
   - Filter chips in Library for mood filtering.
3. **Smart playlists** ✅
   - Virtual playlists: Most played, Recently added, Never played, Favorites.
   - Tappable chips in Library; `SmartPlaylistScreen` shows filtered list with full track actions.
4. **Clean up suggestions** ✅
   - "Clean up suggestions" in storage section opens dialog.
   - Suggests: never played, or last played > 90 days ago with play count < 2.
   - Remove button per track.
5. **Animated progress ring** ✅
   - Circular progress around album art in Player screen; shows playback progress.
6. **Accessibility** ✅
   - **Reduce motion:** Setting toggles; when on, disables list item placement animation and shortens favorite scale animation.
   - **High contrast:** Setting toggles; applies high-contrast color scheme (white on black, bright yellow accent).
   - **TalkBack:** contentDescription added to key icons (album art, queue, settings, search, create playlist).

### Testing:
- [ ] Listening personality appears in Analytics after 5+ plays
- [ ] Mood tags persist; filter works
- [ ] Smart playlists show correct counts and play
- [ ] Clean up suggests appropriate tracks
- [ ] Progress ring animates with playback
- [ ] Reduce motion disables animations
- [ ] High contrast applies immediately

### Deliverables:
- Listening personality in Analytics
- Mood tags with filter in Library
- Smart playlists (Most played, Recently added, Never played, Favorites)
- Clean up suggestions dialog
- Animated progress ring in Player
- Accessibility settings (reduce motion, high contrast) and TalkBack support

---

If you encounter issues in a phase, fix them before proceeding. Don't accumulate technical debt.
