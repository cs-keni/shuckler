# Current Task

Date: 2026-05-14

## Status

In progress: design completion work from `DESIGN.md`.

## Active Slice

Flow-first `DESIGN.md` completion is the active implementation slice:
- Library track artist names open an artist detail screen.
- Artist detail shows a blurred artwork hero, Songs section, Albums artwork shelf, Play All, Shuffle, and currently-playing highlighting.
- Codex restored context from `CLAUDE.md`, `DESIGN.md`, and shared docs.
- User confirmed the project builds successfully in Android Studio on 2026-05-13.
- Codex WSL now has OpenJDK 17 and `JAVA_HOME` configured. Local Gradle starts, but full WSL compile checks need a Linux Android SDK because the current Android SDK is the Windows install with `.exe` build-tools.
- Album metadata and Album Detail are now being added as the next `DESIGN.md` slice.
- Brand/icon cleanup is underway: user chose Direction A, keeping Shuckler and switching the mark to `catdoodle.png`.
- Home visual redesign pass started: warm background, snapshot stats, redesigned hero, and quieter section headers.
- Search visual redesign pass started: Base background, tokenized search field, horizontal recent/suggestion chips, section labels, and redesigned YouTube result cards.
- Visual-token cleanup continued: onboarding pager dots no longer use `CircleShape`, and remaining hardcoded black/white UI overlays in Home, Library, Player, crop cover, and Equalizer were migrated to warm design tokens.
- User feedback: the UI is improved but still feels too box/card-heavy, with awkward Home gaps. Planning shifted to a "flow-first" redesign direction before further implementation.
- `DESIGN.md` now documents the flow-first direction, and `flow-redesign-preview.html` shows a proposed Home composition.
- User approved the flow-first preview. Home implementation has started with the first composition slice: compact topbar, full-bleed continue-listening/search band, inline metrics, chip actions, and flat artwork shelves.
- Library implementation has started: track browsing is no longer hidden inside the storage/download disclosure, maintenance tools moved lower, and shelves/track rows were flattened to reduce the card-stack feel.
- Reported Library sheet gap was addressed by removing the forced minimum track-area height in sheet mode and allowing partial sheet expansion.
- Reported Library mid-page gap was addressed by removing the large "No playlists yet" empty state from the collection flow.
- Library chips now use flat tokenized pills instead of Material default filter chips.
- Android Studio compile error at `LibraryScreen.kt:1613` was fixed by changing the mood-tag dialog chip call to the new local `FilterChip(label: String, selected: Boolean, onClick: () -> Unit)` signature.
- Search idle recommendations now use a horizontal artwork shelf while preserving full result cards for searched results.
- Search idle suggestions/empty copy were flattened to reduce card/panel feel.
- Search now has a discovery starter surface so fresh/idle Search does not feel empty: topbar supporting copy plus starter query chips.
- Search recommendation section now only renders while loading or when there are actual recommendation results, avoiding a blank section after empty fetches.
- Search now fills space under non-empty recommendations with a compact "Keep exploring" chip row.
- Stats flow pass started: tokenized time chips, quieter personality panel, flatter big stats and achievement surfaces.
- Stats follow-up added a dedicated topbar and Top artists ranked bar section from `DESIGN.md`.
- Now Playing flow polish continued: queue sheet uses the warm Base canvas, flat tokenized rows, accent wash for the current track, album-accent progress/controls, and custom action chips instead of Material `FilterChip`.
- Settings dialog polish continued: warm tokenized dialog surface, DM section headers, tokenized segmented controls, and Text1/Text2/Text3 copy hierarchy.
- Search idle recommendation flicker fixed: "Recommended for you" now renders only when visible recommendation tiles are available, and empty background refreshes no longer clear an already-visible shelf.
- Downloads flow polish continued: waveform download cards now use the tighter 8dp card radius, token border, status labels for queued/done/failed states, and Library's storage/download disclosure uses warm token styling.
- Playlist Detail flow polish continued: the screen now uses the warm Base canvas, tokenized header/actions/dialogs, flattened playlist track rows, rectangular artwork, and an accent wash for the currently playing row instead of card containers.

## Next Best Work

Continue remaining `DESIGN.md` consistency passes on the less-polished surfaces: Artist Detail, Album Detail, Import dialog, Onboarding, Create/utility screens, Crop Cover, Equalizer, and any remaining default Material color usage. Android Studio build/run should cover Home, Library sheet/full tab, Search idle/results, active downloads, Stats, Playlist Detail, Now Playing, Queue, Lyrics, and Settings.
