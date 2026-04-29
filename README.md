# Shuckler

A feature-complete personal Android music app built with Jetpack Compose — offline playback, YouTube integration, Spotify discovery, lyrics, analytics, and a polished animated UI across 50+ implemented development phases.

> Personal/non-commercial use only. Not distributed on the Play Store.

---

## Features

### Playback
- Full **background playback** via a foreground service with MediaSession integration
- **Gapless playback** using ExoPlayer/Media3 playlist mode
- **Crossfade** between tracks with configurable duration
- **Shuffle** and **repeat** modes (off / repeat-one / repeat-all)
- **Variable playback speed** (0.5× – 2×) persisted across sessions
- **Sleep timer** — fixed duration or end-of-current-track
- **Queue management** — reorder, jump to any track, swipe to remove
- Lock screen, notification, and headphone button controls via MediaSession
- Audio visualizer (FFT waveform) in the full-screen player

### Library
- **Offline downloads** stored in app-specific storage (no internet required after download)
- **Favorites** — heart toggle protects tracks from auto-delete; spring-bounce animation
- **Playlists** — create, rename, delete, reorder, add/remove tracks
- **Grid/list toggle** in Library with animated crossfade transition
- **Swipe-to-delete** with progressive delete-icon reveal animation
- **Import** local audio files from device storage
- **Storage management** — usage stats, bulk delete, configurable auto-delete policy
- Wi-Fi-only download option

### Discovery
- **YouTube search** via NewPipe Extractor — no API key required
- **30-second track preview** before downloading
- **Spotify integration** — browse top tracks, recently played, and personalized recommendations via OAuth 2.0 PKCE (no server required)
- **Recommendation engine** — suggests tracks based on play count and listening history
- Artist/album cover crop dialog for custom thumbnails

### Analytics
- **Play count** tracking per track with time-range filter (24 h / 7 d / 30 d / all time)
- **Top played** chart with relative bar visualization
- **Achievements** — badge system (e.g. "First Download", "Marathon Listener")
- **Listening personality** — computed archetype from listening patterns (e.g. "Deep Listener", "Variety Seeker")

