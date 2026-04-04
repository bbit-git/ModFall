# 12 — Fix-request 11 review: SAF folder picker (post-fix)

## Summary
- **milestone:** Fix-request 11 — SAF folder picker for API 33+
- **verdict:** pass with fixes
- **scope alignment:** in scope
- **validation confidence:** high

---

## Issue resolution status

| # | Requirement | Status | Evidence |
|---|-------------|--------|----------|
| 1 | SAF OpenDocumentTree launcher | **RESOLVED** | `BlockDropApp.kt:120–131` uses `ActivityResultContracts.OpenDocumentTree()` via `rememberLauncherForActivityResult` |
| 2 | Persistable URI permission | **RESOLVED** | `BlockDropApp.kt:124–127` calls `takePersistableUriPermission()` with `FLAG_GRANT_READ_URI_PERMISSION` before storing URI |
| 3 | URI persisted in SettingsRepository | **RESOLVED** | `SettingsRepository.kt:24–26, 40–48` stores/retrieves URI as Flow; `GameViewModel.kt:71–76` subscribes to changes |
| 4 | `ModLibrary.scan()` branching | **RESOLVED** | `ModLibrary.kt:20–27` branches: SAF URI first → API ≤ 32 filesystem → API 33+ empty |
| 5 | DocumentFile listing + contentResolver reading | **PARTIAL** | DocumentFile used correctly; `listFiles()` iterated without null check (`ModLibrary.kt:147–148`); `ModPlayer.kt:215–226` handles `content://` URIs correctly |
| 6 | UI — Set music folder button | **RESOLVED** | Visible in top bar (`MusicLibraryScreen.kt:91–97`) and in empty state (`MusicLibraryScreen.kt:119`) |
| 7 | AndroidManifest.xml correct | **RESOLVED** | Only `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"`; no `MANAGE_EXTERNAL_STORAGE` or other additions |
| 8 | Test coverage | **NOT RESOLVED** | Zero tests for SAF scan path, URI persistence, or `listFiles()` null case |

---

## Critical issues

### 1. NullPointerException in `scanSaf()` — `listFiles()` not null-checked
**[in-scope fix]**

`DocumentFile.listFiles()` can return `null` (e.g. permission revoked, stale URI, filesystem error). The code iterates the result directly:

```kotlin
// ModLibrary.kt lines 147-148
val files = root.listFiles()
for (file in files) {  // ← crash if null
```

The file-based scan (`scanFileSystem()`) correctly guards this with:
```kotlin
val files = downloadsModsDir.listFiles() ?: run {
    Log.w(TAG, "Could not list files in: ...")
    return@buildList
}
```

Apply the same pattern to `scanSaf()`.

- **Impact:** Any stale URI, revoked permission, or filesystem fault crashes the app during library scan.

---

### 2. Zero-length files not filtered in `scanSaf()`
**[in-scope fix]**

`scanFileSystem()` filters zero-length files (`ModLibrary.kt:54`):
```kotlin
if (file.length() == 0L) continue
```

`scanSaf()` has no equivalent check. An empty `.mod` file placed in the SAF folder will appear in the track list and then fail silently when `ModPlayer` tries to load it via libopenmpt.

- **Impact:** Inconsistent behaviour between scan paths; silent playback failure for empty files on API 33+.
- **Fix:** Add `if (child.length() == 0L) continue` (or `DocumentFile.length() == 0L`) to the `scanSaf()` loop.

---

## Important improvements

### 3. No tests for the SAF scan path
**[test gap]**

`ModLibraryTest.kt` has 11 tests covering `scanFileSystem()`. Zero tests cover `scanSaf()`. The SAF path is the primary fix for API 33+ — the most commonly affected API level — yet it is completely untested.

Minimum cases needed in a new `ModLibrarySafTest`:
- `scanSaf returns empty list when listFiles() returns null`
- `scanSaf filters zero-length files` (once fix #2 is applied)
- `scanSaf filters unsupported extensions`
- `scanSaf returns tracks for valid files`

URI persistence in `SettingsRepository` and ViewModel subscription to URI changes also lack tests.

---

### 4. SAF picker launched without initial URI
**[in-scope fix]**

`BlockDropApp.kt:121`: `OpenDocumentTree()` is launched with no initial URI. The fix-request spec (issue 11) recommended guiding the user to Downloads. Without an `initialUri`, the system picker opens at an arbitrary location and the user must navigate to `Download/Mods/` manually every time they re-select the folder.

- **Fix:** Pass an initial URI hint:
  ```kotlin
  musicFolderLauncher.launch(
      Uri.fromFile(
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
      )
  )
  ```
  This is a hint only — the system may ignore it, but it improves UX on most devices.

---

## What is correct

- **Branching order** is correct: SAF → File (API ≤ 32) → empty (API 33+ no URI)
- **`FLAG_GRANT_READ_URI_PERMISSION`** used (not WRITE) — correct
- **`takePersistableUriPermission()`** called before storing the URI — correct order
- **`ModPlayer.kt:215–226`** correctly distinguishes `content://` from `file://` URI schemes; `openInputStream()` properly used for content URIs
- **Manifest** has no unnecessary permissions; SAF requires no permission declarations — correctly avoided `MANAGE_EXTERNAL_STORAGE`
- **Context** passed to `DocumentFile.fromTreeUri()` is `applicationContext` — correct; Activity context not required
- **User cancels picker** → URI not updated → correct
- **Flow-based reactivity** for URI changes is correct; no race conditions detected
- **Empty-state messaging** correctly distinguishes API 33+ (no SAF URI set) from other states

---

## Optional suggestions

- Log a WARN (not INFO) in `scanSaf()` when `listFiles()` returns null, to aid debugging on devices with intermittent permission revocation.
- Show the chosen folder name/path in the music library header after selection so the user knows which folder is active.

---

## Scope or architecture proposals

None. All changes are within the scope of fix-request 11.

---

## Validation observations

- Thread safety: URI reads/writes are Flow-based with `viewModelScope`; no races detected
- Memory: `contentResolver.openInputStream()` wrapped in `.use {}` in `ModPlayer` — no leaks
- Backward compatibility: File-based scan for API ≤ 32 is untouched; no regressions observed
- Stale URI handling: `DocumentFile.fromTreeUri()` returns null on stale URI — but `scanSaf()` does not guard `listFiles()` on a null root (issue #1)

**Blocking before merge:** issues #1 (crash) and #2 (empty file inconsistency).  
**Should fix:** issue #3 (SAF test coverage) and #4 (initial URI hint).
