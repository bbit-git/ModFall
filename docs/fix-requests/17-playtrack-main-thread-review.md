# 17 — Fix-request 16 review: playTrack() main thread + silent errors (post-fix)

## Summary
- **milestone:** Fix-request 16 — playTrack() main thread blocking + silent errors
- **verdict:** pass with fixes
- **scope alignment:** in scope
- **validation confidence:** high

---

## Prior issue resolution status

| # | Issue | Status | Evidence |
|---|-------|--------|----------|
| A | `selectTrack()` moved to IO dispatcher | **RESOLVED** | `GameViewModel.kt:217` — `viewModelScope.launch(Dispatchers.IO)`, blocking work off main thread, `withContext(Dispatchers.Main)` for UI update |
| B | User-visible error feedback | **RESOLVED** | `GameUiModel.kt:51` — `trackLoadError: String?`; `GameViewModel.kt:220–226` — populated on load failure; `MusicLibraryScreen.kt:110–115` — displayed as error text |

---

## Critical issues

None.

---

## What is correct

**Fix A — IO dispatcher (`GameViewModel.kt:217–228`):**
- `viewModelScope.launch(Dispatchers.IO)` — correct scope and dispatcher
- `modMusicService.playTrack(track)` runs off main thread — `thread.join()`, file I/O, JNI all on IO ✓
- `player.play()` creates `AudioTrack` on IO thread — valid in stream mode ✓
- `withContext(Dispatchers.Main) { refreshMusicState(...) }` — UI update correctly back on main ✓
- Not `GlobalScope` — properly cancelled if ViewModel is cleared ✓

**Fix B — error state:**
- `trackLoadError` set to `track.fileName` when `currentTrackInfo()?.pathOrUri != track.pathOrUri` after `playTrack()` — correct failure detection ✓
- Set to `null` on success — previous error correctly cleared ✓
- `MusicLibraryScreen.kt:110–115` — displayed as light-red text, only when non-blank ✓
- Localized: `R.string.music_library_track_load_error` ✓
- Auto-cleared on next `refreshMusicState()` with default null ✓

**Thread safety:**
- Multiple rapid `selectTrack()` calls — last result wins via atomic `StateFlow.update` ✓
- No unprotected shared mutable state ✓
- `ModTrackInfo` is immutable ✓

---

## Important improvements

### 1. `stopMusic()` still blocks the main thread
**[in-scope fix]**

`stopMusic()` in `GameViewModel.kt:244–246` calls `modMusicService.stopPlayback()` → `player.stop()` → `thread.join()` synchronously on the main thread. This is the same blocking pattern that fix-request 16 addressed for `selectTrack()`.

`pauseMusic()` and `resumeMusic()` are safe — `pause()` and `resume()` in `ModPlayer` only set atomics and call `audioTrack?.pause()/play()` without joining threads.

`stopMusic()` should mirror the `selectTrack()` fix:
```kotlin
fun stopMusic() {
    viewModelScope.launch(Dispatchers.IO) {
        modMusicService.stopPlayback()
        withContext(Dispatchers.Main) { refreshMusicState() }
    }
}
```

### 2. `resumeMusic()` may call `playTrack()` on main thread
**[in-scope fix]**

`GameViewModel.kt:236–241`: if `modMusicService.resume()` fails to start playback, `resumeMusic()` falls back to `modMusicService.currentTrackInfo()?.let(modMusicService::playTrack)` on the main thread. `playTrack()` does blocking I/O — same issue as fix-request 16.

This fallback should also run on `Dispatchers.IO`.

---

## Optional suggestions

- Consider tracking the in-flight `selectTrack` job (`private var selectTrackJob: Job?`) and cancelling it on a new selection, to avoid stale load completions overwriting a more recent selection. Current "last state wins" via `StateFlow` is functionally correct but wastes IO resources loading a track that will be discarded.
- A brief loading indicator (`isTrackLoading: Boolean` in `GameUiModel`) while the IO coroutine is running would improve UX for large mod files.

---

## Scope or architecture proposals

None. All changes are within scope.

---

## Validation observations

- Coroutine scope is correct (`viewModelScope`, not `GlobalScope`) — no leak risk
- Error state clears correctly on both success and subsequent operations
- `pauseMusic()` does not block — safe as-is
- `stopMusic()` and `resumeMusic()` fallback path are the two remaining main-thread blocking calls in the music control surface

**Blocking before merge:** issues #1 (`stopMusic()`) and #2 (`resumeMusic()` fallback) are the same class of bug as fix-request 16 and should be fixed in the same pass.
