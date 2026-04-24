# Shuckler — TikTok Phone Screen Prep

> This is interview prep for the Backend Software Engineer, Social Graph role at TikTok.
> All answers are written in first-person and are grounded directly in the codebase.

---

## Core Understanding

### What problem does this project solve?

Music streaming services lock you out when you're offline and don't let you download arbitrary
content (1–2 hour DJ mixes, compilations, lo-fi streams). Shuckler solves that by letting me
search YouTube, download the audio locally, and play it offline with full system integration —
lock screen controls, notification bar, headphone buttons — with zero subscriptions or ads.

The deeper problem it solves is ownership. Spotify removes songs when licenses change. This app
gives permanent local copies, organized by playlists, mood tags, and play history.

### What are the main components of the system?

| Component | File | Responsibility |
|---|---|---|
| `ShucklerApplication` | `ShucklerApplication.kt` | App singleton; lazily instantiates all services via Kotlin `by lazy`. |
| `MainActivity` | `MainActivity.kt` | Single-activity entry point. Handles permissions, Spotify OAuth callbacks, app shortcuts. Wires Compose DI via `CompositionLocalProvider`. |
| `MusicPlayerService` | `player/MusicPlayerService.kt` | Android Foreground Service. Owns ExoPlayer, the play queue, MediaSession, equalizer, visualizer, crossfade, sleep timer, and notification. |
| `MusicServiceConnection` | `player/MusicServiceConnection.kt` | Binds the UI layer to the service. Exposes `StateFlow`s so the UI can observe playback state reactively. |
| `PlayerViewModel` | `player/PlayerViewModel.kt` | Mediates between UI composables and the service connection. Translates UI events to service `Intent`s. |
| `DownloadManager` | `download/DownloadManager.kt` | Handles the full download lifecycle: resolving YouTube stream URLs, resumable HTTP download, file storage, metadata persistence (JSON), library CRUD, favorites, play counts, mood tags, chapter splitting. |
| `YouTubeRepository` | `youtube/YouTubeRepository.kt` | All YouTube I/O. Uses the NewPipe Extractor library to search YouTube and extract direct audio stream URLs — no official API needed. |
| `PlaylistManager` | `playlist/PlaylistManager.kt` | User-created playlists with ordered entries, cover images, and full CRUD. |
| `RecommendationEngine` | `recommendation/RecommendationEngine.kt` | Signal-weighted system that infers what the user likes (favorites, play counts, searches) and produces YouTube search queries for "Recommended for you." |
| `ListeningPersonalityManager` | `personality/ListeningPersonalityManager.kt` | Tracks listening sessions to infer a user's listening personality type. |
| `LyricsRepository` | `lyrics/LyricsRepository.kt` | Fetches lyrics for currently playing tracks. |
| `SpotifyAuthManager` | `spotify/SpotifyAuthManager.kt` | OAuth flow to import playlists from Spotify. |

### How does data flow through the system?

**Search & download flow:**
```
User types query
  → YouTubeRepository.search() [NewPipe Extractor over OkHttp]
  → Returns List<YouTubeSearchResult> (title, URL, thumbnail, duration, uploader)
  → User taps Download
  → DownloadManager.startDownloadFromYouTube(videoUrl)
    → YouTubeRepository.getAudioStreamUrl() [NewPipe extracts direct CDN URL]
    → HTTP GET with Range headers (resumable, 64KB buffer)
    → File saved to app-specific external Music directory
    → Metadata saved to downloads.json (JSON array, one object per track)
  → StateFlow<List<DownloadedTrack>> emits update → UI re-renders
```

**Playback flow:**
```
User taps play (Library or Search result)
  → PlayerViewModel sends ACTION_PLAY_WITH_QUEUE Intent to MusicPlayerService
  → MusicPlayerService.setQueueAndPlay():
    → Parses QueueItem list from JSON
    → Calls ExoPlayer.setMediaItems() (gapless) or setMediaItem() (crossfade mode)
    → ExoPlayer.play() → audio output
    → MediaSession.setPlaybackState() → lock screen / Bluetooth controls
    → NotificationManager.notify() → notification media controls
    → Widget updated via NowPlayingWidgetProvider
  → ExoPlayer.onIsPlayingChanged → StateFlow<Boolean> → UI updates play/pause button
  → Every 5 seconds: DownloadManager.updateLastPosition() → saves resume point to JSON
```

