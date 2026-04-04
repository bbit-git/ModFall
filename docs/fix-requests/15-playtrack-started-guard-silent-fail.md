# 15 — playTrack() silently ignores explicit selection when game not started

## Reported symptom

Selected track appears in the "current" row of the music library but no audio plays.
Logcat shows `FrameInsert open fail: No such file or directory` (see note below).

## Root cause

`DefaultModMusicService.playTrack()` sets `currentTrack` **before** checking the `started` flag:

```kotlin
// DefaultModMusicService.kt lines 81-96
override fun playTrack(track: ModTrackInfo) {
    lastPlayedPath = track.pathOrUri
    currentTrack = track            // ← written unconditionally
    if (!enabled || !started) return // ← silently returns if started == false
    player.stop()
    ...
    player.play()
}
```

`started` is only set to `true` by `start()`, which is called exclusively from `GameViewModel.startGame()`. It is `false` when:
- The user has never started a game (idle screen)
- The user quit the last game (`quitGame()` → `modMusicService.stop()` → `started = false`)

In both cases, opening the music library and tapping a track:
1. Writes `track` to `currentTrack` → `currentTrackInfo()` returns it → UI shows it in "current"
2. Hits `!started == true` → returns immediately
3. `player.load()` and `player.play()` are never called → silence

`openMusicLibrary()` in `GameViewModel` does **not** call `modMusicService.start()`, so the service remains in its un-started state throughout the library interaction.

**Evidence:**
- `DefaultModMusicService.kt:83` — `currentTrack = track` (unconditional)
- `DefaultModMusicService.kt:84` — `if (!enabled || !started) return`
- `GameViewModel.kt:162` — `modMusicService.start()` called only inside `startGame()`
- `GameViewModel.kt:191` — `openMusicLibrary()` does not call `start()`

## Note on "FrameInsert open fail: No such file or directory"

This logcat message is emitted by Android's `AudioFlinger` effects subsystem when a new audio session is created and a system audio effect (e.g. spatialiser, frame inserter) fails to find its device file. It is a **system-level warning unrelated to this bug** and does not cause audio silence. It will appear whenever any AudioTrack is created on affected Android 15 devices, regardless of whether playback succeeds.

## Fix

Remove `!started` from the `playTrack()` guard. `playTrack()` is an **explicit user action** — the caller has consciously chosen a track. It should only be blocked by mute (`!enabled`), not by whether a game session has been started.

```kotlin
// Before
if (!enabled || !started) return

// After
if (!enabled) return
```

The `started` flag still correctly gates `playNextTrack()` (automatic random selection, should not trigger without an active game) and `resume()`. It is not needed in `playTrack()` because:
- `start()` already sets `started = true` before calling `playTrack()` internally
- `setEnabled()` already guards with `started` before calling `playTrack()` (line 104)
- Explicit user selection from the music library should work in all app states

## Impact

Blocking — the core music library feature (select and preview a track) is non-functional whenever the user opens the library from the idle screen or after quitting a game.
