# 07 — M7 Music Library Screen: Code Review

## Summary
- **milestone:** M7 — Music Library Screen
- **verdict:** pass with fixes
- **scope alignment:** drift detected
- **validation confidence:** medium

---

## Critical issues

### 1. Android 10+ Storage Access Model Violation
**[in-scope fix]**

`ModLibrary.kt` uses `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)` to read user-placed music files. This is incompatible with Android scoped storage (API 29+). The app targets SDK 36 with minSdk 28.

**Evidence:**
- `ModLibrary.kt`, lines 8–11: hardcoded path via deprecated API
- `build.gradle.kts`: targetSdk 36, minSdk 28
- `AndroidManifest.xml`: only declares `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"`
- `BlockDropApp.kt`, line 116: permission only requested for API ≤ 32

**Root cause:** On API 33+, `READ_EXTERNAL_STORAGE` grants no access to Downloads. Files placed by the user or other apps in Downloads are only accessible via:
- **MediaStore Downloads collection** (API 29+, recommended)
- **Storage Access Framework** (SAF, requires user folder grant)

Direct `File` access to `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` silently returns an unreadable directory on API 33+ — `canRead()` returns false, scan returns empty, music library appears empty to the user.

**Correct path (API 29+):** Use `MediaStore.Downloads` content URI with a `ContentResolver` query, filtering by MIME type or file extension. Example:
```kotlin
val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
val projection = arrayOf(MediaStore.Downloads._ID, MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.DATA)
val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
// query for each supported extension: *.mod, *.xm, *.s3m, *.it
```

**Impact:** Music library is always empty on Android 13+ (API 33+). Feature is non-functional for the majority of current Android devices.

**Recommended fix:** Replace `ModLibrary` file-scanning with a `ContentResolver`-based MediaStore query. No new permission is needed on API 29+ for reading shared Downloads. On API 28 (minSdk), fall back to the current `File` approach with `READ_EXTERNAL_STORAGE`.

---

### 2. Missing ViewModel Tests for Music Library Methods
**[test gap]**

No unit tests exist for any of the new `GameViewModel` methods introduced in M7.

**Missing coverage:**
- `openMusicLibrary()` — pause logic with `PauseReason.MusicLibrary`, library rescan, state update
- `closeMusicLibrary()` — state cleanup
- `selectTrack(track)` — track selection and `isMusicPlaying` update
- `pauseMusic()`, `resumeMusic()`, `stopMusic()` — control state transitions

**Impact:** Cannot verify that game pause, library rescan, and UI state updates are consistent. No protection against regressions.

---

### 3. Missing Test for `DefaultModMusicService.playTrack()`
**[test gap]**

The `playTrack()` method — which the spec explicitly requires to "load and play a specific track immediately" — has no direct test coverage. `TrackSelectionTest.kt` only covers `pickNext()` and `findNextPlayableTrack()`.

**Missing cases:** successful load + emit to `trackChanges`, load failure (returns null), playback state after play, interaction with `lastPlayedPath`.

---

### 4. Potential Race Condition in Music State Updates
**[unclear or unsupported]**

Rapid `selectTrack(A)` / `selectTrack(B)` sequences may produce inconsistent state: `playTrack()` emits to `trackChanges` asynchronously while `refreshMusicState()` is called synchronously immediately after. The `trackChanges` collector and the direct state update may interleave, leaving `currentTrack` and `trackDisplay` out of sync.

**Impact:** Flaky UI state (wrong track highlighted, wrong display string) during rapid interaction. Likely minor in practice but worth documenting as a known limitation.

---

## Important improvements

### 5. No Regression Test for TrackInfoOverlay Visibility
**[test gap]**

Spec exit criteria require no regression in `TrackInfoOverlay`. The overlay is correctly hidden when `showMusicLibrary = true` (`BlockDropApp.kt`), but no test verifies this. Add a Compose test asserting `TrackInfoOverlay` is not shown when the music library screen is open.

---

### 6. `resumeMusic()` Relies on Undocumented `ModPlayer.resume()` Behaviour
**[in-scope fix]**

`GameViewModel.resumeMusic()` calls `modMusicService.resume()` and then falls back to `playTrack(currentTrack)` if `isPlaying()` returns false. This works but relies on `ModPlayer.resume()` silently no-oping when the player is stopped. Document this contract in the `ModMusicService` interface, or rename the fallback path to make the intent explicit.

---

## Optional suggestions

- Extract the muted/null-aware track label logic in `MusicLibraryScreen` to a shared location if `TrackInfoOverlay` ever needs the same pattern.
- Upgrade `ModLibrary` log level from INFO to WARN when the directory exists but `canRead()` returns false (permission issue vs. first-run setup).
- Add comments in `ModLibrary` referencing the MediaStore migration path for API 33+.

---

## Scope or architecture proposals

### Storage abstraction layer
**[requires architecture or scope change]**

Introduce a `MusicStorageProvider` interface in front of `ModLibrary`:
```kotlin
interface MusicStorageProvider {
    fun getLibraryFiles(): List<File>  // or return URIs for MediaStore
}
```
This decouples `ModLibrary` from the Android storage API, makes it testable with fake implementations, and allows swapping `File`-based and MediaStore-based scanning without touching `ModLibrary` logic. Recommended as part of the MediaStore fix above.

### Instrumented UI tests for `MusicLibraryScreen`
**[requires architecture or scope change]**

Add `MusicLibraryScreenTest.kt` (Compose testing DSL) covering: back button closes screen, track row triggers playback, empty state renders correctly, current track is highlighted, pause/resume/stop buttons reflect `isMusicPlaying`.

---

## Validation observations

**Works correctly:**
- `MusicLibraryScreen` composable receives state and callbacks with no direct service coupling
- Slide-right `AnimatedContent` transition matches spec
- Back handler closes library without resuming game (game stays paused) — matches spec
- `openMusicLibrary()` pauses game with `PauseReason.MusicLibrary` when state is Running
- `refreshMusicState()` updates all four UI fields: `availableTracks`, `currentTrack`, `isMusicPlaying`, muted flag
- All required string resources present

**Not validated:**
- Music library non-functional on API 33+ (critical)
- ViewModel integration correctness (no tests)
- `playTrack()` contract under failure conditions (no tests)
- TrackInfoOverlay regression (no test)

---

## Action items

**Blocking — must fix before merge:**
1. Replace `Environment.getExternalStoragePublicDirectory()` with MediaStore API for API 29+; keep File fallback for API 28
2. Add unit tests for all new `GameViewModel` music library methods
3. Add unit test for `DefaultModMusicService.playTrack()` covering success and failure paths

**Should fix:**
4. Add Compose regression test for `TrackInfoOverlay` visibility with library open
5. Document or clarify `resumeMusic()` / `ModPlayer.resume()` stopped-state contract

**Nice to have:**
6. `MusicStorageProvider` abstraction (decouple from hardcoded path)
7. Instrumented `MusicLibraryScreenTest`