**Recommendation flow:**
```
RecommendationEngine.getRecommendationQueries(context, completedTracks)
  → Scores artists: favorites × 2 + (playCount × 0.5), capped at 10
  → Scores frequent searches (≥3 queries)
  → Returns top-5 ranked queries
  → Home screen runs each query via YouTubeRepository.search() in parallel (async/await)
  → Results deduplicated against existing library, displayed as "Recommended for you"
```

### What are the key algorithms used?

**Stream quality selection (`YouTubeRepository.selectStreamByQuality`):**
Sort all audio streams descending by `averageBitrate`. Break ties by preferring M4A (AAC) over
WebM/Opus for better device compatibility. "best" takes index 0, "high" takes index 1 (second-best
for data savings), "data_saver" takes the lowest-bitrate stream.

**Resumable download (`DownloadManager.runDownloadAttempt`):**
Implements HTTP range requests (RFC 7233). On each retry attempt: re-fetches a fresh stream URL
from YouTube (since their CDN URLs expire), sends `Range: bytes=N-`, handles `206 Partial Content`
vs. `416 Range Not Satisfiable` (falls back to scratch). Up to 5 retry attempts with 2-second
backoff between each.

**Chapter splitting (`DownloadManager.splitTrackByChapters`):**
Calls `YouTubeRepository.getChapters()` to fetch `streamSegments` from NewPipe (YouTube's chapter
markers). Creates "virtual tracks" — new `DownloadedTrack` records pointing to the same file path
but with `startMs`/`endMs` fields. ExoPlayer's `ClippingConfiguration` handles playback of just
that segment. The original file is not duplicated; it's deleted only when all chapter references
are gone.

**Gapless vs. crossfade playback:**
When crossfade duration is 0, calls `ExoPlayer.setMediaItems(allItems)` which lets ExoPlayer
preload the next track and transition seamlessly. When crossfade is enabled, manages a single
`setMediaItem()` at a time and drives fade-out/fade-in manually: 50ms timer steps, volume linearly
interpolated over the configured duration.

**Recommendation scoring:**
Each artist candidate gets a `score`. Favorites contribute `2 + playCount × 0.5`. Frequently
played (non-favorite) tracks contribute `min(playCount, 10)`. Frequent searches (3+ times) get a
fixed 2.0 score. Candidates are deduplicated by key (artist name), keeping the highest score.
Top-5 by score become the recommendation queries.

**Sleep timer fade-out:**
When the timer fires, if "fade last minute" is enabled, runs a 60-second linear volume fade in
50ms steps (`steps = 60000 / 50 = 1200 steps`), then pauses. This prevents jarring cuts.

---

## Architecture

### What is the high-level architecture?

Single-activity Android app with a **service-bound playback engine**. The architecture has three
layers:

```
┌─────────────────────────────────────────────┐
│  UI Layer (Jetpack Compose)                 │
│  Screens: Home, Search, Library, Player     │
│  State from StateFlows via collectAsState() │
└────────────────────┬────────────────────────┘
                     │ Intent / StateFlow
┌────────────────────▼────────────────────────┐
│  Service Layer                              │
│  MusicPlayerService (Foreground Service)    │
│  ExoPlayer, MediaSession, Equalizer,        │
│  Queue, Crossfade, Sleep Timer              │
└────────────────────┬────────────────────────┘
                     │ Coroutines / IO
┌────────────────────▼────────────────────────┐
│  Data Layer                                 │
│  YouTubeRepository (NewPipe + OkHttp)       │
│  DownloadManager (HTTP + local JSON + File) │
│  PlaylistManager, LyricsRepository          │
└─────────────────────────────────────────────┘
```

