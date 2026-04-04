# 16 — playTrack() blocks main thread; load fails silently; no audio

## Reported symptom

After fix-request 15 was applied (removing the `!started` guard from `playTrack()`), clicking a track row does nothing — the track does not appear in "current" and no audio plays.

## What changed after fix 15

Before fix 15, `playTrack()` returned early after setting `currentTrack = track`. The track appeared in "current" but nothing played.

After fix 15, `playTrack()` now reaches `player.stop()` → `player.load()` → `player.play()`. If `player.load()` returns null, `playTrack()` sets `currentTrack = null` and returns — the UI doesn't change and no audio plays. This is "click does nothing".

## Root cause A — blocking I/O on the main thread

`selectTrack()` in `GameViewModel` is called from the Compose onClick on the main thread. The entire chain runs synchronously on that thread:

```
onClick (main thread)
  → GameViewModel.selectTrack(track)           // main thread
      → modMusicService.playTrack(track)        // main thread
          → player.stop() → thread.join()       // BLOCKS main thread
          → player.load(track)
              → readTrackBytes(track)
                  → contentResolver.openInputStream(safUri).readBytes()
                                                // BLOCKING I/O on main thread
              → nativeOpen(bytes, size)         // JNI call on main thread
```

`readTrackBytes()` reads the entire mod file from a SAF content URI synchronously on the main thread. Mod files range from tens of KB to several MB. On Android 15 with strict mode or audio routing priority, a long block on the main thread may cause the system to defer or drop the call. Even without a strict mode violation, blocking the main thread suppresses the UI while the file is being read — which manifests as a frozen screen and no perceived response.

More critically: on Android 15, `ContentResolver.openInputStream()` for a SAF document URI can throw `SecurityException` if the calling thread's context doesn't have the URI permission in scope at that moment. This is caught by:

```kotlin
val bytes = try {
    readTrackBytes(track)
} catch (e: Exception) {
    Log.w(TAG, "Cannot read file: ${track.pathOrUri}", e)
    return null  // ← silently returns null
}
```

The exception is caught, null is returned, `currentTrack = null` is set, and the click appears to do nothing. The error IS logged with tag `ModPlayer` and message `"Cannot read file: ..."` but only in logcat — there is no user-visible feedback.

## Root cause B — `player.load()` fails silently with no user feedback

Whether the failure is a permission exception, a JNI error (`libopenmpt_jni not available`), or libopenmpt rejecting the file bytes (`nativeOpen` returns 0), all three failure paths in `load()` return null silently. `playTrack()` then sets `currentTrack = null`. The UI state is unchanged or the previous current track disappears. There is no error shown in the music library UI.

The three silent failure paths:
- `Log.w(TAG, "Cannot read file: ...")` — SAF URI unreadable
- `Log.e(TAG, "libopenmpt_jni not available")` — JNI library not loaded
- `Log.w(TAG, "Failed to open module: ...")` — libopenmpt rejected the bytes

## Diagnosis step

Filter logcat by tag `ModPlayer` immediately after tapping a track. One of the three messages above will identify the exact failure. If none appear, the click is not reaching `selectTrack()` at all (unlikely given the wiring is correct at `BlockDropApp.kt:175`).

## Fix

### 1. Move `player.load()` off the main thread

`GameViewModel.selectTrack()` must launch a coroutine for the blocking work:

```kotlin
fun selectTrack(track: ModTrackInfo) {
    viewModelScope.launch(Dispatchers.IO) {
        modMusicService.playTrack(track)
        withContext(Dispatchers.Main) {
            refreshMusicState()
        }
    }
}
```

`player.stop()` (which calls `thread.join()`) and `readTrackBytes()` (file I/O) and `nativeOpen()` (JNI) must all run on `Dispatchers.IO`, not the main thread.

`player.play()` creates an `AudioTrack` which can be created on any thread in stream mode. The playback thread is started inside `play()` itself.

### 2. Surface load failures in the music library UI

When `player.load()` fails, update the UI model with an error state so the user knows why the track didn't play. Minimally: add a `trackLoadError: String?` field to `GameUiModel` and show a snackbar or inline message in `MusicLibraryScreen` when it is non-null.

Without user-visible feedback, "click does nothing" is indistinguishable from a bug in the click handler.

## Impact

Blocking — explicit track selection from the music library is non-functional on Android 15 after fix 15 is applied. The feature remains broken.
