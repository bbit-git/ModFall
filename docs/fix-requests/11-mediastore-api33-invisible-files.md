# 11 — MediaStore returns no results for user-placed files on API 33+ (Android 13+)

## Reported symptom

No tracks appear in the music library on Android 15 (API 35). Files are physically present at `/storage/emulated/0/Download/Mods/`.

## Root cause

On Android 13+ (API 33+), `MediaStore.Downloads` queries **only return files that were created by the current app**. Files placed by the user via a file manager, ADB push, or any other app are invisible to the query — even if the files physically exist on disk and even if MediaStore has indexed them. This is the scoped storage model: each app sees only its own contributions to shared storage.

The current code (`ModLibrary.scanMediaStore()`, API 33+ branch) queries `MediaStore.Downloads` without any mechanism to access files from other sources. On API 35 the query returns zero rows, which is silent and correct behaviour from Android's perspective.

**Evidence:**
- `ModLibrary.kt` line 19: branches to `scanMediaStore()` for `Build.VERSION_CODES.TIRAMISU` and above
- `ModLibrary.kt` lines 85–100: queries `MediaStore.Downloads.getContentUri(VOLUME_EXTERNAL)` with extension and path filters — no permission or mechanism that would make other-app files visible
- `AndroidManifest.xml`: declares no `MANAGE_EXTERNAL_STORAGE` permission; no SAF URI permissions are persisted anywhere in the codebase

## What does NOT fix this

- Calling `MediaScannerConnection.scanFile()` before querying — the scan triggers indexing, but indexed files from other apps are still not returned to this app's query on API 33+
- `READ_EXTERNAL_STORAGE` — deprecated and not granted on API 33+
- `READ_MEDIA_AUDIO/IMAGES/VIDEO` — cover only their respective media types; `.mod/.xm/.s3m/.it` are not audio MIME types recognised by Android

## Valid fix options

### Option 1 — Storage Access Framework (SAF) folder picker [recommended]
Present a one-time "Pick Mods folder" prompt using `ActivityResultContracts.OpenDocumentTree`. The user selects their `Mods` folder; the app receives a persistent URI permission via `contentResolver.takePersistableUriPermission()`. Subsequent scans use `DocumentFile.fromTreeUri()` to list and open files directly — no OS permission needed beyond the URI grant.

Pros: works on all API levels, no Play Store justification required, survives reboots (persisted URI permissions).  
Cons: requires one user interaction to set up; needs UI for the folder-picker flow and a way to clear/change the folder.

### Option 2 — `MANAGE_EXTERNAL_STORAGE` permission
Declare `<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />` and request it via `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`. If granted, the app can use direct `File` API on all of external storage on API 30+.

Pros: transparent to the user after one-time grant; existing `scanFileSystem()` code works unchanged.  
Cons: Google Play requires a declaration and justification for this permission; may be rejected for a game that doesn't have a core file-management use case. Not suitable for Play Store distribution without careful review.

### Option 3 — Accept the limitation; improve the empty state
Keep the current behaviour and update the empty-state message to explain that on Android 13+ the app cannot read user-placed files directly, with a link/instruction to copy files into the app's own private Downloads folder (accessible via `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`).

Pros: no permission changes required.  
Cons: poor UX — the app's private folder is not easily accessible to users; they cannot simply drag files in via a file manager.

## Recommended path

Implement **Option 1 (SAF folder picker)** as a new sub-feature of the music library:

1. Add a "Set music folder" button to `MusicLibraryScreen` (visible when library is empty or via a settings icon)
2. Launch `ActivityResultContracts.OpenDocumentTree` when tapped
3. On result: call `contentResolver.takePersistableUriPermission()` with `FLAG_GRANT_READ_URI_PERMISSION`
4. Persist the chosen URI in `SettingsRepository` (DataStore)
5. Add a `scanSaf(treeUri: Uri)` method to `ModLibrary` using `DocumentFile.fromTreeUri()` for directory listing and `contentResolver.openInputStream()` for file reading
6. Branch `ModLibrary.scan()`: SAF URI if present (all API levels) → File-based if API ≤ 32 → empty if API 33+ and no SAF URI

## Impact

Blocking on Android 13+ (API 33, 34, 35). All users on modern Android cannot use the music feature.