Key structural decisions:
- **No database** — metadata stored in a flat JSON file (`downloads.json`). Fast enough for
  hundreds of tracks, zero Room/SQLite overhead for a personal app.
- **No ViewModel for the service** — `MusicServiceConnection` is the bridge. It binds to the
  service and re-exposes its `StateFlow`s. `PlayerViewModel` wraps this for Compose lifecycle.
- **Dependency injection via `CompositionLocal`** — `LocalDownloadManager`, `LocalPlaylistManager`,
  etc. are Compose composition locals set at the root in `MainActivity`. No Hilt/Dagger.
- **`ShucklerApplication` as service locator** — all singletons are `by lazy` properties on the
  `Application` class. Services access them via `applicationContext as ShucklerApplication`.

### How would you explain this system to a non-technical person?

Imagine YouTube as a radio tower that streams music. Shuckler listens to that tower, records the
audio to your phone's storage, and then plays it back — even when you're on an airplane or in the
subway with no internet. It's like a DVR for music. You search for a song or a 2-hour mix, tap
download, and it's yours forever. Then it works just like Apple Music or Spotify — you can control
it from your lock screen, skip tracks with your headphone buttons, and build playlists.

### What are the tradeoffs in your design?

**JSON flat file vs. SQLite:**
- Pro: zero schema migrations, trivial to inspect and debug, perfectly fast for <1000 tracks.
- Con: every metadata update rewrites the entire array. At 10,000 tracks, this gets slow.
  A proper implementation would use Room (SQLite) with a proper schema.

**NewPipe Extractor vs. YouTube Data API:**
- Pro: no API key, no quota limits, no attribution requirements.
- Con: it reverse-engineers YouTube's internal web scraping, so it can break when YouTube changes
  its site structure. Updates to NewPipe are required. The official API would be stable.

**Local JSON metadata vs. embedded file tags (ID3):**
- Pro: decoupled from file format (works for M4A, WebM, OGG equally).
- Con: metadata is only valid within this app. If you copy files to a PC, the metadata is lost.

**Foreground Service for playback:**
- Pro: Android keeps it alive even when the app is backgrounded/killed. Required for music apps.
- Con: shows a persistent notification and drains slightly more battery than if the OS managed it.

**No user accounts / no backend:**
- Pro: zero server infrastructure, no privacy concerns, works offline permanently.
- Con: no cross-device sync, no backup if you reinstall, no social features.

---

## Your Contribution

### What did YOU specifically build?

Everything. This is a solo personal project. Specifically:

- The full **download pipeline** — stream URL resolution via NewPipe, resumable HTTP download with
  Range headers, retry logic with exponential-style backoff, progress tracking, metadata
  persistence.
- The **playback engine** — ExoPlayer setup, queue management, gapless mode vs. crossfade mode
  (two fundamentally different playback paths), MediaSession integration, notification controls.
- The **recommendation system** — signal-weighted scoring (favorites, play counts, search
  frequency) producing YouTube queries for personalized "Recommended for you" content.
- **Chapter splitting** — detecting YouTube chapter markers, creating virtual tracks with
  ClippingConfiguration, reference-counting file deletion.
- **Sleep timer with fade-out** — 50ms step-based linear volume fade.
- **Library management** — mood tagging, favorites, auto-delete-after-playback, shuffle exclusion,
  clean-up suggestions (never played, not played in 90+ days), storage accounting.
- **Playlist system** — ordered entries, cover images, import from Spotify (OAuth), import from
  YouTube playlist URL.
- **Equalizer** — 5-band EQ mapped to ExoPlayer's audio session, persisted per-band levels.
- **UI in Jetpack Compose** — all screens: Search, Library, Player, Home (recommendations),
  Analytics, Equalizer, Settings, Onboarding.

### What parts were most challenging?

**Resumable downloads** were the hardest to get right. YouTube CDN URLs expire while a large
file is downloading (a 2-hour mix can take 10+ minutes on mobile). My first implementation
just retried from byte 0 each time, which was wasteful and often just re-failed. The fix was
to re-fetch the stream URL on every retry (since the URL changes each time), then send a fresh
`Range: bytes=N-` against the new URL. This requires handling three distinct server responses:
- `206 Partial Content` — success, append data
- `200 OK` when you asked for a range — server ignored Range, can't append, retry from scratch
- `416 Range Not Satisfiable` — byte offset is past end of file, retry from scratch

