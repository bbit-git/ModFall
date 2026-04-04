# 13 — Fix-request 12 review: SAF folder picker (round 2, post-fix)

## Summary
- **milestone:** Fix-request 12 — SAF folder picker post-fix review
- **verdict:** pass with fixes
- **scope alignment:** in scope
- **validation confidence:** high

---

## Prior issue resolution status

| # | Issue | Status | Evidence |
|---|-------|--------|----------|
| 1 | `listFiles()` null check in `scanSaf()` | **RESOLVED** | `ModLibrary.kt:146–149` — `root.listFiles() ?: run { Log.w(...); return emptyList() }`, consistent with `scanFileSystem()` at lines 46–49 |
| 2 | Zero-length file filter in `scanSaf()` | **RESOLVED** | `ModLibrary.kt:158` — `if (file.length == 0L) continue` in `scanSafEntries()`, matching `scanFileSystem():54` |
| 3 | SAF test coverage | **PARTIAL** | `ModLibraryTest.kt` lines 133–184 add 4 tests for `scanSafEntries()` (zero-length, unsupported extension, valid files). `scanSaf()` itself (null context, null root, null `listFiles()`, warning log) remains untested |
| 4 | Initial URI hint for folder picker | **RESOLVED** | `BlockDropApp.kt:134–137` constructs `Uri.fromFile(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS))`; passed to launcher at line 174 |

---

## Critical issues

None. All four prior blocking issues have been addressed.

---

## Important improvements

### 1. `scanSaf()` wrapper logic not covered by tests
**[test gap]**

The new tests cover `scanSafEntries()` (the file-filtering loop) but not `scanSaf()` (the outer method). `scanSaf()` contains non-trivial guard logic:

- `ModLibrary.kt:139` — `context ?: return emptyList()`
- `ModLibrary.kt:141–144` — null/non-directory root guard
- `ModLibrary.kt:146–149` — `listFiles()` null guard + warning log

None of these branches are exercised by any test. The `listFiles() == null` path is the one that was the subject of the prior blocking bug (#1) — it is now handled in code but not verified in tests.

**Minimum tests to add** (can be in `ModLibraryTest.kt`):
- `scanSaf returns empty list when context is null`
- `scanSaf returns empty list when fromTreeUri returns null root`
- `scanSaf returns empty list when listFiles returns null` — verifies the fix from issue #1
- `scanSaf delegates to scanSafEntries when listFiles succeeds`

---

## What is correct

- **Null-check pattern** (`scanSaf` and `scanFileSystem`) is now consistent
- **Zero-length filtering** is consistent across both scan paths
- **Initial URI hint** — `Uri.fromFile(DIRECTORY_DOWNLOADS)` is a sensible default; system may ignore it but improves UX on most devices
- **Permission ordering** — `takePersistableUriPermission()` called before `setMusicFolderUri()` (`BlockDropApp.kt:126–130`) — correct
- **`FLAG_GRANT_READ_URI_PERMISSION`** — READ only, not WRITE — correct
- **Branching order** — SAF → File (API ≤ 32) → empty (API 33+ no URI) — correct
- **Stale/revoked URI** — `DocumentFile.fromTreeUri()` null → guarded at line 141; `listFiles()` null → guarded at line 146; returns empty list gracefully
- **Context** — `applicationContext` used throughout; no Activity context dependency
- **Log levels** — WARN on errors, INFO on success — appropriate

---

## Optional suggestions

- Add SettingsRepository and ViewModel tests for URI persistence and `setMusicFolderUri()` → `modMusicService.setLibraryTreeUri()` call chain (not blocking, but improves overall coverage for the music library feature).
- `MusicLibraryScreen` empty-state messaging correctly distinguishes "no SAF URI set" from "SAF URI set but no files found" — no change needed, noted as correct.

---

## Scope or architecture proposals

None.

---

## Validation observations

- Thread safety: `viewModelScope.launch` for URI subscription — correct cancellation on clear
- DataStore Flow for URI — thread-safe by design
- No race between `takePersistableUriPermission()` and URI storage
- File-based scan (API ≤ 32) untouched — no regressions
- Manifest unchanged — no unnecessary permissions
- `contentResolver.openInputStream()` in `ModPlayer` wrapped in `.use {}` — no stream leaks
