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
- [ ] Timer stops playback after selected duration; fade (if implemented) works.
- [ ] Turning timer off cancels scheduled stop.

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
- [ ] Create playlist with name, description, and cover; appears in list.
- [ ] Add tracks from Library to playlist; remove and reorder; play playlist as queue.
- [ ] Deleting a library track removes it from playlists (or UI hides it); deleting playlist removes data and cover file.

### Deliverables:
- Custom playlists with name, description, optional cover image; add/remove/reorder tracks; play as queue; persistent storage.

---

## Phase 21: Modern UI Refresh (Spotify / YouTube Music / Apple Music Style)
**Goal:** Update the app’s look and feel to feel more modern and cohesive (Spotify, YouTube Music, or Apple Music inspired), with targeted animations where they don’t require risky refactors.

### Tasks:
1. **Design direction**
   - Choose a consistent direction: e.g. **Spotify:** dark accent, rounded cards, bold headings, green CTA. **YouTube Music:** card-heavy, thumbnails prominent, red accent. **Apple Music:** clean, lots of white space, subtle shadows. Apply one consistently: color palette (primary/secondary), card shape (rounded corner radius), typography (title/body scale), spacing (padding/margins).
2. **Global styling**
   - Theme: adjust ColorScheme (primary, surface, background), Shape (componentsDefaultCornerSize, cards), Typography (headlineMedium, titleLarge, etc.). Keep existing theme mode (light/dark/system); ensure both look good.
3. **Screens**
   - **Search:** search bar prominence; result cards with consistent elevation and corner radius; thumbnail size/spacing. **Library:** section headers (e.g. “Playlists,” “Tracks”); list item height and padding; Favorites chip styling. **Player:** large artwork; title/artist hierarchy; control buttons size and spacing; seek bar style. Optional: “Now playing” bar at bottom of Search/Library that taps through to Player (minimized now-playing strip).
4. **Navigation**
   - Current tab bar (Search, Library, Player): restyle with icons and labels; selected state (e.g. primary color). If adding Playlists, add as tab or under Library; keep navigation simple.
5. **Animations (safe)**
   - Keep existing: tab transition (slide), list item placement (animateItem), favorite scale. Add only where low-risk: e.g. button press scale (already have ripple), list item fade-in on first load (initial animation), or subtle progress indicator. **Avoid:** broad changes to NavHost/navigation structure, or animations that depend on rewriting entire screens.
6. **Assets**
   - No new app icon required; optional: adjust default placeholder for “no artwork” (e.g. gradient or icon). Ensure playlist placeholder (Phase 20) fits the new style.

### Testing:
- [ ] Light and dark themes look consistent and modern; no contrast or readability regressions.
- [ ] All existing flows (search, download, library, play, queue, settings) still work; no layout breaks.

### Deliverables:
- Cohesive modern UI (colors, shapes, typography, spacing); optional small animation tweaks; no large structural refactors.

### Notes:
- If a full “bottom sheet now playing” or “mini player” is desired, treat it as a follow-up task after Phase 21 to avoid scope creep.

---

## Phase 22: Home Screen Widget
**Goal:** A home screen widget showing now-playing info and play/pause (and optionally next/previous).

### Tasks:
1. **Widget**
   - Use App Widget (Android XML layout + AppWidgetProvider) or Jetpack Glance (Compose for widgets, if min SDK allows). Widget layout: small/medium size: album art (or placeholder), title, artist, play/pause button. Optional: next/previous buttons. Tapping widget opens the app (or opens Player tab).
2. **Updates**
   - When playback state or track changes, update widget (e.g. MusicPlayerService calls AppWidgetManager.updateAppWidget). Use RemoteViews (or Glance) to set title, artist, and play/pause drawable (play vs pause icon).
3. **Actions**
   - Play/pause (and next/previous if present) send broadcast or start service with action (e.g. ACTION_PLAY, ACTION_PAUSE). Service handles as in existing onStartCommand. Optional: tap artwork to open app.
4. **Artwork**
   - If feasible, set widget’s album art from current track thumbnail (load bitmap and set on ImageView in RemoteViews). Fallback: app icon or placeholder.

### Testing:
- [ ] Widget shows current track and play/pause state; tapping play/pause toggles playback.
- [ ] Widget updates when track changes or app is in background.

### Deliverables:
- Working home screen widget (play/pause, optional next/previous); updates with now-playing info.

---

## Phase 23: Preview Before Download
**Goal:** In Search (YouTube), play a short preview (e.g. 30–60 seconds) from the stream URL without saving; “Download” still saves the full file.

### Tasks:
1. **Preview playback**
   - When user taps “Preview” (or similar) on a search result: obtain stream URL via existing getAudioStreamUrl. Play in ExoPlayer (either a temporary/secondary player in the app, or reuse MusicPlayerService with a “preview mode” that stops after N seconds and doesn’t add to queue). Stop after 30–60 s (or when user taps stop). Do not save to disk.