Getting those three branches correct without corrupting the partial file took several iterations.

**Gapless vs. crossfade playback** was also complex. These are two fundamentally incompatible
approaches that have to coexist based on a user setting. When crossfade is 0, ExoPlayer's
`setMediaItems` handles pre-buffering the next track and transitioning seamlessly. When
crossfade is on, I manually manage a single-item ExoPlayer, driving volume fades on a `Handler`
and swapping tracks at the right moment. The tricky part is that the crossfade trigger
(`remaining <= crossfadeDurationMs`) fires while the track is still playing, so I needed a
`crossfadeStartedForCurrentTrack` guard to avoid triggering it twice.

### What decisions did you personally make?

1. **NewPipe over the YouTube API** — no quota, no key management, and I wanted to learn how
   YouTube stream extraction works internally.
2. **JSON file over SQLite** — simplest solution for the scale; can always migrate to Room later.
3. **Virtual chapter tracks over file splitting** — copying audio file segments is slow and wastes
   storage. Pointing multiple `DownloadedTrack` records at the same file with `startMs`/`endMs`
   is elegant and instant.
4. **`CompositionLocal` over Hilt** — Hilt adds significant complexity and boilerplate for a
   single-developer app with a handful of singletons.
5. **Gapless as the default** — most music apps don't implement true gapless. I made crossfade
   opt-in so the default experience is correct for albums and compilations.

---

## Challenges

### What was the hardest bug or issue?

The most painful bug was **"unexpected end of stream" during long downloads**. Large YouTube
audio files (1–2 hour mixes) would often fail partway through. The root cause: YouTube's CDN
serves these through temporary signed URLs that expire after ~15 minutes. My initial retry
logic reused the same expired URL, which immediately failed again. Worse, partial files on
disk were being deleted on failure, so every retry started from 0.

Fix: (1) re-fetch the stream URL on every retry attempt, (2) keep the partial file across
retries, (3) track bytes downloaded and use `Range: bytes=N-` on the new URL.

### What didn't work initially?

**The equalizer** failed silently on first attach. I was calling `Equalizer(0, sessionId)` with
session ID `0`, which is deprecated on modern Android devices. The constructor succeeds but the
equalizer silently does nothing. The fix was to listen for `ExoPlayer.onAudioSessionIdChanged()`
— which fires once ExoPlayer establishes a real audio session — and only then attach the
Equalizer with the real `audioSessionId`.

Also, the **playlist listener** for queue transitions was being added multiple times because
`setMediaItems` got called repeatedly (e.g., adding a track to the queue). This caused
`onMediaItemTransition` to fire multiple times per track change, resulting in double play-count
increments and double auto-delete triggers. Fix: a `playlistListenerAdded` guard flag.

### What did you have to redesign?

The **queue architecture**. Originally, each track was a standalone `setMediaItem()` call, and
"queue" was just a `mutableListOf` that I manually advanced on `STATE_ENDED`. This worked for
crossfade but produced audible gaps in non-crossfade mode. I redesigned it so the queue either
runs in **playlist mode** (`setMediaItems` with all tracks at once, ExoPlayer preloads/transitions)
or **single-item mode** (manual crossfade). The `useGaplessPlaylist()` function decides which path
to take based on whether crossfade is configured, and the skip/previous/reorder functions have
dual code paths for each mode.

---

## Scalability

### What would break if this had 10,000 users?

Currently the app has **no server-side component** — it's fully client-side. But if this were a
shared service:

1. **YouTube dependency would collapse.** NewPipe reverse-engineers YouTube. YouTube's bot
   detection would flag the traffic and block it. You'd need a proxy layer, rotating user agents,
   or to switch to the official YouTube Data API v3 (which has strict quotas: 10,000 units/day
   free, each search costs 100 units).

