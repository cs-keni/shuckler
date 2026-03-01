# Shuckler — Quality of Life & Polish Ideas

A brainstorm list of QOL features: aesthetic improvements, small UX tweaks, and nice-to-haves. Not all will be implemented; use as a backlog.

---

## Aesthetic & Visual

- **Blurred album art background** — Player screen: subtle blurred version of album art behind content (like Spotify/Apple Music).
- **Animated progress ring** — Circular progress around album art instead of (or in addition to) seek bar.
- **Skeleton loaders** — Shimmer placeholders for Search results and Library while loading (instead of spinner).
- **Haptic feedback** — Light vibration on play/pause, favorite toggle, and key actions.
- **Ripple on album art** — Subtle ripple or glow when track changes.
- **Gradient overlays** — More refined gradients (e.g. from album art) on cards and headers.
- **Typography hierarchy** — Clearer font weights/sizes for titles vs metadata.
- **Empty state illustrations** — Friendly illustrations when Library/Search/Playlists are empty.
- **Smooth tab indicator** — Animated underline or pill for selected tab.
- **Floating action button** — Optional FAB for “Play” or “Search” on Home.
- **Card shadows** — Subtle elevation on list items and cards.
- **Consistent corner radius** — Unified 12dp/16dp radius across the app.
- **Mini-player artwork animation** — Subtle scale or pulse when playing.
- **Notification artwork border** — Rounded corners or subtle border on media notification art.

---

## UX & Interaction

- **Swipe to delete** — Swipe Library/Playlist items to delete (with undo snackbar).
- **Long-press context menu** — More actions: Play next, Add to queue, Add to playlist, Share, Delete.
- **Pull-to-refresh** — Refresh Search results or Library.
- **Double-tap to seek** — Double-tap left/right of seek bar to skip ±10 seconds.
- **Shake to shuffle** — Optional: shake device to shuffle queue (accessibility consideration).
- **Gesture: swipe down to dismiss** — Full player: swipe down to collapse to mini-player.
- **Remember scroll position** — Restore scroll in Library/Search when returning.
- **Search history chips** — Tappable recent searches as chips above search bar.
- **Quick actions on lock screen** — More actions (e.g. favorite) from lock screen controls if supported.
- **Confirmation dialogs** — “Are you sure?” for Clear all, Delete playlist, etc.
- **Undo snackbar** — After delete: “Track removed” with “Undo” for a few seconds.
- **Loading state for Play** — Show spinner when fetching stream URL before playback starts.
- **Offline indicator** — Badge or icon on tracks that are downloaded vs stream-only.
- **Data usage hint** — When streaming: “Streaming uses data” tooltip or setting.

---

## Playback & Audio

- **Gapless playback** — Seamless transition between tracks (ExoPlayer supports; verify config).
- **Preload next track** — Buffer next track in queue while current plays.
- **Remember volume** — Persist volume level (if we add in-app volume control).
- **Last position** — Resume long tracks from last position (e.g. podcasts).
- **Replay gain** — Normalize volume across tracks (advanced).
- **Skip silence** — Option to skip silent parts (ExoPlayer has SilenceSkippingAudioProcessor).

---

## Library & Organization

- **Sort options** — Library: by date added, title, artist, play count, duration.
- **Group by artist** — Library: collapsible sections by artist.
- **Recently played section** — Dedicated “Recently played” in Library or Home.
- **Play count badge** — Show play count on Library items.
- **Batch select** — Multi-select tracks for batch delete, add to playlist, etc.
- **Drag to reorder** — Reorder playlist tracks by dragging.
- **Smart playlists** — Auto-playlists: “Most played,” “Recently added,” “Favorites.”
- **Folder/collection** — Group playlists into folders.

---

## Search & Discovery

- **Search suggestions** — Autocomplete from recent searches or trending.
- **Filter by duration** — Search results: filter short (< 4 min) vs long (> 10 min).
- **Filter by quality** — Show bitrate/format in search results.
- **“More like this”** — From a track: find similar (e.g. same channel, related).
- **Search within playlist** — Filter tracks inside a playlist.
- **Voice search** — Optional: “Hey Shuckler, play …” (Android voice actions).

---

## Settings & Preferences

- **Default tab** — Choose which tab opens on app launch (Home, Search, Library).
- **Stream vs download default** — Prefer “Play” (stream) or “Download” as primary action.
- **Wi‑Fi only downloads** — Option to only download when on Wi‑Fi.
- **Cache stream buffer** — How much to buffer when streaming (e.g. 30 s, 60 s).
- **Notification style** — Compact vs expanded by default.
- **Hide explicit content** — Filter search results (if metadata available).
- **Backup/restore** — Export playlists and metadata; restore on new device.

---

## Notifications & Widgets

- **Widget: compact vs full** — Different widget sizes (2x2, 4x2) with more/less info.
- **Widget: album art only** — Minimal widget: just artwork, tap to open.
- **Notification: lyrics preview** — Show current lyric line in notification (if space).
- **Notification: progress bar** — Seekable progress in notification.
- **Android Auto** — Full support for Android Auto (metadata, controls).

---

## Performance & Technical

- **Image caching** — Coil disk cache for thumbnails (may already be default).
- **Lazy load Library** — Paginate or virtualize if Library is huge.
- **Reduce recomposition** — Optimize Compose recomposition in long lists.
- **Background prefetch** — Prefetch stream URL for next track in queue.
- **Connection retry** — Auto-retry on network failure with exponential backoff.

---

## Accessibility

- **Content descriptions** — All icons and images have proper contentDescription.
- **TalkBack support** — Full screen reader support.
- **Font scaling** — Respect system font size.
- **High contrast** — Optional high-contrast theme.
- **Reduced motion** — Respect “Reduce motion” system setting.

---

## Social & Sharing

- **Share track** — Share link to YouTube video or track info.
- **Share playlist** — Export playlist as text/list.
- **Import/export** — Backup playlists to file; import from file.

---

## Stretch / Experimental

- **Carousel for playlists** — Horizontal scrolling playlist cards on Home.
- **Now playing history** — “What was playing” when you left the app.
- **Sleep timer: custom duration** — Input custom minutes (e.g. 23 min).
- **Crossfade: per-track** — Different crossfade per track (complex).
- **Visualizer** — Audio waveform or spectrum visualizer in Player.
- **Lock screen artwork** — Full-screen artwork on lock screen (device-dependent).
- **Chromecast / Cast** — Cast to TV/speaker (advanced).

---

## Priority Suggestions

If picking a few to start with:

1. **Stream without download** ✅ — Play full track from YouTube without saving (Phase 31).
2. **Delete + undo** ✅ — Delete with Undo snackbar (5s to undo) in Library.
3. **Sort options for Library** ✅ — Date added, Title, Artist, Duration, Play count.
4. **Skeleton loaders** ✅ — Placeholder cards in Search while loading.
5. **Offline indicator** — Partially done: "Downloaded" shown in Search when already saved.
6. **Spotify import** — Phase 28; saves manual re-creation of playlists.