2. **UI**
   - Add “Preview” button next to “Download” on YouTube search results. While preview is playing: show “Preview playing…” and a stop button; optional progress for the 30–60 s window.
3. **Service vs in-app player**
   - Option A: Use MusicPlayerService with a flag “previewOnly” and a timer; when timer fires or track would exceed 60 s, stop and clear. Option B: Use a separate ExoPlayer instance in the ViewModel/Composable scope for preview only (simpler but doesn’t use notification; user can’t leave app and keep preview). Choose based on desired UX.
4. **Edge cases**
   - If user starts a full download while preview is playing, stop preview and start download. If user leaves Search during preview, decide: stop preview or let it play (simpler to stop).

### Testing:
- [ ] Preview plays for up to 30–60 s then stops; Download still saves full file.
- [ ] No leftover preview state that breaks normal playback.

### Deliverables:
- Preview button in Search; short preview playback from stream URL without saving.

---

## Phase 24: Recommendation System (Simple)
**Goal:** Surface “For you”–style content using simple rules: recent searches and favorites (no ML).

### Tasks:
1. **Data**
   - Persist last N search queries (e.g. 10–20) in SharedPreferences or a small JSON file. We already have favorites (DownloadedTrack.isFavorite) and play counts; no new backend.
2. **“For you” / Home section**
   - If we have a “Home” or “Listen now” surface (could be part of Phase 21 or a new tab): section “Recent searches” (tappable to run search again or show recent results if cached); section “From your favorites” (tracks that are favorite, or most-played). Alternatively: add “Recommended” or “Quick picks” at top of Library (e.g. “Recently played,” “Favorites,” “Recent searches” as horizontal chips or rows).
3. **Logic**
   - “Recently played”: last N tracks played (need to persist “last played” order or timestamps; e.g. add lastPlayedMs to metadata or a separate list). “Favorites”: filter library by isFavorite. “Recent searches”: list of saved query strings; tap opens Search with that query. Keep it simple; no collaborative filtering or external API.
4. **UI**
   - Small, scoped UI: e.g. a “Home” tab with 2–3 sections, or a collapsible “For you” block at top of Library. Don’t overwhelm; 1–2 rows of “Quick access” is enough.

### Testing:
- [ ] Recent searches appear and tapping one re-runs search (or fills search box).
- [ ] Favorites / recently played section shows correct tracks.

### Deliverables:
- Simple recommendations: recent searches and “from favorites” (and optionally recently played) surfaced in a dedicated section or Home.

---

## Phase 25: Equalizer
**Goal:** Let the user adjust bass/treble or a simple multi-band EQ using Android’s AudioEffect APIs.

### Tasks:
1. **API**
   - Use Android’s Equalizer (and optionally BassBoost) from android.media.audiofx. Attach to the same audio session as ExoPlayer. ExoPlayer/Media3: obtain audio session ID from the player and attach Equalizer to it. Enable/disable and set levels from UI.
2. **UI**
   - Settings or Player: “Equalizer” entry; open a simple screen with presets (Normal, Rock, Pop, etc.) and/or sliders for a few bands (e.g. 5-band). Persist user’s preset or custom levels in SharedPreferences.
3. **Compatibility**
   - Not all devices support Equalizer; check Equalizer.isAvailable() and hide or disable UI if not available. Handle cleanup when playback stops (release effect).

### Testing:
- [ ] When available, equalizer affects playback; settings persist.
- [ ] No crash when Equalizer is not available.

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

## Phase 27: Lyrics (Stretch)
**Goal:** Show lyrics in the Player screen if a source is available.

### Tasks:
1. **Source**
   - Options: (1) Embed lyrics in downloaded file (ID3/USLT or similar) and read with a library; (2) Use a third-party lyrics API (e.g. by artist + title); (3) Skip if no reliable free source. Document chosen approach. If no source is feasible, mark phase as “deferred” and do not implement.
2. **UI**
   - In Player: expandable “Lyrics” section or a “Lyrics” tab/sheet. Show synchronized lyrics (timestamp + line) if available; otherwise plain text. Scrolling and optional highlight of current line (using playback position) improve UX.
3. **Storage**
   - If from API: cache lyrics by (artist, title) in app storage to avoid repeated requests. TTL or versioning optional.

### Testing:
- [ ] When lyrics are available, they display and (if synced) highlight current line.
- [ ] No crash when lyrics are missing or API fails.

### Deliverables:
- Lyrics in Player when source is available; or document “deferred until source identified.”

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

If you encounter issues in a phase, fix them before proceeding. Don't accumulate technical debt.
