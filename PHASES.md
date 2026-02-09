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

**Static when playing:** Often an **emulator** issue (buffer/sample-rate quirks). Test on a **real device** first. If it still happens, try updating Media3/ExoPlayer and/or adjusting buffers; see Phase 12.

**Lossless compression:** Android supports **FLAC** (lossless). **Opus** is lossy but very efficient. Phase 14 covers quality/lossless options and storage.

**Parity ideas (YouTube Music / Spotify style):** Queue + Next/Previous (Phase 10), smooth animations (Phase 11), crossfade (Phase 12), artwork/thumbnails (Phase 13), quality/bitrate options (Phase 14), then optional: preview before download, playback speed, equalizer, sleep timer, widget, offline search, custom playlists (Phase 15). Metadata/art is attainable for YouTube-sourced tracks via thumbnail URLs; full video metadata is optional.

---

## Phase 10: Queue, Next/Previous & Basic Playlist
**Goal:** Make Next/Previous buttons functional with a play queue.

### Why it was deferred:
- Next/Previous require a queue (list of tracks to play). Without a queue there is no "next" or "previous" track, so the buttons were no-ops until this phase.

### Tasks:
1. Define a play queue (in-memory or persisted)
   - Queue: ordered list of track URIs + metadata (title, artist, optional trackId for library items)
   - Current index into the queue
2. Integrate queue with MusicPlayerService
   - On track end (STATE_ENDED): if repeat mode off, advance to next queue item; if at end, stop
   - Next: skip to next item in queue (or stop if last)
   - Previous: go to previous item or restart current (e.g. if under 3s in, go to previous; else restart current)
3. Populate queue from Library
   - "Play" from Library: set queue to full library (or filtered list), set current index to selected track, play
   - Optional: "Play next" / "Add to queue" from Library or Search (add single track to queue)
4. Update Player UI
   - Next/Previous buttons call service methods that use the queue
   - Optional: show "Track X of Y" or queue length

### Testing:
- [ ] Next advances to next track in queue
- [ ] Previous goes to previous or restarts current
- [ ] Playing from Library sets queue and plays selected track; Next/Previous work
- [ ] When queue ends (no repeat), playback stops

### Deliverables:
- Functional Next/Previous
- Play queue populated from Library (and optionally from Search)

---

## Phase 11: Animations & UX Polish
**Goal:** Smooth, satisfying animations and transitions (Spotify/YouTube Music–style feel), plus app theme support.

### Tasks:
1. **App theme (light / dark / follow system)**
   - Add a theme setting: **Light**, **Dark**, or **Follow system** (use system light/dark setting).
   - Persist the choice (e.g. SharedPreferences or DataStore).
   - Apply theme at app startup and when the setting changes (Compose: use `MaterialTheme.colorScheme` from a theme that respects the setting; wrap app in a theme that reads the preference and uses `darkTheme =` / `ColorScheme` accordingly).
   - Expose the setting in the existing Settings dialog (Player tab) or a dedicated "Appearance" section (e.g. dropdown or list choice).
2. Screen and transition animations
   - Animated transitions between tabs (e.g. fade/slide)
   - Fragment/screen enter/exit transitions
3. List and item animations
   - Animate list items on appear (e.g. staggered fade-in or slide)
   - Smooth scroll behavior; consider item animations on scroll
4. Loading and feedback
   - Skeleton loaders or shimmer for Search results and Library
   - Button/layout state changes with subtle scale or opacity animation
   - Pull-to-refresh on Search or Library (if applicable)
5. Micro-interactions
   - Play button press feedback (e.g. ripple, scale)
   - Favorite heart animation (e.g. brief scale or fill animation)
   - Seek bar thumb feedback

### Testing:
- [ ] Theme setting switches between Light, Dark, and Follow system correctly
- [ ] Theme choice persists after app restart
- [ ] Transitions feel smooth and consistent
- [ ] No jank or dropped frames on target device

### Deliverables:
- Light / Dark / Follow system theme with persisted setting
- Cohesive animation set for main flows
- Improved perceived quality and "polish"

---

## Phase 12: Crossfade & Audio Quality
**Goal:** Crossfade between tracks (Spotify-like) and reduce/eliminate static or glitches.

### Tasks:
1. Crossfade between tracks
   - ExoPlayer/Media3 has no built-in crossfade; use **volume-based crossfade** with two player instances (or fade out current, then switch and fade in next)
   - Optional: configurable crossfade duration (e.g. 3–10 seconds) in Settings
   - When queue advances: start next track at 0 volume, fade out current while fading in next over N seconds
2. Static / crackling (research and mitigation)
   - **Emulator note:** Static and glitches are common on the Android emulator (buffer size, sample rate quirks). Always verify playback on a **real device** (e.g. S22 Ultra) before assuming an app bug.
   - If static persists on real device: ensure Media3/ExoPlayer is up to date (SilenceSkippingAudioProcessor bugs have been fixed in newer versions)
   - Optional: try increasing buffer sizes or disabling silence-skipping if using custom pipeline
   - Add a short "Audio troubleshooting" note in app docs or developer notes