### UX & Polish
- **Lyrics** — synced/unsynced lyrics from LRCLIB API with auto-scroll and karaoke highlight
- **Dynamic color theming** — album art colors extracted via AndroidX Palette, applied to player UI
- **AMOLED / dark / light** themes with Material 3 color tokens
- **Spring-physics animations** throughout (favorite bounce, press-scale feedback, swipe reveals)
- **Reduce motion** setting disables all animations system-wide
- **Home screen widget** — now-playing with play/pause control
- **App shortcuts** — quick-launch Search or resume playback from launcher long-press
- **Onboarding flow** — first-launch walkthrough
- **Equalizer** preferences screen
- Haptic feedback on key interactions
- Mini player bar with progress indicator, up-next strip, and press-scale button feedback

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Audio engine | ExoPlayer / AndroidX Media3 |
| System integration | MediaSession, MediaStyle notification, foreground service |
| YouTube extraction | [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) v0.25.1 |
| Spotify | Spotify Web API, OAuth 2.0 PKCE (no backend) |
| Lyrics | [LRCLIB](https://lrclib.net) REST API |
| Image loading | Coil 2 with crossfade |
| Dynamic color | AndroidX Palette |
| Networking | OkHttp 4 |
| Async | Kotlin Coroutines + StateFlow / Flow |
| Persistence | SharedPreferences (settings, queue state, playback position) |
| Navigation | AndroidX Navigation Compose |
| Min SDK | Android 10 (API 29) |
| Target SDK | Android 16 (API 36 "Baklava") |

---

## Architecture

```
UI (Compose screens)
    │  collectAsState()
    ▼
PlayerViewModel / DownloadManager (StateFlow)
    │  startForegroundService / serviceConnection
    ▼
MusicPlayerService (foreground service)
    │  ExoPlayer / Media3
    ▼
MediaSession ──► System (lock screen, notification, headphones)
```

- **MVVM** — `PlayerViewModel` exposes `StateFlow` properties consumed by Compose screens via `collectAsState()`
- **Foreground service** (`MusicPlayerService`) owns the ExoPlayer instance and all playback state; it outlives any Activity
- **`MusicServiceConnection`** binds the ViewModel to the service; exposed via `CompositionLocal` so any screen can reach it without prop-drilling
- **`CompositionLocal` provider pattern** — `DownloadManager`, `PlaylistManager`, `AchievementManager`, `ListeningPersonalityManager`, and others are injected once at the app root and accessed from any composable
- **`ShucklerApplication`** wires all singleton managers at startup

---

## Project Structure

```
app/src/main/java/com/shuckler/app/
├── MainActivity.kt
├── ShucklerApplication.kt
├── accessibility/          # Reduce motion, font scale preferences
├── achievement/            # Badge definitions, unlock logic, AchievementManager
├── download/               # DownloadManager, DownloadedTrack model, persistence
├── equalizer/              # EQ band preferences
├── lyrics/                 # LRCLIB API client, LyricsRepository, sync/unsync result types
├── navigation/             # NavGraph, route constants
├── onboarding/             # First-launch state
├── personality/            # Listening archetype computation
├── player/                 # MusicPlayerService, PlayerViewModel, MusicServiceConnection,
│                           #   QueueItem, DefaultTrackInfo
├── playlist/               # PlaylistManager, Playlist / PlaylistEntry models
├── preview/                # 30-second preview player
├── recommendation/         # RecommendationEngine (history-based suggestions)
├── shortcut/               # Launcher app shortcuts
├── spotify/                # SpotifyAuthManager (PKCE), SpotifyRepository
├── ui/                     # All Compose screens and shared components
│   ├── AnalyticsScreen.kt
│   ├── CreateScreen.kt     # Playlist creation
│   ├── CropCoverDialog.kt
│   ├── EqualizerScreen.kt
│   ├── HomeScreen.kt       # Recommendations, recents, hero banner
│   ├── ImportDialog.kt
│   ├── LibraryScreen.kt    # Downloads, favorites, grid/list toggle, swipe-delete
│   ├── MiniPlayerBar.kt    # Persistent bottom player strip
│   ├── OnboardingScreen.kt
│   ├── PlayerScreen.kt     # Full-screen player, lyrics, queue, visualizer
│   ├── PlaylistScreen.kt
│   ├── ScreenHeader.kt
│   ├── SearchScreen.kt     # YouTube search + preview
│   ├── SettingsDialog.kt
│   └── theme/              # Material 3 color tokens, typography, AMOLED palette
├── util/
│   └── ShareUtil.kt
├── widget/
│   └── NowPlayingWidgetProvider.kt
└── youtube/
    ├── OkHttpDownloader.kt # OkHttp bridge for NewPipe Extractor
    ├── YouTubeModel.kt
    └── YouTubeRepository.kt
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later (for SDK + build tools)
- Android 10+ device or emulator
- (Optional) Spotify Developer account — add `SPOTIFY_CLIENT_ID` to `local.properties` to enable Spotify features

### Setup

```bash
git clone https://github.com/<you>/shuckler.git
cd shuckler
# Optional: echo "SPOTIFY_CLIENT_ID=your_id_here" >> local.properties
```

1. Open the project in Android Studio
2. Sync Gradle
3. Run on a device or emulator (API 29+)

> See [ANDROID_SETUP.md](./ANDROID_SETUP.md) for detailed environment setup.

---

## Screenshots

*Coming soon — see `/docs/screenshots/` once captured.*

Screens to capture: Home, Player (album art + lyrics), Library (grid view), Analytics, Onboarding.

---

## Development History

50+ incremental development phases — from basic local file playback all the way through Spotify OAuth, achievement systems, dynamic theming, and spring-physics micro-interactions.

Full phase breakdown: [PHASES.md](./PHASES.md)

---

## License

MIT — see [LICENSE](./LICENSE)
