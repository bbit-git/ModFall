# M7 — Music Library Screen

**Status:** Planned
**Depends on:** M6

## Scope

A dedicated full-screen music library browser that lets the player pick, pause, or stop the currently playing MOD track. Accessed via long-press on the sound icon in the top bar.

### Entry / exit flow
- Long-press on the sound icon in the top bar
- If game is `Running`, pause it with `PauseReason.MusicLibrary` before opening the screen
- Screen slides in from the right (standard push transition over the main game view)
- Back icon in the library top bar closes the screen; game stays paused — user resumes from the pause menu

### Screen layout

```
┌─────────────────────────────────┐
│ ←   Music Library               │  top bar — back icon
│                                 │
│  ♫ Current Track Name  ⏸ ⏹ ▶  │  fixed: current track + playback controls
│  ──────────────────────────────  │  HorizontalDivider
│  ♫ Track A                 ▶   │  ↑
│  ♫ Track B                 ▶   │  scrollable list
│  ♫ Track C                 ▶   │  ↓
│                                 │
│  [empty state]                  │
│  "No music files found"         │
│  "Place .mod / .xm / .s3m /    │
│   .it files in Downloads/Mods/" │
└─────────────────────────────────┘
```

#### Current-track row (fixed, above divider)
- Shows `ModTrackInfo.displayString()` of the currently playing track
- Three action icons on the right:
  - ⏸ / ▶  — pause/resume toggle (icon reflects current playback state)
  - ⏹ — stop playback
  - ▶  — restart / play (visible when stopped or when track is paused from stop)
- If music is muted or no track is loaded, row shows a placeholder label

#### Track list (scrollable, below divider)
- One row per track in `ModLibrary.tracks()`
- Each row: track display string on the left, ▶ play icon on the right
- Tapping ▶ on any row immediately starts that track without closing the screen
- Currently playing track row is visually highlighted

#### Empty state
- Shown in place of the list when no tracks are available
- Message + path hint (`Downloads/Mods/`) shown at the bottom of the content area

### Architecture

#### `ModMusicService` interface additions
- `tracks(): List<ModTrackInfo>` — return scanned library tracks
- `playTrack(track: ModTrackInfo)` — load and play a specific track immediately
- `isPlaying(): Boolean` — whether the player is actively playing (not paused / stopped)

#### `DefaultModMusicService` additions
- Implement `tracks()` delegating to `ModLibrary.tracks()`
- Implement `playTrack()` — stop current, load specified track, emit to `trackChanges` flow, play
- Implement `isPlaying()` delegating to `ModPlayer`

#### `PauseReason` addition
- Add `MusicLibrary` variant so the library open/close path is distinguishable from user-initiated pause

#### `GameUiModel` additions
- `showMusicLibrary: Boolean = false`
- `availableTracks: List<ModTrackInfo> = emptyList()`
- `currentTrack: ModTrackInfo? = null`
- `isMusicPlaying: Boolean = false`

#### `GameViewModel` additions
- `openMusicLibrary()` — pauses game if running (`PauseReason.MusicLibrary`), rescans library, sets `showMusicLibrary = true`, refreshes `availableTracks` / `currentTrack` / `isMusicPlaying`
- `closeMusicLibrary()` — sets `showMusicLibrary = false`
- `selectTrack(track: ModTrackInfo)` — calls `modMusicService.playTrack(track)`, updates `currentTrack` / `isMusicPlaying`
- `pauseMusic()` / `resumeMusic()` — delegate to `modMusicService`, update `isMusicPlaying`
- `stopMusic()` — delegate to `modMusicService.stop()`, update `isMusicPlaying` and `currentTrack`
- Track-change collector already in `init` — extend it to also refresh `currentTrack` / `isMusicPlaying`

#### New file: `MusicLibraryScreen.kt`
- `MusicLibraryScreen` composable — self-contained, receives state and callbacks
- `MusicTrackRow` — reusable row composable for list items

#### `BlockDropApp.kt` changes
- `CompactChromeButton` — add `onLongClick` parameter (use `combinedClickable` or `pointerInput` with `detectTapGestures(onLongPress = …)`)
- Thread `onOpenMusicLibrary`, `onCloseMusicLibrary`, `onSelectTrack`, `onPauseMusic`, `onResumeMusic`, `onStopMusic` through `BlockDropScreen` and `TopBar`
- Wrap root content in `AnimatedContent` keyed on `showMusicLibrary` with a horizontal slide transition (slide in from right on open, slide out to right on close)

## Exit criteria

- Long-pressing the sound icon opens the music library screen with a slide-right transition
- If the game was running it is paused before the screen opens
- The current track (if any) is shown at the top with working pause / resume / stop controls
- Tapping ▶ on a list track plays it immediately without closing the screen
- The currently playing track is visually distinct in the list
- Back icon closes the screen; game remains paused
- Empty state displays the correct message and path hint when `Downloads/Mods/` contains no valid module files
- Muted state is respected — service controls remain consistent with mute flag
- No regression in existing mute toggle (short-press), game lifecycle pause/resume, or `TrackInfoOverlay`