3. Optional: audio focus and ducking
   - Ensure other apps (e.g. navigation) can take focus and Shuckler ducks correctly

### Testing:
- [ ] Crossfade works when moving to next track (no hard cut)
- [ ] Playback on real device: confirm whether static is emulator-only or needs code fix

### Deliverables:
- Crossfade option (and setting for duration)
- Documented approach for static (emulator vs device, Media3 version)

---

## Phase 13: Artwork & Metadata
**Goal:** Show track art (e.g. thumbnails) in Library, Player, and notification where feasible.

### Tasks:
1. Artwork source
   - **YouTube:** Thumbnail URL is available from search results and from extractor (e.g. NewPipe/yt-dlp). Store thumbnail URL or download and cache a small image per track in metadata.
   - For direct MP3 URLs: no thumbnail unless we add a separate "artwork URL" field or embed in ID3 (advanced).
2. Display artwork
   - Library list: small thumbnail per track (or placeholder icon if none)
   - Player screen: larger art (e.g. album-art style) with placeholder when missing
   - Notification: set large icon / artwork in media notification (MediaStyle supports large icon)
3. Storage
   - Store thumbnail URL in track metadata, or cache image in app storage (e.g. in files dir by track id) and store path. Cache eviction when track is deleted.

### Notes:
- Full "video" or rich metadata (e.g. channel art, description) is optional and not required for parity; focus on thumbnail/art for now.

### Testing:
- [ ] YouTube-downloaded tracks show thumbnail in Library and Player when available
- [ ] Notification shows artwork when supported

### Deliverables:
- Thumbnail/art in Library, Player, and notification where we have a source (e.g. YouTube)

---

## Phase 14: Storage, Quality & Lossless Options
**Goal:** Offer quality options and explore lossless/compression for storage.

### Tasks:
1. Research and document
   - **Lossless:** Android supports **FLAC** (decode from 3.1+, encode from 4.1+). Good for archival/high fidelity; no quality loss, smaller than raw PCM.
   - **Opus:** Lossy but efficient; great quality/size; supported on Android 5+ (decode), 10+ (encode). Good alternative to MP3 for smaller files at similar quality.
   - **MP3:** Current default; widely compatible. Consider configurable bitrate (e.g. 128 / 192 / 320 kbps) if the download pipeline supports it (e.g. yt-dlp format selection).
2. Quality selector (download / settings)
   - Let user choose preferred format or bitrate (e.g. "High (320 kbps MP3)", "Normal (192 kbps)", "Save space (Opus or 128 kbps)")
   - If using yt-dlp or similar, map quality option to format codes
3. Storage optimization
   - Show "Storage used" breakdown (e.g. by format or by folder) in Settings or Library
   - Optional: "Re-encode to lower bitrate" or "Convert to Opus" for existing files to free space
4. Compression notes
   - Document in PHASES or README: FLAC for lossless; Opus for best lossy compression; MP3 for compatibility. No change required if current MP3 pipeline is sufficient.

### Testing:
- [ ] Quality option affects downloaded file format/bitrate when supported
- [ ] App works with FLAC/Opus if we add support (ExoPlayer supports both)

### Deliverables:
- Quality/format options in UI (where pipeline supports it)
- Documentation on lossless (FLAC) and compression (Opus) options

---

## Phase 15: Optional Features (Pick as Needed)
**Goal:** Extra features for parity or convenience; implement in any order.

### Features (choose which to implement):
1. **Preview before download** – In Search (YouTube), play a short preview (e.g. 30–60 s) from stream URL without saving; "Download" saves full file.
2. **Playback speed** – 0.5x, 1x, 1.25x, 1.5x, 2x (ExoPlayer supports setPlaybackSpeed).
3. **Equalizer** – Use Android AudioEffect (BassBoost, Equalizer) or a simple band EQ if desired.
4. **Sleep timer** – Stop playback after N minutes; optional fade-out.
5. **Home screen widget** – Show now playing and play/pause (and optionally next/previous).
6. **Offline search** – Search within downloaded library (by title/artist) without network.
7. **Custom playlists** – User-named playlists; add/remove library tracks; play playlist as queue.
8. **Split long compilations** – For very long YouTube videos, optional split by chapters or time intervals into separate tracks.
9. **"Play next" / "Add to queue"** – From Search or Library, add one track to queue or insert after current.
10. **Lyrics** – If a source is available (e.g. some APIs or embedded), show lyrics in Player (stretch goal).

### Implementation notes:
- Each item can be a small sub-phase or a single task block; no strict order.
- Prioritize based on user need (e.g. widget and sleep timer are high value for minimal effort).

### Deliverables:
- One or more of the above features, documented and tested.

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

If you encounter issues in a phase, fix them before proceeding. Don't accumulate technical debt.
