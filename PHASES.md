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
- [ ] Music continues playing when screen turns off
- [ ] Music continues when app is minimized
- [ ] Notification appears and shows playback controls
- [ ] Service survives app being swiped away (optional, depends on device)

### Deliverables:
- Background audio playback working
- Foreground service with notification

---

## Phase 4: MediaSession & System Controls
**Goal:** Headphone buttons, lock screen, and notification controls work

### Tasks:
1. Implement MediaSession
   - Create MediaSession in MusicPlayerService
   - Set metadata (title, artist, album art)
   - Handle media button events
2. Update notification
   - Add previous/next buttons
   - Add seek bar (optional for Phase 4)
   - Style notification as media notification
3. Implement MediaSession callbacks
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
1. Create `DownloadManager.kt`
   - Handle file downloads
   - Save to app-specific storage (`getExternalFilesDir()` or `getFilesDir()`)
   - Show download progress
2. Create download data model
   - Track metadata (title, artist, file path, duration)
   - Track download status
3. Implement storage management
   - Create app-specific audio directory
   - Handle file naming conflicts
   - Store metadata (use SharedPreferences or simple JSON for now)
4. Add download UI
   - Download button in search results
   - Progress indicator
   - Download status in library

### Testing:
- [ ] Can download a direct MP3 URL
- [ ] File saves to app storage
- [ ] Download progress is visible
- [ ] Downloaded file can be played

### Deliverables:
- Working download system for direct audio URLs
- Files stored in app-specific directory
- Basic download UI

---

## Phase 6: YouTube Integration & Search
**Goal:** Search YouTube and download audio using yt-dlp or similar

### Tasks:
1. Research YouTube audio extraction
   - Option A: Use yt-dlp via ProcessBuilder (requires root or external tool)
   - Option B: Use YouTube API + audio extraction library
   - Option C: Use a third-party library (NewPipe extractor, etc.)
   - **Recommendation:** Start with NewPipe extractor or similar Kotlin library
2. Implement YouTube search
   - Search YouTube for queries
   - Display results (title, thumbnail, duration)
3. Implement YouTube download
   - Extract audio URL or download directly
   - Convert to MP3 (if needed)
   - Save with proper metadata
4. Integrate with UI
   - SearchFragment shows YouTube results
   - Download button triggers download
   - Show download progress

### Testing:
- [ ] Can search YouTube
- [ ] Search results display correctly
- [ ] Can download audio from YouTube
- [ ] Downloaded audio plays correctly
- [ ] Works with long compilations (1-2 hours)

### Deliverables:
- YouTube search functionality
- YouTube audio download
- Integration with existing download system

---

## Phase 7: Library & Cache Management
**Goal:** Display downloaded tracks, manage storage

### Tasks:
1. Create LibraryFragment UI
   - List of downloaded tracks
   - Display track info (title, duration, file size)
   - Play button for each track
2. Implement track database/storage
   - Store track metadata (use Room database or simple JSON)
   - Track file paths, download dates, play counts
3. Implement library playback
   - Select track from library
   - Play selected track
   - Queue management (optional: play next)
4. Add storage management UI
   - Show total storage used
   - Delete individual tracks
   - Clear all cache option

### Testing:
- [ ] Library shows all downloaded tracks
- [ ] Can play tracks from library
- [ ] Can delete tracks
- [ ] Storage usage is accurate

### Deliverables:
- Functional library screen
- Track management (view, play, delete)
- Storage tracking

---

## Phase 8: Favorites & Auto-Delete
**Goal:** Mark favorites, auto-delete after playback (except favorites)

### Tasks:
1. Add favorites functionality
   - Favorite button on tracks
   - Store favorite status in database
   - Filter library by favorites
2. Implement auto-delete logic
   - Track playback completion
   - Delete track after playback (if not favorite)
   - Optional: Configurable auto-delete delay
3. Update UI
   - Favorite indicator in library
   - Settings/preferences for auto-delete
4. Handle edge cases
   - Don't delete currently playing track
   - Don't delete if user manually stopped

### Testing:
- [ ] Can mark/unmark favorites
- [ ] Favorites persist across app restarts
- [ ] Non-favorite tracks auto-delete after playback
- [ ] Favorite tracks are never auto-deleted
- [ ] Currently playing track is not deleted

### Deliverables:
- Favorites system
- Auto-delete functionality
- Settings/preferences UI

---

## Phase 9: Polish & Optimization
**Goal:** Improve UX, optimize for long compilations, add remaining features

### Tasks:
1. Implement loop functionality
   - Single track loop
   - Queue/playlist loop
   - UI toggle for loop mode
2. Optimize for long compilations
   - Efficient storage (check compression options)
   - Progress tracking for long downloads
   - Seek functionality in player
3. Improve UI/UX
   - Better loading states
   - Error handling and user feedback
   - Dark mode support (optional)
   - Responsive layouts
4. Add seek bar
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

## Phase 10: Optional Enhancements (Future)
**Goal:** Additional features if needed

### Potential Features:
- Split long compilations into individual tracks
- Offline search within cached songs
- Custom playlists
- Playback speed control
- Equalizer
- Sleep timer
- Widget for home screen

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
