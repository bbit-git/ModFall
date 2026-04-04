# 09 — Music library: files not found + no reload gesture

## Reported symptoms

1. No tracks appear in the music library despite a valid `.mod` file placed at `/storage/emulated/0/Download/Mods/`.
2. There is no gesture or button to reload the library after adding files.

---

## Root causes

### Issue A — MediaStore does not index newly-placed files immediately

On API 29+ (`Build.VERSION_CODES.Q`) `ModLibrary.scanMediaStore()` is used exclusively. MediaStore's `Downloads` collection is **passively maintained by the OS media scanner**, which runs on an indeterminate schedule. When the user manually copies a `.mod` file into `Download/Mods/` (e.g. via a file manager or ADB), the file may not appear in MediaStore for seconds, minutes, or longer — or never, if the OS considers the format unindexable.

There is no call to `MediaScannerConnection.scanFile()` anywhere in the codebase before querying MediaStore. As a result, `scanMediaStore()` silently returns an empty list even though the files are physically present.

**Evidence:** `ModLibrary.kt` lines 68–130: `scanMediaStore()` queries `MediaStore.Downloads` but never triggers an explicit media scan. `DefaultModMusicService.rescanLibrary()` calls `library.scan()` directly; no scan broadcast or connection is issued beforehand.

**Fix direction:** Before querying MediaStore, call `MediaScannerConnection.scanFile()` for the target directory (or known file paths), then re-query. Alternatively — and more reliably for tracker module files which have no standard MIME type and may be skipped by the OS scanner — use direct `File`-based scanning on API 29–32 (where `READ_EXTERNAL_STORAGE` still works) and reserve MediaStore for API 33+ only.

---

### Issue B — `.mod`/`.xm`/`.s3m`/`.it` files may not be indexed by MediaStore at all

`MediaStore.Downloads` indexes files based on MIME type detection. Tracker module formats (`.mod`, `.xm`, `.s3m`, `.it`) have no registered MIME type on Android. The OS scanner may classify them as `application/octet-stream` or skip them entirely. If the OS does not index them, they will never appear in a `MediaStore.Downloads` query regardless of when the scan runs.

**Evidence:** No MIME type filtering exists in `scanMediaStore()` (`ModLibrary.kt` lines 68–130); the query relies on `DISPLAY_NAME LIKE '%.mod'` etc., but if the OS never inserts the rows, the query returns zero results with no error or log warning.

**Fix direction:** Prefer direct `File` scanning for API 29–32 (READ_EXTERNAL_STORAGE covers it). For API 33+, either use `MediaScannerConnection` with explicit MIME type hints (`audio/mod`), or request `MANAGE_MEDIA` permission and trigger a full rescan, or document the limitation.

---

### Issue C — Empty-state hint shows wrong path (`Downloads/Mods/` vs `Download/Mods/`)

`strings.xml` line 51: `"Place .mod / .xm / .s3m / .it files in Downloads/Mods/"` — note the plural **Downloads**.

`Environment.DIRECTORY_DOWNLOADS` is `"Download"` (singular). The user confirmed their files are at `/storage/emulated/0/Download/Mods`. The hint directs users to the wrong folder name, causing confusion.

**Evidence:** `app/src/main/res/values/strings.xml:51` — `music_library_empty_hint`.

**Fix direction:** Change hint string to `Download/Mods/` (singular). Consider resolving the actual path at runtime with `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)` and displaying it dynamically so it is correct on all device variants.

---

### Issue D — No reload gesture

`MusicLibraryScreen.kt` has no pull-to-refresh, refresh button, or any other mechanism to trigger `GameViewModel.refreshMusicLibrary()` from within the library screen. The only automatic rescan happens at `openMusicLibrary()` (called once when the screen opens). If the user adds a file while the library is open — or if MediaStore needed time to index — there is no recovery path short of closing and reopening the screen.

**Evidence:** `MusicLibraryScreen.kt` — no refresh callback in the composable signature or the layout. `GameViewModel.openMusicLibrary()` calls `modMusicService.rescanLibrary()` once on entry (line 185), but there is no subsequent trigger.

**Fix direction:** Add a refresh icon button to the `MusicLibraryScreen` top bar (next to the title). Wire it to a new `onRefresh` callback that calls `GameViewModel.refreshMusicLibrary()` followed by `refreshMusicState()`. This gives the user an explicit way to re-scan after adding files.

---

## Priority

| # | Issue | Severity |
|---|-------|----------|
| A | MediaStore indexing delay / tracker files not indexed | **Blocking** — feature does not work |
| B | MIME type: tracker formats may be invisible to MediaStore | **Blocking** — same symptom as A |
| C | Wrong path in empty-state hint | **High** — actively misleads the user |
| D | No reload gesture | **High** — no recovery path once library is open |

Issues A and B share the same fix (prefer `File`-based scan on API ≤ 32; handle API 33+ separately). C and D are independent and straightforward.