2. **Metadata JSON file** becomes a bottleneck at scale. It's O(n) to read and rewrites the entire
   file on every update. At 10K tracks it would block the IO thread noticeably.

3. **Audio file storage** is on the user's device — that scales naturally. But if you wanted a
   shared CDN of downloaded tracks (deduplication across users), you'd need a content-addressable
   store keyed by YouTube video ID + quality tier.

4. **Recommendation engine** is O(library × signals). Still fast even at 10K tracks per user
   since it's all in-memory. The bottleneck would be the YouTube search calls it triggers (5 serial
   or parallel HTTP requests).

### How would you scale this system?

If this were a multi-user backend service:

**Stream extraction layer:** Stateless microservice wrapping NewPipe (or YouTube API). Horizontally
scalable behind a load balancer. Cache stream URLs by video ID + quality in Redis with a TTL
of ~10 minutes (matching YouTube CDN expiry). Prevents redundant extraction for the same video.

**Audio storage:** Content-addressable blob storage (S3-equivalent) keyed by
`{videoId}_{qualityTier}.m4a`. Users who download the same track share the blob — zero duplication.
Track download completion per user in a separate `user_downloads` relation.

**Metadata:** PostgreSQL. Schema:
```
tracks(id, source_url, title, artist, duration_ms, file_key, ...)
user_library(user_id, track_id, downloaded_at, play_count, last_played, is_favorite, ...)
user_playlists(id, user_id, name, ...)
playlist_entries(playlist_id, track_id, position)
```

**Recommendation:** Precompute recommendations offline (batch job) rather than on-demand. Run
nightly per user, store top-N recommendations in a `user_recommendations` table.

**Download queue:** Message queue (Kafka or SQS). User submits download request → enqueued →
worker picks it up → streams from YouTube → uploads to blob storage → marks complete → pushes
notification to client.

### What bottlenecks exist?

1. **YouTube stream URL extraction** — it involves parsing YouTube's HTML/JavaScript. At scale,
   this is CPU-heavy and fragile. Each extraction is ~200–500ms.
2. **JSON metadata rewrite** — O(n) write, blocking IO. Should be a database write.
3. **Artwork loading for notifications** — done on a bare `Thread`, not a coroutine. Fine for
   now, but not controlled concurrency.
4. **Recommendation queries** — 5 YouTube searches run on every home screen load. Should be
   cached or pre-fetched.

---

## Improvement

### What would you improve if you had more time?

1. **Replace the JSON metadata store with Room (SQLite).** Schema migrations, indexed queries,
   partial updates — all of which the JSON file can't do efficiently.

2. **Background downloads with WorkManager.** Currently downloads die if the user force-closes
   the app. WorkManager would persist downloads across app restarts and handle network
   connectivity constraints natively.

3. **Proper test coverage.** There are zero unit or integration tests right now. The download
   pipeline and queue logic have enough edge cases (416, range restart, gapless vs. crossfade)
   to warrant property-based tests.

4. **Cache recommendation results.** Currently re-fetches 5 YouTube queries every time the home
   screen is opened. Should cache for at least 1 hour.

5. **Media3 MediaBrowser / MediaLibrarySession** instead of the manual `MediaSession` and service
   `Intent` protocol. The current approach uses string actions and `PendingIntent`s, which is
   verbose and error-prone. Media3's `MediaController` gives a proper typed API.

6. **Equalizer UI** — currently a 5-band EQ with manual sliders. Would add audio visualizer
   sync (the FFT data is already being captured), and presets (Pop, Rock, Classical).

### What would you redesign from scratch?

The **service ↔ UI communication protocol**. Right now, the UI sends `Intent`s with string action
constants to `MusicPlayerService`, and the service exposes `StateFlow`s via `MusicServiceConnection`
(which binds to the service). This works but it's two asymmetric channels with no type safety.

The proper redesign is **Media3 MediaController / MediaLibrarySession**. The controller gives you
a `suspend`-friendly, typed API — `controller.play()`, `controller.seekTo()`, `controller.addMediaItem()`
— instead of constructing Intents manually. The library session handles the `MediaBrowser` protocol
for Android Auto, Wear OS, and Bluetooth HFP automatically. I'd rewrite `MusicPlayerService` as a
`MediaLibraryService` and replace `MusicServiceConnection` with `MediaController`.

