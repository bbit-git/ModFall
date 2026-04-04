# 08 — M7 Music Library Screen: Code Review (Round 2, post-fix)

## Summary
- **milestone:** M7 — Music Library Screen
- **verdict:** pass with fixes
- **scope alignment:** in scope
- **validation confidence:** high

---

## Prior blocking issues — resolution status

| # | Issue | Status |
|---|-------|--------|
| 1 | Storage: MediaStore API 29+ / File fallback API 28 | **RESOLVED** |
| 2 | ViewModel tests for all music library methods | **RESOLVED** |
| 3 | `DefaultModMusicService.playTrack()` unit test | **RESOLVED** |
| 4 | TrackInfoOverlay regression test | **NOT RESOLVED** |
| 5 | `resumeMusic()` / stopped-state contract documented | **RESOLVED** |

---

## Critical issues

### 1. TrackInfoOverlay regression test still missing
**[test gap]**

`BlockDropApp.kt` lines 174–183 correctly hide `TrackInfoOverlay` when `showMusicLibrary = true`, but no test verifies this. `BlockDropScreenTest.kt` covers composite rendering but not this specific visibility condition.

- **Impact:** Spec exit criteria require no regression in `TrackInfoOverlay`. The code is correct; the gap is test coverage only.
- **Fix:** Add one Compose test asserting `TrackInfoOverlay` is not present in the composition when `showMusicLibrary = true`.

---

## What is correct

**Storage fix (RESOLVED):**
- `ModLibrary.kt`: dual-path — `scanMediaStore()` (API 29+) via `MediaStore.Downloads` content URIs; File-based fallback for API 28. No permission needed on API 33+.
- `ModPlayer.kt`: `readTrackBytes()` handles both content URIs and file paths based on URI scheme.
- `AndroidManifest.xml`: `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` is correct.
- `BlockDropApp.kt`: permission requested only for API ≤ 32.

**ViewModel tests (RESOLVED):**
- `GameViewModelMusicLibraryTest.kt` covers `openMusicLibrary()`, `closeMusicLibrary()`, `selectTrack()`, `pauseMusic()`, `resumeMusic()`, `stopMusic()`, pause logic with `PauseReason.MusicLibrary`, and all UI state fields.

**`playTrack()` tests (RESOLVED):**
- `DefaultModMusicServiceTest.kt`: success path (load + emit + playback state) and failure path (load returns null → no emit, no play).

**`resumeMusic()` contract (RESOLVED):**
- `ModMusicService.kt`: interface documents that callers must reissue `playTrack()` after stop rather than expecting resume to work.
- `GameViewModel.kt`: fallback in `resumeMusic()` matches the documented contract.

**Screen layout and behaviour:**
- Current-track row: pause/resume toggle and stop reflect `isMusicPlaying` correctly; controls hidden when muted.
- Track list: `MusicTrackRow` highlights current track by `pathOrUri` comparison.
- Slide-right `AnimatedContent` transition matches spec exactly.
- Back handler closes library without auto-resuming game.
- Empty state renders correct message and path hint.
- `trackChanges` collector in `GameViewModel.init` updates `trackDisplay` and music state.

---

## Optional suggestions

- A `MusicStorageProvider` interface would further decouple `ModLibrary` from Android storage APIs — not needed for M7 but worthwhile if storage access needs to change again.
- `ModLibrary.kt` line 36: WARN level for unreadable directory is appropriate; a comment linking to the MediaStore migration path would help future maintainers.

---

## Scope or architecture proposals

None. Implementation is within M7 scope.

---

## Validation observations

All exit criteria from the spec are met at runtime. The single remaining gap is the `TrackInfoOverlay` regression test — code is correct, coverage is not. Recommend merging with that test added in the same or an immediate follow-up commit.
