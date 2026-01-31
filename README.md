# Shuckler

A personal, non-commercial Android music app for offline music playback with YouTube integration.

## Overview

Shuckler is a private Android application designed for personal use and sharing with a few friends. It allows users to search for music (including long 1–2 hour compilations), download audio locally, and play music offline with full background playback support.

## Key Features

- **Music Search**: Search for music and long compilations (1–2 hours)
- **Local Downloads**: Download audio as MP3 files (192–320 kbps preferred)
- **Offline Playback**: Play downloaded music without internet connection
- **Background Playback**: Music continues playing when screen is off
- **Media Controls**: Full support for:
  - Headphone button controls (play, pause, next, previous)
  - Lock screen media controls
  - Notification media controls
  - Loop functionality
- **Audio Management**:
  - Cache downloaded audio locally
  - Mark tracks as favorites (protected from auto-delete)
  - Optional auto-delete after playback (except favorites)
  - Storage management

## Platform & Stack

- **Platform**: Android (target device: Samsung S22 Ultra)
- **Language**: Kotlin
- **IDE**: Cursor (for development) + Android Studio (for building/running)
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Android 16 (API 36 "Baklava")
- **Architecture**:
  - Foreground Service for background audio playback
  - MediaSession for system media controls
  - App-specific storage for downloaded audio

## Project Status

**In Development** - Phase 3 complete (background playback)

See [PHASES.md](./PHASES.md) for detailed development phases and progress.

## Development Phases

The project is broken down into 10 incremental phases:

1. [x] Project Setup & Basic UI Foundation
2. [x] Basic Audio Playback (Local File) - **See [PHASE2_SETUP.md](./PHASE2_SETUP.md)**
3. [x] Foreground Service for Background Playback
4. [ ] MediaSession & System Controls
5. [ ] Download Functionality (Basic)
6. [ ] YouTube Integration & Search
7. [ ] Library & Cache Management
8. [ ] Favorites & Auto-Delete
9. [ ] Polish & Optimization
10. [ ] Optional Enhancements (Future)

## Constraints & Design Decisions

- **Non-commercial**: Personal use only, not intended for Play Store distribution
- **No user accounts**: Simple, offline-first design
- **No ads**: Clean, distraction-free experience
- **Offline-first**: Designed to work primarily offline after initial downloads
- **Simple & maintainable**: First personal Android app, keeping code straightforward

## Getting Started

### Prerequisites

- Android Studio (for SDK, build tools, and emulator)
- Android device (Samsung S22 Ultra) or emulator for testing
- Kotlin knowledge (or willingness to learn)

### Setup

1. Clone this repository
2. Open project in Android Studio
3. Sync Gradle files
4. Set up emulator or connect physical device
5. Run the app

See [ANDROID_SETUP.md](./ANDROID_SETUP.md) for detailed setup instructions.

## Project Structure

```
shuckler/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/shuckler/app/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── MusicPlayerService.kt
│   │   │   │   ├── DownloadManager.kt
│   │   │   │   └── ...
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   └── values/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
├── PHASES.md          # Development phases
├── project.txt        # Initial project planning notes
└── README.md          # This file
```

## Key Components

- **MainActivity**: Main UI with navigation between Search, Library, and Player screens
- **MusicPlayerService**: Foreground service handling background audio playback
- **DownloadManager**: Handles audio downloads and storage management
- **MediaSession**: Integrates with system media controls (headphones, lock screen, notifications)

## Audio Quality

- Preferred format: MP3
- Bitrate: 192–320 kbps
- Optimized for long compilations without excessive storage usage

## License

MIT License - See [LICENSE](./LICENSE) file for details.

## Notes

- This is a personal project for learning Android development
- Not intended for public distribution
- Focus on simplicity and maintainability
- Test thoroughly on target device (Samsung S22 Ultra)

---

**Built for personal music enjoyment**