---

## Quick-Answer Specifics

### "How does it work?"

User searches YouTube by title. The app uses NewPipe Extractor (an open-source library that
reverse-engineers YouTube's web interface) to search and get results. When the user taps Download,
the app extracts a direct CDN audio URL from YouTube, then downloads the file over HTTP with
resume support. The file lands on local storage. Playback uses ExoPlayer — a Google audio engine
— inside an Android Foreground Service that keeps music playing even when the screen is off. It
exposes media controls to the lock screen and notification bar via the Android MediaSession API.

### "What's the hardest part?"

Two things: first, making downloads reliable for large files. YouTube's CDN URLs expire in ~15
minutes, but a 2-hour mix can take longer than that to download. I had to re-fetch the stream URL
on every retry and use HTTP Range requests to resume from where I left off — while correctly
handling three different server responses to a Range request.

Second, gapless playback. Most music apps just let there be a small gap between tracks. Achieving
true gapless (where ExoPlayer preloads the next track and transitions seamlessly) conflicts with
crossfade (where you manually fade volume and swap tracks). These two modes are fundamentally
incompatible in ExoPlayer's API, so I had to build two separate code paths that switch based on
user settings, and make sure every queue operation (skip, previous, add, reorder) handles both.

### "What would you improve?"

Immediate priority: replace the flat JSON metadata file with Room (SQLite). Every time a track's
play count updates or a position is saved, I rewrite the entire metadata file. At a few hundred
tracks that's fine; at a few thousand it starts to lag. A proper relational schema with indexed
queries would fix that with no other architectural changes.

Longer term: adopt Media3's `MediaLibraryService` and `MediaController` to replace the current
`Intent`-based service communication. It's typed, testable, and gives Android Auto / Wear OS
compatibility for free.

### "What challenges did you face?"

**YouTube as an adversarial dependency.** NewPipe reverse-engineers YouTube's internal API. YouTube
changes their site regularly, which can break the extractor silently. I've had to handle cases
where `audioStreams` comes back empty, where the URL format changes, and where certain video types
(livestreams, premieres) are simply unsupported.

**Android lifecycle complexity.** A Foreground Service, a bound Service connection, Compose
recomposition, and coroutines all interact. Getting state to survive screen rotation, process
death, and app backgrounding without leaks required careful use of `StateFlow`s on the service
side (which outlive the UI) rather than `LiveData` or Compose state (which doesn't).

**Chapter splitting edge cases.** A "chapter track" points to the same audio file as its parent.
When you delete a chapter track, you must not delete the file if other chapters still reference it.
I had to implement reference counting — check whether any remaining track has the same `filePath`
before deleting the file.

---

## Connecting to the Social Graph Role

While Shuckler is client-side, the problem space maps directly to backend/social graph concerns:

- **Graph data model:** Playlists are edges (user → track), ordered by position. Favorites are
  weighted edges. Play history is a time-series edge. This is a bipartite graph (users × tracks)
  with typed, attributed edges — exactly the kind of structure a social graph team maintains for
  follows, likes, comments.

- **Recommendation as graph traversal:** My engine scores artists based on what's in your local
  graph (library). A social graph recommendation system does the same thing — walk N hops from
  a user node, aggregate edge weights, surface high-signal nodes. The signal types differ
  (follows vs. play counts) but the scoring logic is identical.

- **Eventual consistency:** DownloadManager updates metadata and file state independently.
  A file can exist on disk before its metadata record is fully written. I handle this by
  filtering `loadMetadata()` to only return records where `File(filePath).exists()` — analogous
  to a distributed system tolerating a window of inconsistency between storage layers.

- **Cache invalidation:** YouTube stream URLs have a TTL (15 min). I cache nothing and re-fetch
  fresh URLs on every retry. At scale this is the tradeoff between freshness and latency that
  every CDN and graph query cache faces.
